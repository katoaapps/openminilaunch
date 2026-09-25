import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file(".signing/release.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}

fun releaseSigningValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull
        ?: localSigningProperties.getProperty(propertyName)

val releaseStorePath = releaseSigningValue("ANDROID_KEYSTORE_PATH", "storeFile")
val releaseStorePassword = releaseSigningValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = releaseSigningValue("ANDROID_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = releaseSigningValue("ANDROID_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.katoaapps.openminilaunch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.katoaapps.openminilaunch"
        minSdk = 26
        targetSdk = 35
        versionCode = 27
        versionName = "1.5.4"
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf(
            "en",
            "en-rGB",
            "b+zh+Hans",
            "b+yue+Hant+HK",
            "ja",
            "ko",
            "hi",
            "es",
            "fr",
            "bn",
            "pt-rBR",
            "ru",
            "ur",
            "b+id",
            "ar",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("upstreamRelease") {
                storeFile = rootProject.file(checkNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isPseudoLocalesEnabled = true
        }
        getByName("release") {
            signingConfig = signingConfigs.findByName("upstreamRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.window:window:1.5.1")
    implementation("com.google.zxing:core:3.5.4")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
}
