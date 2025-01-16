plugins {
    kotlin("jvm") version "2.0.21"
}

group = "dev.zanckor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.rctcwyvrn:blake3:1.3")
    implementation("org.tukaani:xz:1.8")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}