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

rootProject.name = "ReverseBenchMark"

include(":challenge-core")
include(":challenge-native")
include(":app-level0")
include(":app-level1")
include(":app-level2")
include(":app-level3")
include(":app-level4")
include(":app-level5")
include(":app-level6")
include(":app-level7")
include(":app-level8")
