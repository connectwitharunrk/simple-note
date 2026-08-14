package com.arunrk.simplenote

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform