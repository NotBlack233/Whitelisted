package me.not_black.whitelisted.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import me.not_black.whitelisted.api.WhitelistAPI
import net.kyori.adventure.text.Component
import kotlin.uuid.toKotlinUuid

object PlayerJoinListener {
    @Subscribe
    fun onServerPreConnectEvent(event: ServerPreConnectEvent) {
        if (!WhitelistAPI.inWhitelist(event.player.uniqueId.toKotlinUuid()))
            event.player.disconnect(Component.text("You are not whitelisted!"))
    }
}