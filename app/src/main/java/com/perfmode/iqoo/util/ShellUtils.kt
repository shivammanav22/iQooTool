package com.perfmode.iqoo.util

import android.util.Log
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellUtils {

    private var loopJob: Job? = null

    /**
     * Runs a single shell command using Shizuku.
     * This function executes the command once and waits for its completion.
     *
     * @param command The shell command string to execute.
     */
    fun runShellCommand(command: String) {
        if (!Shizuku.pingBinder()) {
            Log.e("ShellUtils", "Shizuku binder not available for command: $command")
            return
        }

        CoroutineScope(Dispatchers.IO).launch { // Run shell command in a coroutine to avoid blocking UI thread
            try {
                // Using "sh -c command" is generally more robust for single commands
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                // It's good practice to consume input/error streams to prevent process hanging
                process.outputStream.close()
                process.inputStream.close()
                process.errorStream.close()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    Log.e("ShellUtils", "Single command failed with exit code $exitCode: $command")
                } else {
                    Log.d("ShellUtils", "Single command executed: $command")
                }
            } catch (e: Exception) {
                Log.e("ShellUtils", "Error executing single command: $command", e)
            }
        }
    }

    /**
     * Executes a shell command using Shizuku and returns its output.
     * This is useful for commands that return data (like 'cat /proc/stat').
     *
     * @param command The shell command string to execute.
     * @return The output of the command as a String, or null if an error occurs.
     */
    suspend fun executeCommandWithOutput(command: String): String? {
        if (!Shizuku.pingBinder()) {
            Log.e("ShellUtils", "Shizuku binder not available for command with output: $command")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                    val errorOutput = StringBuilder()
                    var errorLine: String?
                    while (errorReader.readLine().also { errorLine = it } != null) {
                        errorOutput.append(errorLine).append("\n")
                    }
                    Log.e("ShellUtils", "Command with output failed (exit $exitCode): $command\nError: $errorOutput")
                    null
                } else {
                    Log.d("ShellUtils", "Command with output executed successfully: $command")
                    output.toString().trim()
                }
            } catch (e: Exception) {
                Log.e("ShellUtils", "Error executing command with output: $command", e)
                null
            }
        }
    }

    /**
     * Loops through a list of shell commands using Shizuku.
     * This function continuously executes the provided commands with a delay.
     * Any previously running loop will be stopped before starting a new one.
     *
     * @param commands The list of shell command strings to execute in a loop.
     */
    fun runCommandsLoop(commands: List<String>) {
        stopExecution() // Stop previous loop if any

        if (!Shizuku.pingBinder()) {
            Log.e("ShellUtils", "Shizuku binder not available for loop commands.")
            return
        }

        loopJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) { // Check isActive to ensure the coroutine hasn't been cancelled
                commands.forEach { command ->
                    if (!isActive) return@forEach // Check isActive before running each command in the loop
                    try {
                        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                        // It's good practice to consume input/error streams if not explicitly needed,
                        // to prevent the process from hanging if its buffers fill up.
                        process.outputStream.close()
                        process.inputStream.close()
                        process.errorStream.close()
                        val exitCode = process.waitFor()
                        if (exitCode != 0) {
                            Log.e("ShellUtils", "Loop command failed with exit code $exitCode: $command")
                        } else {
                            Log.d("ShellUtils", "Loop command executed: $command")
                        }
                    } catch (e: Exception) {
                        Log.e("ShellUtils", "Error executing loop command: $command", e)
                    }
                }
                // IMPORTANT: Adjust this delay to balance persistence and system load.
                // 3000L = 3 seconds, 5000L = 5 seconds.
                delay(5000L) // Recommended delay to alleviate system lag
            }
        }
    }

    /**
     * Stops any currently running command loop.
     */
    fun stopExecution() {
        loopJob?.cancel() // Cancel the coroutine job
        loopJob = null // Clear the job reference
        Log.d("ShellUtils", "Loop execution stopped.")
    }
}