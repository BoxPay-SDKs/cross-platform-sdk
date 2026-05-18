pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") // ✅ add this
        maven {
            url = uri("https://jitpack.io")
            credentials {
                username = providers.gradleProperty("jitpack.username").get()
                password = providers.gradleProperty("jitpack.token").get()
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") // ✅ add this
        maven {
            url = uri("https://jitpack.io")
            credentials {
                username = providers.gradleProperty("jitpack.username").get()
                password = providers.gradleProperty("jitpack.token").get()
            }
        }
    }
}


rootProject.name = "crossplatformsdk"

include(":cross-platform-sdk")
project(":cross-platform-sdk").projectDir = file("cross-platform-sdk")
include(":androidkmpapp")
