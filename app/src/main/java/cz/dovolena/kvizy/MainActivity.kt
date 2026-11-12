package cz.dovolena.kvizy

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    companion object {
        private const val START_URL = "https://dovolena-krkonose.vercel.app"
        private const val ALLOWED_HOST = "dovolena-krkonose.vercel.app"
    }

    private lateinit var webView: WebView
    private lateinit var offlineView: View
    private lateinit var retryButton: Button

    /** Nastaví se, když hlavní stránka selže při načítání (typicky bez internetu). */
    private var mainFrameFailed = false

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
            userAgentString = "$userAgentString KvizyTV"
        }
        webView.setBackgroundColor(getColor(R.color.bg_dark))
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = GameWebViewClient()

        retryButton.setOnClickListener { retry() }

        // Zpět = historie WebView, na začátku historie zavřít aplikaci.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (offlineView.visibility != View.VISIBLE && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
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
