plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"

    // Dumps the client's public ABI to Source/api/Source.api and fails the build when it changes
    // unexpectedly. Adding a property to a data class silently invalidates every Java caller using
    // a positional constructor, which has broken Java twice; this is what makes that visible in a
    // diff rather than in someone's build.
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

// Applied to every subproject too (not just root) so useLatestVersions rewrites version
// literals wherever they're actually declared, not just in the root build file.
subprojects {
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "se.patrikerdes.use-latest-versions")
}

fun isNonStable(version: String): Boolean {
    val unstableKeyword = listOf("ALPHA", "BETA", "RC", "M", "PREVIEW", "EAP")
        .any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return unstableKeyword || !regex.matches(version)
}

allprojects {
    tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>().configureEach {
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }
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
