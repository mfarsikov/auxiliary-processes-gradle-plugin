plugins {
    kotlin("jvm") version "1.7.10"
    `maven-publish`
    `java-gradle-plugin`
    id("com.github.mfarsikov.kewt-versioning") version "1.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation (gradleApi())

    testImplementation ("org.jetbrains.kotlin:kotlin-test")
    testImplementation ("org.jetbrains.kotlin:kotlin-test-junit")
}

project.group = "com.github.mfarsikov.auxiliary-processes"
project.version = kewtVersioning.version

gradlePlugin {
    val auxproc by plugins.creating {
        id = "com.github.mfarsikov.auxiliary-processes"
        displayName = "Auxiliary processes"
        implementationClass = "com.github.mfarsikov.auxiliary.plugin.AuxiliaryProcessesPlugin"
    }
}