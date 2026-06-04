package me.not_black.whitelisted.config

import kotlinx.serialization.Serializable
import me.not_black.whitelisted.config.entry.DatabaseConfigs
import me.not_black.whitelisted.config.entry.HttpConfig
import me.not_black.whitelisted.config.entry.ProfileAPIConfig

@Serializable
data class Config (
    val enabled: Boolean = true,
    val locale: String = "en_US",
    val httpServer: HttpConfig = HttpConfig(),
    val profileAPI: ProfileAPIConfig = ProfileAPIConfig(),
    val database: DatabaseConfigs = DatabaseConfigs(),
)