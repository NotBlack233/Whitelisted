package me.not_black.whitelisted.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import me.not_black.whitelisted.Whitelisted
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object MainCommand : LiteralArgumentBuilder<CommandSource>("whitelisted") {
    init {
        requires { it.hasPermission("whitelisted.main") }
            .executes {
                it.source.sendMessage(Component.translatable("msg.whitelisted.command.main.usage")
                    .color(NamedTextColor.RED))
                Command.SINGLE_SUCCESS
            }
            .then(
                BrigadierCommand.literalArgumentBuilder("reload")
                    .requires { it.hasPermission("whitelisted.main.reload") }
                    .executes {
                        Whitelisted.inst.reload()
                        it.source.sendMessage(Component.translatable("msg.whitelisted.command.main.reload.success")
                            .color(NamedTextColor.GREEN))
                        Command.SINGLE_SUCCESS
                    }
            )
    }
}