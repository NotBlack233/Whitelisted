package me.not_black.whitelisted.config.entry

import kotlinx.serialization.Serializable

@Serializable
data class HttpConfig(
    val enabled: Boolean = false,
    val port: Int = 9876,
    val host: String = "127.0.0.1",
    val token: String? = null,
)