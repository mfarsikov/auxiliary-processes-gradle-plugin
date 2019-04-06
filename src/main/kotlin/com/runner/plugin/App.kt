package com.runner.plugin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gradle.api.Plugin
import org.gradle.api.Project
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.NamedDomainObjectContainer
import org.slf4j.Logger
import java.io.File
import java.time.Instant
import kotlin.RuntimeException

class AuxiliaryProcessPlugin : Plugin<Project> {
    private val client = OkHttpClient()
    private var runningAuxProcesses: List<RunningAuxProcess>? = null
    private lateinit var logger: Logger
    override fun apply(project: Project) {

        logger = project.logger
        val commands: NamedDomainObjectContainer<AuxConfig> = project.container(AuxConfig::class.java)
        project.extensions.add("auxProcesses", commands)

        project.task("startAuxProcesses") {
            it.doLast {

                try {
                    runningAuxProcesses = commands.map { run(it) }

                    Runtime.getRuntime().addShutdownHook(Thread {
                        runningAuxProcesses!!.forEach { it.process.destroy() }
                    })

                    runningAuxProcesses!!.forEach {
                        awaitReadiness(it)
                    }
                } catch (ex: Exception) {
                    stop()
                    throw ex
                }
            }
        }
        project.task("stopAuxProcesses") {
            it.doLast {
                stop()
            }
        }
    }

    private fun stop() {
        runningAuxProcesses?.forEach {
            if (it.process.isAlive) it.process.destroy()
            logger.info("Process ${it.config.name} destroyed")
        }
    }


    private fun awaitReadiness(runningAuxProcess: RunningAuxProcess) {
        if (runningAuxProcess.config.readinessProbeUrl == null) return

        do {
            val request = Request.Builder()
                    .url(runningAuxProcess.config.readinessProbeUrl!!)
                    .build()

            logger.info("${runningAuxProcess.config.name} Readiness check...")

            val response = client.newCall(request).execute().also { it.close() }

            if (response.code() == 200) {
                logger.info("Process ${runningAuxProcess.config.name} is ready")
                return
            }
            if (runningAuxProcess.config.readinessTimeout != null) {
                val threshold = runningAuxProcess.startTime.plusMillis(runningAuxProcess.config.readinessTimeout!!.toLong())
                if (Instant.now() < threshold) throw RuntimeException("${runningAuxProcess.config.name} Readiness timeout exceeded")
            }
            logger.info("${runningAuxProcess.config.name} is not ready...")

            runBlocking { delay(1000) }
        } while (true)
    }

    private fun run(config: AuxConfig): RunningAuxProcess {
        logger.info("Executing ${config.name}")

        File("logs").also { if (!it.exists()) it.mkdirs() }

        val logFile = File("logs/${config.name}.log").also { if (!it.exists()) it.createNewFile() }

        val process = ProcessBuilder()
                .redirectOutput(logFile)
                .command(config.command.split(" "))
                .start()

        GlobalScope.launch {
            delay(200)
            if (!process.isAlive) throw RuntimeException("Failed to start ${config.name}")
        }
        return RunningAuxProcess(config, process)
    }
}

data class RunningAuxProcess(
        val config: AuxConfig,
        val process: Process,
        val startTime: Instant = Instant.now()
)

open class AuxConfig(
        val name: String
) {
    lateinit var command: String
    var readinessProbeUrl: String? = null
    var readinessTimeout: Int? = null
}
