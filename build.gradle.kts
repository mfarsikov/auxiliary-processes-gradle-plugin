import org.jetbrains.kotlin.gradle.tasks.KotlinJavaToolchain

plugins {
    kotlin("jvm") version "1.7.10"
    `maven-publish`
    `java-gradle-plugin`
    id("com.github.mfarsikov.kewt-versioning") version "1.0.0"
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation(gradleApi())

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

project.group = "io.github.mfarsikov.auxiliary-processes"
project.version = kewtVersioning.version

gradlePlugin {
    val auxproc by plugins.creating {
        id = "io.github.mfarsikov.auxiliary-processes"
        displayName = "Auxiliary processes"
        implementationClass = "io.github.mfarsikov.auxiliary.plugin.AuxiliaryProcessesPlugin"
    }
}

pluginBundle {
    website = "https://github.com/mfarsikov/auxiliary-processes-gradle-plugin"
    vcsUrl = "https://github.com/mfarsikov/auxiliary-processes-gradle-plugin"
    description = "Helps to run auxiliary processes in background"
    tags = listOf("build", "process", "parallel")
}
project.ext["gradle.publish.key"] = System.getenv("GRADLE_PUBLISH_KEY")
project.ext["gradle.publish.secret"] = System.getenv("GRADLE_PUBLISH_SECRET")

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}