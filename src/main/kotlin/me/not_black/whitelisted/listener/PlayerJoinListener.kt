package me.not_black.whitelisted.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.api.WhitelistAPI
import net.kyori.adventure.text.Component
import kotlin.uuid.toKotlinUuid

object PlayerJoinListener {
    @Subscribe
    fun onLogin(event: LoginEvent) {
        if (!Whitelisted.inst.config.enabled) return
        if (!WhitelistAPI.inWhitelist(event.player.uniqueId.toKotlinUuid()))
            event.player.disconnect(Component.text("You are not whitelisted!"))
    }
}