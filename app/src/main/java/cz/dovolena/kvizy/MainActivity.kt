package cz.dovolena.kvizy

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    companion object {
        private const val START_URL = "https://dovolena-krkonose.vercel.app"
        private const val ALLOWED_HOST = "dovolena-krkonose.vercel.app"
        private const val ORIGIN = "https://dovolena-krkonose.vercel.app"

        /** Měřítko stránky na TV (1.0 = jako dosud, menší = oddálenější). */
        private const val PAGE_SCALE = 0.75f

        /** Za jak dlouho se musí Zpět stisknout podruhé, aby se aplikace zavřela. */
        private const val EXIT_CONFIRM_MS = 2500L
    }

    private lateinit var webView: WebView
    private lateinit var offlineView: View
    private lateinit var retryButton: Button

    /** Nastaví se, když hlavní stránka selže při načítání (typicky bez internetu). */
    private var mainFrameFailed = false

    /** Skript s úpravami pro ovladač (assets/tv.js). */
    private lateinit var tvScript: String
    private var lastBackPress = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obrazovka během hry nezhasíná.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()

        webView = findViewById(R.id.webView)
        offlineView = findViewById(R.id.offlineView)
        retryButton = findViewById(R.id.retryButton)

        CookieManager.getInstance().setAcceptCookie(true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // localStorage – pamatuje si přihlášení správce
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)  // target="_blank" se otevře ve stejném WebView
            // Oddálení: stránka se rozvrhne do širšího viewportu a zmenší se na šířku obrazovky.
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            userAgentString = "$userAgentString KvizyTV"
        }
        webView.setBackgroundColor(getColor(R.color.bg_dark))
        webView.webChromeClient = WebChromeClient()

        val dm = resources.displayMetrics
        val layoutWidth = (dm.widthPixels / dm.density / PAGE_SCALE).roundToInt()
        tvScript = assets.open("tv.js").bufferedReader().use { it.readText() }
            .replace("__LAYOUT_WIDTH__", layoutWidth.toString())
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, tvScript, setOf(ORIGIN))
        }
        webView.webViewClient = GameWebViewClient()

        retryButton.setOnClickListener { retry() }

        // Zpět: nejdřív ho zkusí zpracovat web (zavřít okno, opustit posuvník, tlačítko „← Zpět“),
        // pak historie WebView, a nakonec zavření aplikace po druhém stisku.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else if (isOnline()) {
            webView.loadUrl(START_URL)
        } else {
            showOffline()
        }
        if (offlineView.visibility != View.VISIBLE) webView.requestFocus(View.FOCUS_DOWN)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersive()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        // Uložit cookies na disk, aby přežily zavření aplikace.
        CookieManager.getInstance().flush()
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun enterImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun onBack() {
        if (offlineView.visibility == View.VISIBLE) {
            finish()
            return
        }
        webView.evaluateJavascript("window.__kvizyTvBack ? window.__kvizyTvBack() : false") { handled ->
            if (handled == "true") return@evaluateJavascript
            if (webView.canGoBack()) {
                webView.goBack()
                return@evaluateJavascript
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastBackPress < EXIT_CONFIRM_MS) {
                finish()
            } else {
                lastBackPress = now
                Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun retry() {
        if (!isOnline()) {
            Toast.makeText(this, R.string.offline_title, Toast.LENGTH_SHORT).show()
            retryButton.requestFocus()
            return
        }
        mainFrameFailed = false
        val current = webView.url
        if (current.isNullOrBlank() || current.startsWith("about:")) {
            webView.loadUrl(START_URL)
        } else {
            webView.reload()
        }
    }

    private fun showOffline() {
        offlineView.visibility = View.VISIBLE
        webView.visibility = View.INVISIBLE
        retryButton.requestFocus()
    }

    private fun showWeb() {
        if (offlineView.visibility == View.VISIBLE || webView.visibility != View.VISIBLE) {
            offlineView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.requestFocus(View.FOCUS_DOWN)
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isAllowed(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return false
        return uri.host?.lowercase() == ALLOWED_HOST
    }

    private fun showBlocked() {
        Toast.makeText(this, R.string.external_link_blocked, Toast.LENGTH_SHORT).show()
    }

    private inner class GameWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (isAllowed(uri)) return false
            // Odkazy mimo naši doménu se ve WebView neotevírají – zkusí se jiná aplikace, jinak nic.
            if (request.hasGesture()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                    showBlocked()
                } catch (_: SecurityException) {
                    showBlocked()
                }
            }
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            mainFrameFailed = false
        }

        override fun onPageFinished(view: WebView, url: String?) {
            // Záloha pro starší WebView bez skriptů při startu dokumentu (skript se nevloží dvakrát).
            if (!mainFrameFailed && url != null && isAllowed(Uri.parse(url))) {
                view.evaluateJavascript(tvScript, null)
            }
            if (mainFrameFailed) showOffline() else showWeb()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                mainFrameFailed = true
                showOffline()
            }
        }
    }
}
