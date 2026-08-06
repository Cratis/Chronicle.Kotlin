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
include("Testing")
include("Samples:Kotlin:Console")
include("Samples:Java:Console")
