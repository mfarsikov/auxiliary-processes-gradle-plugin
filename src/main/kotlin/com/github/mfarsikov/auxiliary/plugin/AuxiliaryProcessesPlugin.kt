package io.github.mfarsikov.auxiliary.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

class AuxiliaryProcessesPlugin : Plugin<Project> {
    //private val client = OkHttpClient()
    private var runningAuxProcesses: List<RunningAuxProcess>? = null
    private lateinit var logger: Logger

    //private val retryDelay = 1000.toLong()
    override fun apply(project: Project) {

        logger = project.logger
        val commands: NamedDomainObjectContainer<AuxConfig> = project.container(AuxConfig::class.java)
        project.extensions.add("auxProcesses", commands)

        project.task("startAuxProcesses") {

            it.doLast {

                try {
                    runningAuxProcesses = commands.map { it.start() }
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

    private fun AuxConfig.start(): RunningAuxProcess {
        logger.debug("Starting aux process ${name}")

        File("build/logs").also { if (!it.exists()) it.mkdirs() }

        val logFile = File("build/logs/${name}.log").also { if (!it.exists()) it.createNewFile() }

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
        return RunningAuxProcess(this, process)
    }

    private fun RunningAuxProcess.awaitReadiness() {
        return
//        if (config.readinessProbeUrl == null) return
//
//        do {
//            logger.info("${config.name} readiness check...")
//
//            if (isReady()) return
//            if (isTimeoutExceeded()) throw RuntimeException("${config.name} readiness timeout exceeded")
//
//            logger.info("${config.name} is not ready. Retrying in ${retryDelay / 1000.0} sec.")
//
//            runBlocking { delay(retryDelay) }
//        } while (true)
    }

//    private fun RunningAuxProcess.isReady() = try {
//
//        val request = Request.Builder().url(config.readinessProbeUrl!!).build()
//
//        client.newCall(request).execute().use { it.code() == 200 }
//
//    } catch (e: Exception) {
//        logger.info("Readiness probe failed", e)
//        false
//    }
//
//    private fun RunningAuxProcess.isTimeoutExceeded() = when (config.readinessTimeout) {
//        null -> false
//        else -> Instant.now() < startTime.plusMillis(config.readinessTimeout!!.toLong())
//    }

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
    val startTime: Instant = Instant.now(),
)

open class AuxConfig(
    val name: String
) {
    lateinit var command: String
//    var readinessProbeUrl: String? = null
//    var readinessTimeout: Int? = null
}
