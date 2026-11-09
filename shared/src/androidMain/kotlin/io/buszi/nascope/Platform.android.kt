package io.buszi.nascope

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType = PlatformType.Android
    override val os: OperatingSystem = OperatingSystem.Android
}

actual fun getPlatform(): Platform = AndroidPlatform()