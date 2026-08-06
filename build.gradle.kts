plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false

    // Dumps the client's public ABI to Source/api/Source.api and fails the build when it changes
    // unexpectedly. Adding a property to a data class silently invalidates every Java caller using
    // a positional constructor, which has broken Java twice; this is what makes that visible in a
    // diff rather than in someone's build.
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

apiValidation {
    // Only the published client has an ABI worth guarding. The samples are applications.
    ignoredProjects.addAll(listOf("Samples", "Kotlin", "Java", "Console"))
}
