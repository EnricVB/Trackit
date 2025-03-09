package dev.enric.util.common

/**
 * Utility object to manage environment variables across different operating systems.
 * It allows setting and retrieving environment variables for Windows, Linux, and macOS.
 *
 * It supports:
 * - Setting environment variables globally on Windows, Linux, and macOS.
 * - Retrieving environment variables from the system environment.
 *
 * The set environment variables are applied globally, affecting future sessions or processes.
 */
object EnvironmentVariables {

    /**
     * Sets an environment variable globally based on the operating system.
     *
     * @param key The environment variable name.
     * @param value The value of the environment variable to set.
     */
    fun setEnv(key: String, value: String) {
        val os = detectOS()

        if (os == OS.WINDOWS) {
            setEnvGlobal(key, value)
        }

        if (os == OS.LINUX || os == OS.MAC) {
            setEnvGlobalUnix(key, value)
            setEnvGlobalMac(key, value)
        }
    }

    /**
     * Sets the environment variable globally on Windows.
     * It uses the `setx` command to persist the environment variable across sessions.
     *
     * @param key The environment variable name.
     * @param value The value of the environment variable to set.
     */
    fun setEnvGlobal(key: String, value: String) {
        try {
            ProcessBuilder("cmd", "/c", "setx $key \"$value\"").start().waitFor()
        } catch (e: Exception) {
            println("Error setting Windows environment variable: ${e.message}")
        }
    }

    /**
     * Sets the environment variable globally on Linux.
     * It appends the export statement to the user's `~/.bashrc` file for future sessions.
     *
     * @param key The environment variable name.
     * @param value The value of the environment variable to set.
     */
    fun setEnvGlobalUnix(key: String, value: String) {
        try {
            ProcessBuilder("bash", "-c", "echo 'export $key=\"$value\"' >> ~/.bashrc").start().waitFor()
        } catch (e: Exception) {
            println("Error setting Linux environment variable: ${e.message}")
        }
    }

    /**
     * Sets the environment variable globally on macOS.
     * It appends the export statement to the `/etc/profile` file, which affects all users.
     *
     * @param key The environment variable name.
     * @param value The value of the environment variable to set.
     */
    fun setEnvGlobalMac(key: String, value: String) {
        try {
            ProcessBuilder("sudo", "sh", "-c", "echo 'export $key=\"$value\"' >> /etc/profile").start().waitFor()
        } catch (e: Exception) {
            println("Error setting macOS environment variable: ${e.message}")
        }
    }

    /**
     * Retrieves the value of an environment variable from the system environment.
     *
     * @param key The environment variable name.
     * @return The value of the environment variable, or `null` if not found.
     */
    fun getEnv(key: String): String? {
        return System.getenv(key)
    }

    /**
     * Detects the current operating system.
     *
     * @return The operating system type (Windows, Linux, macOS, or Unknown).
     */
    private fun detectOS(): OS {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> OS.WINDOWS
            osName.contains("mac") -> OS.MAC
            osName.contains("nix") || osName.contains("nux") || osName.contains("linux") -> OS.LINUX
            else -> OS.UNKNOWN
        }
    }

    enum class OS { WINDOWS, MAC, LINUX, UNKNOWN }
}
