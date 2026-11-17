plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":minkboard-model"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
