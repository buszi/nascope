package io.buszi.nascope

import androidx.compose.runtime.staticCompositionLocalOf

enum class PlatformType {
    Android,
    Desktop;

    val isMobile: Boolean
        get() = this == Android

    val isDesktop: Boolean
        get() = this == Desktop
}

enum class OperatingSystem {
    MacOS,
    Windows,
    Linux,
    Android,
    Unknown;

    val isMac: Boolean
        get() = this == MacOS

    val isWindows: Boolean
        get() = this == Windows

    val isLinux: Boolean
        get() = this == Linux

    val isAndroid: Boolean
        get() = this == Android
}

interface Platform {
    val name: String
    val type: PlatformType
    val os: OperatingSystem

    val isMobile: Boolean
        get() = type.isMobile

    val isDesktop: Boolean
        get() = type.isDesktop

    val isMac: Boolean
        get() = os.isMac

    val isWindows: Boolean
        get() = os.isWindows

    val isLinux: Boolean
        get() = os.isLinux
}

expect fun getPlatform(): Platform

val LocalPlatform = staticCompositionLocalOf { getPlatform() }
