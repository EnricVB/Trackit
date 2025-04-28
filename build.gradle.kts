@file:Suppress("ReplaceReadLineWithReadln")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.dokka") version "1.9.0"
}

group = "dev.zanckor"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.github.rctcwyvrn:blake3:1.3")                         // Blake3 hash function
    implementation("org.bouncycastle:bcprov-jdk18on:1.76")                   // Argon2 password hashing

    implementation("org.tukaani:xz:1.8")                                     // ZLib compression algorithm
    implementation("info.picocli:picocli:4.7.6")                             // Command line interface
    implementation("org.fusesource.jansi:jansi:2.4.0")                       // ANSI escape codes

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")              // Standard library
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")    // Coroutines

    implementation("io.github.java-diff-utils:java-diff-utils:4.12")         // Diff algorithm

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.17")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(22))
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


        doLast {
            val outputDir = Paths.get("docker/jar")
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir)
            }

            val jarFile = archiveFile.get().asFile
            jarFile.copyTo(outputDir.resolve("trackit.jar").toFile(), overwrite = true)

            println("JAR copiado a docker/jar/trackit.jar")
        }
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


    doFirst {
        print("Enter the commit title:")
        val title = readLine() ?: ""

        print("Enter the commit message:")
        val message = readLine() ?: ""

        args = listOf("commit", title, message, "-a")
    }
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

    args = listOf(
        "user-create", "-n", "Usuario Prueba", "-p", "Password Prueba", "-m", "mail@gmail.com", "-P", "123456789"
    )
}

tasks.register<JavaExec>("tktUserList") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("user-list")
}

tasks.register<JavaExec>("tktRoleCreate") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("role-create", "-n", "Rol Prueba 3", "-l", "1", "-r", "usma", "-b", "main", "'--'")
}

tasks.register<JavaExec>("tktRoleModify") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("role-modify", "-n", "Rol Prueba", "-l", "3", "-r", "----", "-b", "main", "'rw'")
}

tasks.register<JavaExec>("tktRoleList") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("role-list")
}

tasks.register<JavaExec>("tktCheckout") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    doFirst {
        print("Enter the commit hash to checkout:")
        val hash = readLine() ?: ""

        args = listOf("checkout", hash) // Second parameter must be the hash
    }
}

tasks.register<JavaExec>("tktLog") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    args = listOf("log")
}

tasks.register<JavaExec>("tkt") {
    mainClass.set("dev.enric.Main")
    workingDir = file("${project.rootDir}/tktFolder")
    group = "execute"
    classpath = project.sourceSets["main"].runtimeClasspath

    doFirst {
        print("trackit ")
        System.out.flush()

        val command = readLine() ?: ""

        args = command.split(' ')
    }
}