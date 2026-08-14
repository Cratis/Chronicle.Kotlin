plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-library`
    id("com.vanniktech.maven.publish")
}

group = "io.cratis"
version = providers.gradleProperty("version").getOrElse("0.0.0-SNAPSHOT")

val springBootVersion = "3.5.3"
val coroutinesVersion = "1.9.0"

dependencies {
    api(project(":Source"))

    api("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    api("org.springframework.boot:spring-boot:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")

    // Web and security integration are wired only when the host application brings them in, so they
    // stay compile-only here - the starter works just as well in a plain worker application.
    compileOnly("org.springframework:spring-web:6.2.8")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
    compileOnly("org.springframework.security:spring-security-core:6.5.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    testImplementation("org.springframework.security:spring-security-core:6.5.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.mockk:mockk:1.13.14")
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

    coordinates("io.cratis", "chronicle-spring-boot-starter", version.toString())

    pom {
        name.set("Chronicle Spring Boot Starter")
        description.set("Spring Boot auto-configuration for the Cratis Chronicle Kotlin and Java client")
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
