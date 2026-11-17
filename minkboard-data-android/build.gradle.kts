plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.katoaapps.minkboard.data"
    compileSdk = 36

    defaultConfig {
        // Room, Keystore AES-GCM, and java.time usage in this module are available at API 26.
        // Do not make embedded launcher hosts inherit the standalone app's API 28 floor.
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":minkboard-core"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
}
