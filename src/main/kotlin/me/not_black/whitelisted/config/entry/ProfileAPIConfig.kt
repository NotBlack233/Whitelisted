package me.not_black.whitelisted.config.entry

import kotlinx.serialization.Serializable

@Serializable
data class ProfileAPIConfig(
    // "mojang" or "yggdrasil"
    val type: String = "mojang",
    val name: String? = null,
    val url: String? = null,
    val cacheExpireTime: Long = 10 * 60, // seconds
)
