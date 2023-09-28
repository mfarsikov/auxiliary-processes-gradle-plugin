plugins {
    kotlin("jvm") version "1.9.10"
    `maven-publish`
    `java-gradle-plugin`
    id("com.github.mfarsikov.kewt-versioning") version "1.0.0"
    id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation(gradleApi())

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

project.group = "io.github.mfarsikov.auxiliary-processes"
project.version = kewtVersioning.version

gradlePlugin {
    website = "https://github.com/mfarsikov/auxiliary-processes-gradle-plugin"
    vcsUrl = "https://github.com/mfarsikov/auxiliary-processes-gradle-plugin"

    plugins {
        create("auxproc") {
            id = "io.github.mfarsikov.auxiliary-processes"
            displayName = "Auxiliary processes"
            implementationClass = "io.github.mfarsikov.auxiliary.plugin.AuxiliaryProcessesPlugin"
            tags = listOf("build", "process", "parallel")
            description = "Helps to run auxiliary processes in background"
        }
    }
}
project.ext["gradle.publish.key"] = System.getenv("GRADLE_PUBLISH_KEY")
project.ext["gradle.publish.secret"] = System.getenv("GRADLE_PUBLISH_SECRET")

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}