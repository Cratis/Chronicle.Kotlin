plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "io.cratis.samples"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Spring Boot's dependency management pins kotlinx-coroutines below the 1.11 the client is built
// against, which fails at startup with NoSuchMethodError (BuildersKt.runBlockingK). Applications using
// the starter need the same override; see Documentation/guides/spring-boot.md.
extra["kotlin-coroutines.version"] = "1.11.0"

dependencies {
    implementation(project(":Integrations:SpringBoot"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("io.grpc:grpc-netty-shaded:1.84.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
