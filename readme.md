Auxiliary processes Gradle plugin

It helps running auxiliary processes in background, such as mock services for integration tests

## Usage

### Setup

```kotlin
plugins {
    id("io.github.mfarsikov.auxiliary-processes") version "0.1.0"
}
```

### Configuration

```kotlin
auxProcesses {
    create("mock server") {
        command = """java -jar myMockServer.jar"""
    }
}

task("integrationTest") {
    dependsOn("startAuxProcesses")
    doLast {
        println("testing...")
        Thread.sleep(3000)
    }
    finalizedBy("stopAuxProcesses")
}
```