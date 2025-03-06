import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.zanckor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.rctcwyvrn:blake3:1.3")             // Blake3 hash function
    implementation("org.bouncycastle:bcprov-jdk18on:1.76")  // Argon2 password hashing

    implementation("org.tukaani:xz:1.8")                         // ZLib compression algorithm
    implementation("info.picocli:picocli:4.7.6")                 // Command line interface
    implementation("org.fusesource.jansi:jansi:2.4.0")           // ANSI escape codes

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")  // Standard library

    testImplementation(kotlin("test"))
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = "dev.enric.Main"
        }
    }

    // Configurar Shadow para generar un JAR ejecutable
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("trackit")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

// Para tareas específicas
tasks.withType<JavaExec> {
    standardInput = System.`in`
    jvmArgs = listOf("-Djava.awt.headless=true")
}

// Ejecutar el programa con comandos
tasks.register<JavaExec>("tktHelp") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("-h")
}

tasks.register<JavaExec>("tktInit") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("init")
}

tasks.register<JavaExec>("tktStage") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("stage", ".")
}

tasks.register<JavaExec>("tktUnstage") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("unstage", ".")
}

tasks.register<JavaExec>("tktIgnore") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("ignore", "src")
}

tasks.register<JavaExec>("tktCommit") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("commit", "Title", "Message")
}

tasks.register<JavaExec>("tktKeepSession") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("config", "-ks", "-u", "username", "-p", "password", "--local")
}

tasks.register<JavaExec>("tktUserCreate") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("user-create", "-n", "Usuario Prueba", "-p", "Password Prueba", "-m", "mail@gmail.com", "-P", "123456789")
}

tasks.register<JavaExec>("tktUserList") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("user-list")
}