plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false

    // Dumps the client's public ABI to Source/api/Source.api and fails the build when it changes
    // unexpectedly. Adding a property to a data class silently invalidates every Java caller using
    // a positional constructor, which has broken Java twice; this is what makes that visible in a
    // diff rather than in someone's build.
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

apiValidation {
    // The client is what has an ABI worth guarding - it is the artifact Java applications compile
    // against, and the one Java has broken on twice. The samples are applications with no consumers.
    //
    // "SpringBoot" covers the two samples of that name as well as the starter, because the validator
    // matches on project name and all three share one. The starter is thin Spring wiring over the
    // client, so guarding the client is what matters here.
    ignoredProjects.addAll(listOf("Samples", "Kotlin", "Java", "Console", "SpringBoot"))
}
