Auxiliary processes Gradle plugin

It helps running auxiliary processes in background, such as mock services for integration tests

## Usage

### Setup

```kotlin
plugins {
    id("io.github.mfarsikov.auxiliary-processes") version "0.3.0"
}
```

### Configuration

```kotlin
auxProcesses {
    create("mock server") {
        command = """java -jar myMockServer.jar"""
        readinessProbe {
            successHttpGet("http://localhost:8080") || // simple GET http request
                    logContainsLine { "success" in it } || // checks log lines
                    httpClient.newCall(Request.Builder().url("localhost:8080").header("Auth", "token").build()).execute().body?.string() // arbitrary http call
                        ?.contains("")
                    ?: true
        }
        readinessTimeout = 60_000 // milliseconds
    }
    create("database") {
        ...
    }
}

task("integrationTest") {
    dependsOn("startAuxProcesses") // runs all aux processes before 'integrationTest' task
    doLast {
        println("testing...")
        Thread.sleep(3000)
    }
    finalizedBy("stopAuxProcesses") // stops all aux processes after 'integrationTest' task
}
```