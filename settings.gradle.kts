// FILE: settings.gradle.kts
// FULL REPLACEMENT

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

rootProject.name = "iCoffee"
include(":app")