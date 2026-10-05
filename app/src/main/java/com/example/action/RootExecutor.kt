package com.example.action

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

data class ShellResult(
    val command: String,
    val isRoot: Boolean,
    val isSuccess: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long
)

class RootExecutor {

    // Standard paths where `su` binary typically resides
    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    // Destructive command patterns that MUST trigger user confirmation
    private val destructivePatterns = listOf(
        Regex("""\brm\s+-[rRfF]+\s+[/~*]"""),
        Regex("""\brm\s+-[rRfF]+\s+/system"""),
        Regex("""\brm\s+-[rRfF]+\s+/data"""),
        Regex("""\breboot\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpoweroff\b""", RegexOption.IGNORE_CASE),
        Regex("""\bshutdown\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdd\s+if=.*of=/dev/"""),
        Regex("""\bmkfs\b"""),
        Regex("""\bformat\b"""),
        Regex("""\bmount\s+.*remount.*rw\s+/system"""),
        Regex("""\bkill\s+-9\s+1\b""")
    )

    fun isRootBinaryPresent(): Boolean {
        for (path in suPaths) {
            if (File(path).exists()) return true
        }
        return false
    }

    suspend fun checkRootAccess(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val binaryExists = isRootBinaryPresent()
        if (!binaryExists) {
            return@withContext Pair(false, "Root binary (su) was not detected in standard system directories.")
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0 && (output.contains("uid=0") || output.contains("root"))) {
                Pair(true, "Root access granted: $output")
            } else {
                Pair(false, "Root binary present, but root permission was denied or timed out.")
            }
        } catch (e: Exception) {
            Pair(false, "Root check failed: ${e.message}")
        }
    }

    fun isDestructiveCommand(command: String): Boolean {
        val trimmed = command.trim()
        return destructivePatterns.any { it.containsMatchIn(trimmed) }
    }

    suspend fun executeCommand(command: String, useRoot: Boolean): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        var exitCode = -1

        try {
            val process: Process = if (useRoot) {
                Runtime.getRuntime().exec("su")
            } else {
                Runtime.getRuntime().exec("sh")
            }

            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes(command + "\n")
                os.writeBytes("exit\n")
                os.flush()
            }

            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (outReader.readLine().also { line = it } != null) {
                stdout.appendLine(line)
            }
            while (errReader.readLine().also { line = it } != null) {
                stderr.appendLine(line)
            }

            exitCode = process.waitFor()
        } catch (e: Exception) {
            stderr.append("Execution Exception: ${e.message}")
        }

        val elapsed = System.currentTimeMillis() - startTime
        ShellResult(
            command = command,
            isRoot = useRoot,
            isSuccess = exitCode == 0,
            exitCode = exitCode,
            stdout = stdout.toString().trim(),
            stderr = stderr.toString().trim(),
            executionTimeMs = elapsed
        )
    }

    // Supported safe root operations
    suspend fun forceStopPackage(packageName: String): ShellResult {
        return executeCommand("am force-stop $packageName", useRoot = true)
    }

    suspend fun takeRootScreenshot(outputPath: String): ShellResult {
        return executeCommand("screencap -p $outputPath", useRoot = true)
    }

    suspend fun getSystemProps(): ShellResult {
        return executeCommand("getprop ro.build.version.release && getprop ro.product.model", useRoot = true)
    }
}
