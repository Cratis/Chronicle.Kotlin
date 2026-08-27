plugins {
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish")
}

group = "io.cratis"
version = providers.gradleProperty("version").getOrElse("0.0.0-SNAPSHOT")

val coroutinesVersion = "1.9.0"

dependencies {
    api(project(":Source"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
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

    coordinates("io.cratis", "chronicle-testing", version.toString())

    pom {
        name.set("Chronicle Kotlin Client Testing")
        description.set("In-process test support for the Cratis Chronicle event sourcing client for Kotlin and Java")
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
