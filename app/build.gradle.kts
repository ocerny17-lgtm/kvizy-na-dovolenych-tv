plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Podpis release buildu – hodnoty přicházejí z proměnných prostředí
// (v GitHub Actions z secrets, lokálně je můžete nastavit ručně).
val signingStoreFile: String? = System.getenv("SIGNING_STORE_FILE")
val signingStorePassword: String? = System.getenv("SIGNING_STORE_PASSWORD")
val signingKeyAlias: String? = System.getenv("SIGNING_KEY_ALIAS")
val signingKeyPassword: String? = System.getenv("SIGNING_KEY_PASSWORD")
val hasSigning = !signingStoreFile.isNullOrBlank() && file(signingStoreFile!!).exists()

// versionCode musí s každou verzí růst, jinak Android aktualizaci odmítne.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "cz.dovolena.kvizy"
    compileSdk = 36

    defaultConfig {
        applicationId = "cz.dovolena.kvizy.tv"
        minSdk = 24
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.$buildNumber"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.14.0")
}
