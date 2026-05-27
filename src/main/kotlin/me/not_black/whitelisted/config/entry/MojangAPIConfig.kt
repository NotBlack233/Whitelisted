package me.not_black.whitelisted.config.entry

import kotlinx.serialization.Serializable

@Serializable
data class MojangAPIConfig(
    val apiServer: String = "https://api.mojang.com/",
    val sessionServer: String = "https://sessionserver.mojang.com/",
    val cacheExpireTime: Long = 10 * 60 * 1000, // milliseconds
)
