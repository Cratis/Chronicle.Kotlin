plugins {
    kotlin("jvm")
    application
}

group = "io.cratis.samples"
version = "1.0.0"

repositories {
    mavenCentral()
}

val coroutinesVersion = "1.9.0"

dependencies {
    implementation(project(":Source"))
    runtimeOnly("io.grpc:grpc-netty-shaded:1.70.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.cratis.chronicle.samples.console.MainKt")
}

tasks.run.get().apply {
    standardInput = System.`in`
}
