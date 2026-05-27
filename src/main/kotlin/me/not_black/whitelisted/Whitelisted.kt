package me.not_black.whitelisted

import com.google.inject.Inject
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import me.not_black.whitelisted.api.WhitelistAPI
import me.not_black.whitelisted.api.WhitelistAPI.Result
import me.not_black.whitelisted.command.WhitelistCommand
import me.not_black.whitelisted.config.Config
import me.not_black.whitelisted.config.ConfigManager
import me.not_black.whitelisted.crypto.ECDH
import me.not_black.whitelisted.database.connect
import me.not_black.whitelisted.http.WhitelistedServer
import me.not_black.whitelisted.listener.PlayerJoinListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.Translatable
import org.http4k.server.Http4kServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.Logger
import java.nio.file.Path
import java.security.KeyPair
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.uuid.Uuid

class Whitelisted @Inject constructor(val server: ProxyServer, val logger: Logger, @param:DataDirectory val dataDirectory: Path) {
    val keyPair: KeyPair = ECDH.generateECKeyPair()
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
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        server.eventManager.register(this, PlayerJoinListener)
        if (config.httpServer.enabled) {
            httpServer.start()
            logger.info("HTTP server started successfully")
        }
        registerCommands()
    }

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
    }

    fun reload() {
        config = ConfigManager.loadConfig()
        httpServer = WhitelistedServer(config.httpServer.port, config.httpServer.host)
        ConfigManager.saveConfig(config)
    }

    companion object {
        lateinit var inst: Whitelisted
            private set
    }
}
