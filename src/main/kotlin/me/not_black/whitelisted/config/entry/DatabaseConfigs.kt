package me.not_black.whitelisted.config.entry

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfigs(
    val whitelist: DatabaseConfig = DatabaseConfig(urlOrName = "whitelist.db"),
    val cache: DatabaseConfig = DatabaseConfig(urlOrName = "cache.db"),
)
