package io.buszi.nascope.domain.telemetry

/** How often a plugin is polled, relative to the refresh interval setting. */
internal enum class PollTier {
    /** The setting itself. */
    Fast,

    /** 5 x the setting, at most 30 s. Also the retry period of unavailable and not yet loaded plugins. */
    Slow,

    /** Once per session; retried at the slow period until it succeeds. */
    Static,
}

/** The Glances plugins the poller covers and the tier each one is polled in. */
internal enum class TelemetryPlugin(val tier: PollTier) {
    Cpu(PollTier.Fast),
    PerCpu(PollTier.Fast),
    Mem(PollTier.Fast),
    MemSwap(PollTier.Fast),
    Load(PollTier.Fast),
    Network(PollTier.Fast),
    DiskIo(PollTier.Fast),

    Fs(PollTier.Slow),
    Sensors(PollTier.Slow),
    Containers(PollTier.Slow),
    ProcessCount(PollTier.Slow),
    ProcessList(PollTier.Slow),
    Uptime(PollTier.Slow),
    QuickLook(PollTier.Slow),

    System(PollTier.Static),
}
