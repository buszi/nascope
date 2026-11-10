package io.buszi.nascope.ui.dashboard.widgets.system

/**
 * Operating system and host platform classifications supported by the telemetry monitor.
 *
 * @param id Unique identifier string matching Glances / POSIX platform identifiers.
 * @param displayName Human-readable operating system or distribution label.
 * @param shortCode Technical uppercase monospace tag for compact displays.
 */
enum class HostPlatform(
    val id: String,
    val displayName: String,
    val shortCode: String,
) {
    Linux("linux", "Linux", "LNX"),
    Ubuntu("ubuntu", "Ubuntu", "UBU"),
    Debian("debian", "Debian", "DEB"),
    Synology("synology", "Synology DSM", "DSM"),
    TrueNas("truenas", "TrueNAS CORE", "NAS"),
    FreeBsd("freebsd", "FreeBSD", "BSD"),
    MacOS("darwin", "macOS", "MAC"),
    Windows("windows", "Windows Server", "WIN"),
    Docker("docker", "Docker Host", "DOC"),
    Generic("generic", "Unix Server", "SRV");

    companion object {
        /**
         * Resolves a [HostPlatform] from a Glances `os_name` or platform string.
         */
        fun fromIdentifier(identifier: String?): HostPlatform {
            if (identifier.isNullOrBlank()) return Generic
            val normalized = identifier.lowercase()
            return when {
                "ubuntu" in normalized -> Ubuntu
                "debian" in normalized -> Debian
                "synology" in normalized || "dsm" in normalized -> Synology
                "truenas" in normalized -> TrueNas
                "freebsd" in normalized -> FreeBsd
                "darwin" in normalized || "macos" in normalized || "os x" in normalized -> MacOS
                "windows" in normalized -> Windows
                "docker" in normalized -> Docker
                "linux" in normalized -> Linux
                else -> Generic
            }
        }
    }
}
