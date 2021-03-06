## Deploy 

* to maven local: `./gradlew publishToMavenLocal`
    
* to bintray: `./gradlew bintrayPublish`
    
## Usage
### Setup

```
buildscript {
    repositories {
        maven {
            url "https://dl.bintray.com/farsikov-max/gradle-auxiliary-processes-plugin"
        }
    }
    dependencies {
        classpath 'mfarsikov:auxiliary-processes-plugin:0.0.3'
    }
}

plugins {
...
}

apply plugin: 'mfarsikov.auxiliary-processes'
```

###  Configuration

```
auxProcesses {
    'long-runner-1' {
        command = 'java -jar build/libs/runner-app.jar'
        readinessProbeUrl = 'http://google.com'
    }
    'long-runner-2' {
        command = 'java -jar build/libs/runner-app.jar'
        readinessProbeUrl = 'http://google.com'
        readinessTimeout = 1
    }
}

task('inegrationTest') {
    dependsOn('startAuxProcesses')
    doLast{
        println('testing...')
        Thread.sleep(3000)
    }
    finalizedBy('stopAuxProcesses')
}
```