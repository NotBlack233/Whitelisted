package me.not_black.whitelisted.config

import kotlinx.serialization.Serializable
import me.not_black.whitelisted.config.entry.DatabaseConfigs
import me.not_black.whitelisted.config.entry.HttpConfig
import me.not_black.whitelisted.config.entry.MojangAPIConfig

@Serializable
data class Config (
    val enabled: Boolean = true,
    val locale: String = "en_US",
    val httpServer: HttpConfig = HttpConfig(),
    val mojangAPI: MojangAPIConfig = MojangAPIConfig(),
    val database: DatabaseConfigs = DatabaseConfigs(),
)