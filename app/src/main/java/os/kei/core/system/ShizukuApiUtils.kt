package os.kei.core.system

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import os.kei.core.log.AppLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import java.io.InputStream
import java.lang.reflect.Method
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ShizukuApiUtils(
    private val requestCode: Int = DEFAULT_REQUEST_CODE
) {
    private data class InteractiveCommandRewriteResult(
        val command: String,
        val adaptedTopOnce: Boolean = false
    )

    private data class UiDumpRewriteResult(
        val command: String,
        val redirectedPath: String?
    )


    private var statusCallback: ((String) -> Unit)? = null
    private var cachedNewProcessMethod: Method? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        publishStatus(currentStatus())
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        publishStatus("Shizuku service disconnected")
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
        if (code != requestCode) return@OnRequestPermissionResultListener
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            publishStatus("Shizuku permission: granted")
        } else {
            publishStatus("Shizuku permission: denied")
        }
    }

    fun attach(onStatusChanged: (String) -> Unit) {
        statusCallback = onStatusChanged
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }.onFailure {
            publishStatus("Shizuku init failed: ${it.javaClass.simpleName}")
        }
        publishStatus(currentStatus())
    }

    fun detach() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
        statusCallback = null
    }

    fun requestPermissionIfNeeded() {
        runCatching {
            when {
                !Shizuku.pingBinder() -> publishStatus("Shizuku service unavailable (start Shizuku app first)")
                Shizuku.isPreV11() -> publishStatus("Shizuku pre-v11 is unsupported")
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                    publishStatus("Shizuku permission: granted")
                }

                Shizuku.shouldShowRequestPermissionRationale() -> {
                    publishStatus("Shizuku permission denied permanently")
                }

                else -> {
                    publishStatus("Requesting Shizuku permission...")
                    Shizuku.requestPermission(requestCode)
                }
            }
        }.onFailure {
            publishStatus("Shizuku request failed: ${it.javaClass.simpleName}")
        }
    }

    fun currentStatus(): String {
        return runCatching {
            if (!Shizuku.pingBinder()) return "Shizuku service unavailable (start Shizuku app first)"
            if (Shizuku.isPreV11()) return "Shizuku pre-v11 is unsupported"
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                "Shizuku permission: granted"
            } else {
                "Shizuku permission: not granted"
            }
        }.getOrDefault("Shizuku unavailable")
    }

    fun canUseCommand(): Boolean {
        return runCatching {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun execCommand(command: String, timeoutMs: Long = 2000L): String? {
        if (!canUseCommand()) return null
        val process = createShellProcess(command) ?: return null
        return runCatching {
            executeProcessAndRead(process = process, timeoutMs = timeoutMs)
        }.onFailure {
            AppLogger.w(
                TAG,
                "execCommand failed: ${it.javaClass.simpleName}${it.message?.let { msg -> ": $msg" }.orEmpty()}"
            )
        }.getOrNull()
    }

    suspend fun execCommandCancellable(command: String, timeoutMs: Long = 2000L): String? {
        if (!canUseCommand()) return null
        val process = createShellProcess(command) ?: return null

        return suspendCancellableCoroutine { continuation ->
            val worker = Thread(
                {
                    val result = runCatching { executeProcessAndRead(process = process, timeoutMs = timeoutMs) }
                    if (!continuation.isActive) return@Thread
                    result.onSuccess { output ->
                        continuation.resume(output)
                    }.onFailure { throwable ->
                        continuation.resumeWithException(throwable)
                    }
                },
                "KeiOS-ShizukuExec"
            ).apply { isDaemon = true }
            worker.start()

            continuation.invokeOnCancellation {
                runCatching { process.destroy() }
                runCatching { process.destroyForcibly() }
                runCatching { worker.interrupt() }
            }
        }
    }

    private fun createShellProcess(command: String): Process? {
        val interactiveRewrite = rewriteInteractiveShellCommand(command)
        if (interactiveRewrite.adaptedTopOnce) {
            publishStatus("top command adapted: run once with -n 1")
        }
        val resolved = rewriteUiAutomatorDumpCommand(interactiveRewrite.command)
        if (!resolved.redirectedPath.isNullOrBlank()) {
            publishStatus("UI dump redirected: ${resolved.redirectedPath}")
        }
        val processMethod = resolveNewProcessMethod() ?: run {
            publishStatus("Shizuku process API unavailable")
            AppLogger.w(TAG, "createShellProcess skipped: Shizuku newProcess method unavailable")
            return null
        }
        return runCatching {
            processMethod.invoke(
                null,
                arrayOf("sh", "-c", resolved.command),
                null,
                null
            ) as? Process ?: error("Shizuku newProcess did not return Process")
        }.onFailure {
            AppLogger.w(
                TAG,
                "createShellProcess failed: ${it.javaClass.simpleName}${it.message?.let { msg -> ": $msg" }.orEmpty()}"
            )
        }.getOrNull()
    }

    private fun rewriteInteractiveShellCommand(command: String): InteractiveCommandRewriteResult {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return InteractiveCommandRewriteResult(command = command)
        val normalized = trimmed.lowercase(Locale.ROOT)
        if (!normalized.startsWith("top")) {
            return InteractiveCommandRewriteResult(command = command)
        }
        val hasIterationCount = Regex("""(^|\s)-n(\s*\d+)?(\s|$)""").containsMatchIn(trimmed)
        if (hasIterationCount) {
            return InteractiveCommandRewriteResult(command = command)
        }
        return InteractiveCommandRewriteResult(
            command = "$trimmed -n 1",
            adaptedTopOnce = true
        )
    }

    private fun executeProcessAndRead(process: Process, timeoutMs: Long): String? {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val stdoutReader = startStreamCollector(
            name = "KeiOS-ShizukuStdout",
            stream = process.inputStream,
            sink = stdout
        )
        val stderrReader = startStreamCollector(
            name = "KeiOS-ShizukuStderr",
            stream = process.errorStream,
            sink = stderr
        )

        var waitThrowable: Throwable? = null
        val waiter = Thread(
            {
                runCatching { process.waitFor() }
                    .onFailure { throwable -> waitThrowable = throwable }
            },
            "KeiOS-ShizukuWait"
        ).apply {
            isDaemon = true
            start()
        }

        waiter.join(timeoutMs)
        if (waiter.isAlive) {
            runCatching { process.destroy() }
            runCatching {
                waiter.join(300)
                if (waiter.isAlive) {
                    process.destroyForcibly()
                    waiter.join(300)
                }
            }
            stdoutReader.join(300)
            stderrReader.join(300)
            val partialOutput = stdout.toString().trim().ifBlank { stderr.toString().trim() }
            if (partialOutput.isNotBlank()) return partialOutput
            throw IllegalStateException("command timed out after ${timeoutMs}ms")
        }
        waitThrowable?.let { throw it }
        stdoutReader.join(600)
        stderrReader.join(600)
        return stdout.toString().trim().ifBlank { stderr.toString().trim() }.ifBlank { null }
    }

    private fun startStreamCollector(
        name: String,
        stream: InputStream,
        sink: StringBuilder
    ): Thread {
        return Thread(
            {
                runCatching {
                    stream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            sink.appendLine(line)
                        }
                    }
                }
            },
            name
        ).apply {
            isDaemon = true
            start()
        }
    }

    private fun resolveNewProcessMethod(): Method? {
        cachedNewProcessMethod?.let { return it }
        val resolved = runCatching {
            val parameterTypes = arrayOf(
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            Shizuku::class.java.declaredMethods.firstOrNull { method ->
                method.parameterTypes.contentEquals(parameterTypes) &&
                    Process::class.java.isAssignableFrom(method.returnType)
            }?.apply {
                isAccessible = true
            }
        }.onFailure {
            AppLogger.w(TAG, "resolveNewProcessMethod failed: ${it.javaClass.simpleName}")
        }.getOrNull()
        cachedNewProcessMethod = resolved
        return resolved
    }

    private fun rewriteUiAutomatorDumpCommand(command: String): UiDumpRewriteResult {
        val normalized = command.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return UiDumpRewriteResult(command = command, redirectedPath = null)
        if (!normalized.contains("uiautomator dump")) {
            return UiDumpRewriteResult(command = command, redirectedPath = null)
        }
        val tokens = command.trim().split(Regex("\\s+"))
        val uiIndex = tokens.indexOfFirst { it.equals("uiautomator", ignoreCase = true) }
        val dumpIndex = uiIndex + 1
        if (uiIndex < 0 || dumpIndex >= tokens.size || !tokens[dumpIndex].equals("dump", ignoreCase = true)) {
            return UiDumpRewriteResult(command = command, redirectedPath = null)
        }

        val options = mutableListOf<String>()
        var rawOutputPath: String? = null
        for (i in (dumpIndex + 1) until tokens.size) {
            val token = tokens[i]
            if (token.startsWith("-")) {
                options += token
                continue
            }
            rawOutputPath = token
            break
        }

        val requestedName = rawOutputPath
            ?.trim('"', '\'')
            ?.substringAfterLast('/')
            ?.ifBlank { null }
            ?: "window_dump.xml"
        val safeName = sanitizeUiDumpFileName(requestedName)
        val targetDir = AppBuildEnv.uiDumpShellDirectory()
        val targetPath = "$targetDir/$safeName"
        val optionText = if (options.isEmpty()) "" else options.joinToString(prefix = " ", separator = " ")
        val rewritten = "mkdir -p \"$targetDir\" && uiautomator dump$optionText \"$targetPath\""
        return UiDumpRewriteResult(command = rewritten, redirectedPath = targetPath)
    }

    private fun sanitizeUiDumpFileName(raw: String): String {
        val cleaned = raw
            .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
            .trim('_')
        val withExt = if (cleaned.lowercase(Locale.ROOT).endsWith(".xml")) cleaned else "${cleaned}.xml"
        return withExt.ifBlank { "window_dump.xml" }.take(64)
    }

    fun detailedRows(): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        val activated = binderAlive && granted

        rows += "Shizuku Binder Alive" to binderAlive.toString()
        rows += "Shizuku Permission Granted" to granted.toString()
        rows += "Shizuku Activated" to activated.toString()
        rows += "Shizuku Pre-v11" to runCatching { Shizuku.isPreV11().toString() }.getOrDefault("unknown")
        rows += "Shizuku Permission Rationale" to runCatching { Shizuku.shouldShowRequestPermissionRationale().toString() }.getOrDefault("unknown")

        reflectAny("getUid")?.let { rows += "Shizuku Service UID" to it.toString() }
        reflectAny("getVersion")?.let { rows += "Shizuku Service Version" to it.toString() }
        reflectAny("getServerPatchVersion")?.let { rows += "Shizuku Server Patch Version" to it.toString() }
        reflectAny("getSELinuxContext")?.let { rows += "Shizuku SELinux Context" to it.toString() }
        reflectAny("getLatestServiceVersion")?.let { rows += "Shizuku Latest Service Version" to it.toString() }

        if (activated) {
            execCommand("id")?.let { rows += "Shizuku id" to it.lineSequence().firstOrNull().orEmpty() }
            execCommand("whoami")?.let { rows += "Shizuku whoami" to it.lineSequence().firstOrNull().orEmpty() }
            execCommand("uname -a")?.let { rows += "Shizuku uname" to it.lineSequence().firstOrNull().orEmpty() }
            execCommand("getenforce")?.let { rows += "Shizuku getenforce" to it.lineSequence().firstOrNull().orEmpty() }
            execCommand("ps -A | wc -l")?.let { rows += "Shizuku process count" to it.lineSequence().firstOrNull().orEmpty() }
        }

        return rows.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    }

    private fun reflectAny(methodName: String): Any? {
        return runCatching {
            val method = Shizuku::class.java.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.isEmpty()
            } ?: return null
            method.isAccessible = true
            method.invoke(null)
        }.getOrNull()
    }

    private fun publishStatus(message: String) {
        val callback = statusCallback ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback(message)
        } else {
            mainHandler.post {
                statusCallback?.invoke(message)
            }
        }
    }

    companion object {
        private const val TAG = "ShizukuApiUtils"
        const val DEFAULT_REQUEST_CODE = 1001
        const val API_VERSION = "13.1.5"
    }
}
