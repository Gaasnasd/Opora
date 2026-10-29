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

rootProject.name = "opora"

// Логика — отдельная сборка: её можно собрать и проверить без Android SDK
// (./gradlew -p core test).
includeBuild("core")
include(":app")
