package me.not_black.whitelisted.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.not_black.whitelisted.Whitelisted
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object ConfigManager {
    private val logger: Logger by lazy { LoggerFactory.getLogger("whitelisted-config") }
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val configFile: File = Whitelisted.inst.dataDirectory.resolve("config.json").toFile()

    fun loadConfig(): Config {
        if (!configFile.exists()) {
            logger.info("Config file does not exist, creating it...")
            configFile.createNewFile()
            configFile.writeText("{}")
        }
        var config: Config
        logger.debug("Loading config...")
        try {
            config = json.decodeFromString<Config>(configFile.readText())
        } catch (e: Exception) {
            logger.warn("Malformed config file, resetting to default")
            config = Config()
            configFile.copyTo(Whitelisted.inst.dataDirectory.resolve("config.json.bak").toFile(), true)
            saveConfig(config)
        }
        logger.debug("Config loaded")
        return config
    }

    fun saveConfig(config: Config) {
        logger.debug("Saving config...")
        configFile.writeText(json.encodeToString(config))
        logger.debug("Config saved")
    }
}