package io.buszi.nascope

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform