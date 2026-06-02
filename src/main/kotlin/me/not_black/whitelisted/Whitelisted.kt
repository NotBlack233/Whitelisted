package me.not_black.whitelisted

import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import me.not_black.whitelisted.command.MainCommand
import me.not_black.whitelisted.command.WhitelistCommand
import me.not_black.whitelisted.config.Config
import me.not_black.whitelisted.config.ConfigManager
import me.not_black.whitelisted.database.connect
import me.not_black.whitelisted.http.WhitelistedServer
import me.not_black.whitelisted.listener.PlayerJoinListener
import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import org.http4k.server.Http4kServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.Logger
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class Whitelisted @Inject constructor(val server: ProxyServer, val logger: Logger, @param:DataDirectory val dataDirectory: Path) {
    var config: Config
        private set
    var httpServer: Http4kServer
        private set
    var whitelistDb: Database
        private set
    var cacheDb: Database
        private set

    init {
        inst = this
        if (!dataDirectory.exists()) {
            dataDirectory.createDirectories()
        }
        config = ConfigManager.loadConfig()
        httpServer = WhitelistedServer(config.httpServer.port, config.httpServer.host)
        whitelistDb = connect(config.database.whitelist)
        cacheDb = connect(config.database.cache)
        ConfigManager.saveConfig(config)

        // i18n
        val translationStore = TranslationStore.messageFormat(Key.key("namespace:value"))
        val (lang, region) = config.locale.split('_')
        val locale = Locale.of(lang, region)
        val bundle = ResourceBundle.getBundle("me.not_black.whitelisted.Bundle", locale)
        translationStore.registerAll(locale, bundle, true)
        GlobalTranslator.translator().addSource(translationStore)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        server.eventManager.register(this, PlayerJoinListener)
        if (config.httpServer.enabled) {
            httpServer.start()
            logger.info("HTTP server started successfully")
        }
        registerCommands()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        httpServer.stop()
        logger.info("HTTP server stopped successfully")
    }

    private fun registerCommands() {
        val manager = server.commandManager

        // Plugin whitelist command
        manager.register(
            manager.metaBuilder("whitelist")
                .aliases("wl")
                .plugin(this)
                .build(),
            BrigadierCommand(WhitelistCommand)
        )

        // Plugin main command
        manager.register(
            manager.metaBuilder("whitelisted")
                .plugin(this)
                .build(),
            BrigadierCommand(MainCommand)
        )
    }

    fun reload() {
        config = ConfigManager.loadConfig()
        if (config.httpServer.enabled) {
            httpServer.stop()
            httpServer = WhitelistedServer(config.httpServer.port, config.httpServer.host)
            httpServer.start()
            logger.info("Reloaded HTTP server")
        }
        whitelistDb = connect(config.database.whitelist)
        cacheDb = connect(config.database.cache)
        ConfigManager.saveConfig(config)
        logger.info("Plugin reloaded")
    }

    companion object {
        lateinit var inst: Whitelisted
            private set
    }
}
