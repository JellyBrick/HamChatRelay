import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("org.jmailen.kotlinter") version "3.11.1"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "be.zvz.hamchatrelay"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("be.zvz:KotlinInside:1.14.6")
    implementation("org.fusesource.jansi:jansi:1.18")
    implementation("ch.qos.logback:logback-classic:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_6.toString()
}

tasks.create<LintTask>("ktLint") {
    group = "verification"
    source(files("src"))
}

tasks.create<FormatTask>("ktFormat") {
    group = "formatting"
    source(files("src"))
}

application {
    mainClass.set("${group}.AppKt")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("shadow")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass.get()))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}