pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "OpenMiniLaunch"
include(
    ":app",
    ":minkpad-core",
    ":minkboard-model",
    ":minkboard-core",
    ":minkboard-data-android",
    ":minkboard-focus-android",
    ":minkboard-compose",
    ":sable-design",
    ":sable-games-runtime",
    ":sable-games-console",
)
