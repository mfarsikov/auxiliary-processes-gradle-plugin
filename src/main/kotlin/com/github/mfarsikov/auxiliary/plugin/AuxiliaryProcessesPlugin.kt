package io.github.mfarsikov.auxiliary.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

class AuxiliaryProcessesPlugin : Plugin<Project> {
    private val client = OkHttpClient()
    private var runningAuxProcesses: List<RunningAuxProcess>? = null
    private lateinit var logger: Logger

    private val retryDelay = 1000.toLong()
    override fun apply(project: Project) {

        logger = project.logger
        val commands: NamedDomainObjectContainer<AuxConfig> = project.container(AuxConfig::class.java)
        project.extensions.add("auxProcesses", commands)

        project.task("startAuxProcesses") {

            it.doLast {

                try {
                    runningAuxProcesses = commands.map { it.start(project) }
                        .also { Runtime.getRuntime().addShutdownHook(Thread { it.forEach { it.stop() } }) }

                    runningAuxProcesses!!.forEach {
                        it.awaitReadiness()
                    }

                } catch (ex: Exception) {
                    runningAuxProcesses?.forEach { it.stop() }
                    throw ex
                }
            }
        }

        project.task("stopAuxProcesses") {
            it.doLast {
                runningAuxProcesses?.forEach {
                    it.stop()
                }
            }
        }
    }

    private fun AuxConfig.start(project: Project): RunningAuxProcess {
        logger.debug("Starting aux process ${name}")

        File("${project.layout.buildDirectory}/logs").also { if (!it.exists()) it.mkdirs() }

        val logFile = File("${project.layout.buildDirectory}/logs/${name}.log").also { if (!it.exists()) it.createNewFile() }

        val process = ProcessBuilder()
            .redirectOutput(logFile)
            .redirectError(logFile)
            .command(command.split(" "))
            .start()

        Thread.sleep(200)
        if (!process.isAlive) {
            val msg = "Failed to start aux process $name, see log for details: ${logFile.absolutePath}"
            logger.error(msg)
            throw Exception(msg)
        }

        logger.lifecycle("Started aux process ${name}, pid:${process.pid()}, log: ${logFile.absolutePath}")
        return RunningAuxProcess(this, process, logFile)
    }

    private fun RunningAuxProcess.awaitReadiness() {
        if (config.readinessProbe == null) return

        do {
            logger.debug("Aux process ${config.name} readiness check...")

            if (config.readinessProbe!!.invoke(
                    ReadinessCheckContext(
                        httpClient = client,
                        logFile = logFile,
                        logger = logger,
                    )
                )
            ) {
                logger.debug("Aux process ${config.name} is ready")
                return
            }
            if (isTimeoutExceeded()) throw RuntimeException("Aux process ${config.name} has exceeded readiness timeout")

            logger.debug("Aux process ${config.name} is not ready. Retrying in ${retryDelay / 1000.0} sec.")

            Thread.sleep(retryDelay)
        } while (true)

    }

    private fun RunningAuxProcess.isTimeoutExceeded() = when (config.readinessTimeout) {
        null -> false
        else -> Instant.now() < startTime.plusMillis(config.readinessTimeout!!.toLong())
    }

    private fun RunningAuxProcess.stop() {
        if (process.isAlive) {
            process.descendants().forEach { it.destroy() }
            process.destroy()
            process.waitFor(10, TimeUnit.SECONDS)
            if (process.isAlive) {
                logger.warn("Cannot stop aux process ${config.name}, pid: ${process.pid()}, try to stop it manually")
            } else {
                logger.lifecycle("Aux process ${config.name} stopped")
            }
        }
    }
}

data class RunningAuxProcess(
    val config: AuxConfig,
    val process: Process,
    val logFile: File,
    val startTime: Instant = Instant.now(),
)

open class AuxConfig(
    val name: String
) {
    lateinit var command: String
    var readinessTimeout: Int? = null
    var readinessProbe: (ReadinessCheckContext.() -> Boolean)? = null

    fun readinessProbe(block: (ReadinessCheckContext.() -> Boolean)) {
        readinessProbe = block
    }

    fun isReadinesProbeDefined() = readinessProbe != null
}

data class ReadinessCheckContext(
    val httpClient: OkHttpClient,
    private val logFile: File,
    private val logger: Logger,
) {
    fun successHttpGet(url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { it.code == 200 }
        } catch (e: Exception) {
            logger.info("Readiness probe failed", e)
            false
        }
    }

    fun logContainsLine(predicate: (String) -> Boolean): Boolean {
        return logFile.useLines { it.any(predicate) }
    }
}