import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Duration

abstract class StopServer : DefaultTask() {
    @get:Internal
    abstract val pidFile: RegularFileProperty

    @TaskAction
    fun stop() {
        ServerProcess.stop(pidFile.get().asFile)
    }
}

class StartServerAction(
    val role: String,
) : Action<Task> {
    override fun execute(task: Task) {
        val exec = task as JavaExec
        val buildDir = exec.workingDir.resolve("build")
        ServerProcess.start(
            javaExecutable = File(System.getProperty("java.home"), "bin/java").absolutePath,
            classpath = exec.classpath.asPath,
            mainClassName = exec.mainClass.get(),
            workingDir = exec.workingDir,
            environment = exec.environment,
            logFile = buildDir.resolve("server-$role.log"),
            pidFile = buildDir.resolve("server-$role.pid"),
            logger = exec.logger,
        )
    }
}

object ServerProcess {
    fun start(
        javaExecutable: String,
        classpath: String,
        mainClassName: String,
        workingDir: File,
        environment: Map<String, Any>,
        logFile: File,
        pidFile: File,
        logger: Logger,
    ) {
        stop(pidFile)
        logFile.parentFile.mkdirs()
        logFile.writeText("")
        val argsFile = pidFile.resolveSibling("${pidFile.nameWithoutExtension}.args")
        argsFile.writeText("-cp\n\"$classpath\"\n$mainClassName\n")

        val launcher = ProcessBuilder(
            "sh",
            "-c",
            "nohup \"\$1\" @\"\$2\" >> \"\$3\" 2>&1 & echo \$!",
            "sh",
            javaExecutable,
            argsFile.absolutePath,
            logFile.absolutePath,
        )
            .directory(workingDir)
            .redirectErrorStream(true)
        environment.forEach { (key, value) ->
            launcher.environment()[key] = value.toString()
        }

        val started = launcher.start()
        val output = started.inputStream.bufferedReader().readText().trim()
        val exitCode = started.waitFor()
        if (exitCode != 0) {
            error("Failed to launch the server (exit $exitCode): $output")
        }
        val pid = output.lineSequence().mapNotNull { it.trim().toLongOrNull() }.lastOrNull()
            ?: error("Server launcher did not return a pid: $output")
        pidFile.writeText(pid.toString())

        val deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos()
        while (System.nanoTime() < deadline) {
            val alive = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)
            val logText = if (logFile.isFile) logFile.readText() else ""
            if ("Responding at" in logText) {
                logger.lifecycle("Server is up (pid $pid). Logs: ${logFile.path}")
                return
            }
            if (!alive) {
                error("Server process $pid exited before it started listening.\n${logText.takeLast(4000)}")
            }
            Thread.sleep(200)
        }
        error(
            "Server process $pid did not report ready within 45s. Logs: ${logFile.path}\n" +
                logFile.readText().takeLast(4000),
        )
    }

    fun stop(pidFile: File) {
        if (!pidFile.isFile) return
        val pid = pidFile.readText().trim().toLongOrNull()
        if (pid == null) {
            pidFile.delete()
            return
        }
        val handle = ProcessHandle.of(pid).orElse(null)
        if (handle == null || !handle.isAlive) {
            pidFile.delete()
            return
        }
        handle.destroy()
        val deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos()
        while (handle.isAlive && System.nanoTime() < deadline) {
            Thread.sleep(100)
        }
        if (handle.isAlive) {
            handle.destroyForcibly()
        }
        pidFile.delete()
    }
}
