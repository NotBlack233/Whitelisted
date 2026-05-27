package me.not_black.whitelisted.config.entry

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val sqlite: Boolean = true,
    val urlOrName: String? = null,
    val driver: String? = null,
    val username: String? = null,
    val password: String? = null,
)
