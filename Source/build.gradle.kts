plugins {
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish")
}

group = "io.cratis"
version = providers.gradleProperty("version").getOrElse("0.0.0-SNAPSHOT")

val coroutinesVersion = "1.9.0"
val chronicleContractsVersion = "16.44.1"
val dnsJavaVersion = "3.6.5"
val classGraphVersion = "4.8.180"
val openTelemetryVersion = "1.64.0"

dependencies {
    api("io.cratis:chronicle-contracts:$chronicleContractsVersion")
    api("io.grpc:grpc-netty-shaded:1.70.0")
    api("com.google.code.gson:gson:2.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("dnsjava:dnsjava:$dnsJavaVersion")
    api(kotlin("reflect"))

    // The OpenTelemetry API only. It no-ops until an application registers an SDK, so instrumenting
    // stays the application's choice - one that does not is unaffected beyond a small jar.
    api("io.opentelemetry:opentelemetry-api:$openTelemetryVersion")

    // Classpath scanning behind automatic artifact discovery.
    implementation("io.github.classgraph:classgraph:$classGraphVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    // The OpenTelemetry SDK is a test-only dependency: the client ships the API alone so that
    // instrumenting stays the application's choice. The in-memory exporter is what lets a spec
    // assert on the spans the client actually produced.
    testImplementation("io.opentelemetry:opentelemetry-sdk:$openTelemetryVersion")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:$openTelemetryVersion")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.cratis", "chronicle", version.toString())

    pom {
        name.set("Chronicle Kotlin Client")
        description.set("Idiomatic event sourcing client for Kotlin and Java (JVM) for Cratis Chronicle")
        url.set("https://github.com/Cratis/Chronicle.Kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("cratis")
                name.set("Cratis")
                email.set("post@cratis.io")
            }
        }
        scm {
            url.set("https://github.com/Cratis/Chronicle.Kotlin")
            connection.set("scm:git:git://github.com/Cratis/Chronicle.Kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/Cratis/Chronicle.Kotlin.git")
        }
    }
}
