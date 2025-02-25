package dev.enric.util

object EnvironmentVariables {
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

    fun setEnvGlobal(key: String, value: String) {
        try {
            ProcessBuilder("cmd", "/c", "setx $key \"$value\"").start().waitFor()
        } catch (e: Exception) {
            println("Error setting Windows environment variable: ${e.message}")
        }
    }

    fun setEnvGlobalUnix(key: String, value: String) {
        try {
            ProcessBuilder("bash", "-c", "echo 'export $key=\"$value\"' >> ~/.bashrc").start().waitFor()
        } catch (e: Exception) {
            println("Error setting Linux environment variable: ${e.message}")
        }
    }

    fun setEnvGlobalMac(key: String, value: String) {
        try {
            ProcessBuilder("sudo", "sh", "-c", "echo 'export $key=\"$value\"' >> /etc/profile").start().waitFor()
        } catch (e: Exception) {
            println("Error setting macOS environment variable: ${e.message}")
        }
    }

    fun getEnv(key: String): String? {
        return System.getenv(key)
    }

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
