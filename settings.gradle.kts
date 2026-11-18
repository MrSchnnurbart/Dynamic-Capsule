pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "DynamicCapsule"

include(":app")
include(":core:model")
include(":core:datastore")
include(":core:database")
include(":core:system")
include(":feature:overlay")
include(":feature:onboarding")
include(":feature:settings")

project(":core").projectDir = file("core")
project(":feature").projectDir = file("feature")

