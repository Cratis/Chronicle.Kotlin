plugins {
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.cratis"
version = providers.gradleProperty("version").getOrElse("0.0.0-SNAPSHOT")

val coroutinesVersion = "1.9.0"
val chronicleContractsVersion = "15.34.4"

dependencies {
    api("io.cratis:chronicle-contracts:$chronicleContractsVersion")
    api("io.grpc:grpc-netty-shaded:1.70.0")
    api("com.google.code.gson:gson:2.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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
        description.set("Idiomatic event sourcing Kotlin client for Cratis Chronicle")
        url.set("https://github.com/cratis/chronicle.kotlin")
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
            url.set("https://github.com/cratis/chronicle.kotlin")
            connection.set("scm:git:git://github.com/cratis/chronicle.kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/cratis/chronicle.kotlin.git")
        }
    }
}
