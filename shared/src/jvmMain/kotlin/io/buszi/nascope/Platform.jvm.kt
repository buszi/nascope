package io.buszi.nascope

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val type: PlatformType = PlatformType.Desktop
    override val os: OperatingSystem = detectOperatingSystem()

    private companion object {
        private fun detectOperatingSystem(): OperatingSystem {
            val osName = System.getProperty("os.name")?.lowercase() ?: return OperatingSystem.Unknown
            return when {
                "mac" in osName || "darwin" in osName -> OperatingSystem.MacOS
                "win" in osName -> OperatingSystem.Windows
                "linux" in osName || "nix" in osName -> OperatingSystem.Linux
                else -> OperatingSystem.Unknown
            }
        }
    }
}

actual fun getPlatform(): Platform = JVMPlatform()