pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // isteğe bağlı ek repo
    }
}

rootProject.name = "fui-project"
include(":androidApp", ":shared", ":server", ":webApp")
