package dev.lassen.herdr

import java.io.File

/** Resolves the herdr binary, since GUI-launched JetBrains IDEs often don't inherit a login shell's PATH. */
object HerdrBinaryLocator {

    private val commonInstallPaths = listOf(
        "/opt/homebrew/bin/herdr",
        "/usr/local/bin/herdr",
        "${System.getProperty("user.home")}/.cargo/bin/herdr",
        "${System.getProperty("user.home")}/.local/bin/herdr",
    )

    fun resolve(configuredPath: String?): String? {
        if (!configuredPath.isNullOrBlank() && File(configuredPath).canExecute()) {
            return configuredPath
        }
        System.getenv("PATH")?.split(File.pathSeparator)?.forEach { dir ->
            val candidate = File(dir, "herdr")
            if (candidate.canExecute()) return candidate.absolutePath
        }
        commonInstallPaths.firstOrNull { File(it).canExecute() }?.let { return it }
        return resolveFromLoginShell()
    }

    private fun resolveFromLoginShell(): String? = try {
        val shell = System.getenv("SHELL")?.takeIf(String::isNotBlank) ?: "/bin/zsh"
        val process = ProcessBuilder(shell, "-lic", "command -v herdr")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.takeIf { it.isNotBlank() && File(it).canExecute() }
    } catch (e: Exception) {
        null
    }
}
