plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.katoaapps.minkboard.ui"
    compileSdk = 36

    defaultConfig {
        // Both launcher hosts support Android 8. Keep the reusable UI at their shared floor;
        // the standalone shell may choose a stricter product minimum independently.
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":minkboard-core"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.activity:activity-compose:1.9.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
