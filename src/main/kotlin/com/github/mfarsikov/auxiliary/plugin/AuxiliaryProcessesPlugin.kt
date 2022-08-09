package io.github.mfarsikov.auxiliary.plugin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import java.io.File
import java.time.Instant
import kotlin.RuntimeException

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
                    runningAuxProcesses = commands.map { it.run() }.also(::addShutdownHook)

                    runningAuxProcesses!!.forEach {
                        it.awaitReadiness()
                        logger.lifecycle("${it.config.name} is ready")
                    }

                } catch (ex: Exception) {
                    runningAuxProcesses?.forEach { it.stop() }
                    throw ex
                }
            }
        }

        project.task("stopAuxProcesses") {
            it.doLast {
                runningAuxProcesses?.forEach { it.stop() }
            }
        }
    }

    private fun AuxConfig.run(): RunningAuxProcess {
        logger.info("Starting ${name}")

        File("build/logs").also { if (!it.exists()) it.mkdirs() }

        val logFile = File("build/logs/${name}.log").also { if (!it.exists()) it.createNewFile() }

        logger.info("Aux rrocess $name log: ${logFile.absoluteFile}")

        val process = ProcessBuilder()
            .redirectOutput(logFile)
            .redirectError(logFile)
            .command(command.split(" "))
            .start()

        GlobalScope.launch {
            delay(200)
            if (!process.isAlive) throw RuntimeException("Failed to start ${name}")
        }
        return RunningAuxProcess(this, process)
    }

    private fun addShutdownHook(runningAuxProcesses: List<RunningAuxProcess>) {
        Runtime.getRuntime().addShutdownHook(Thread { runningAuxProcesses.forEach { it.stop() } })
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
        if (process.isAlive) process.destroy()
        logger.info("Process ${config.name} stopped")
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
//    var readinessProbeUrl: String? = null
//    var readinessTimeout: Int? = null
}
