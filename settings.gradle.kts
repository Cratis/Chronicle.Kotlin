pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "chronicle-kotlin-workspace"

include("Source")
include("Samples:Kotlin:Console")
include("Samples:Java:Console")
