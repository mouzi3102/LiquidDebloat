package com.liquiddebloat.data

import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around a root shell. Everything the app does to the system goes
 * through here, so there is exactly one place to audit.
 */
object RootShell {

    data class Result(val code: Int, val output: String) {
        val ok: Boolean get() = code == 0
    }

    private const val DEFAULT_TIMEOUT_SECONDS = 45L

    /** Runs `su -c <command>`. Returns `null` when su itself is unavailable. */
    fun run(command: String, timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS): Result? {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Result(-1, output + "\n[timeout after ${timeoutSeconds}s]")
            } else {
                Result(process.exitValue(), output)
            }
        } catch (t: Throwable) {
            null
        }
    }

    /** True when su answers and the shell really runs as uid 0. */
    fun isRootAvailable(): Boolean {
        val result = run("id -u") ?: return false
        return result.output.trim().lineSequence().firstOrNull()?.trim() == "0"
    }
}
