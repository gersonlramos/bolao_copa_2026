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

rootProject.name = "BolãoCopa2026"

include(":app")
include(":feature:auth")
include(":feature:groups")
include(":feature:matches")
include(":feature:bets")
include(":feature:ranking")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:common")
