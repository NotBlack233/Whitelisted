package me.not_black.whitelisted.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import me.not_black.whitelisted.api.WhitelistAPI
import me.not_black.whitelisted.api.WhitelistAPI.Result
import net.kyori.adventure.text.Component
import kotlin.uuid.Uuid

object WhitelistCommand : LiteralArgumentBuilder<CommandSource>("whitelist") {
    init {
        requires { it.hasPermission("whitelisted.whitelist") }
            .then(
                BrigadierCommand.literalArgumentBuilder("add")
                    .requires { it.hasPermission("whitelisted.whitelist.add") }
                    .then(
                        BrigadierCommand.requiredArgumentBuilder("target", StringArgumentType.word())
                            .executes {
                                val target = StringArgumentType.getString(it, "target")
                                val asUuid = Uuid.parseOrNull(target)
                                val result = if (asUuid != null) {
                                    WhitelistAPI.addToWhitelist(asUuid)
                                } else {
                                    WhitelistAPI.addToWhitelist(target)
                                }
                                if (result != Result.OK) {
                                    it.source.sendMessage(Component.translatable(
                                        when (result) {
                                            Result.MOJANG_API_ERROR -> ""
                                            Result.DUPLICATE -> ""
                                            Result.DB_ERROR -> ""
                                            else -> ""
                                        }
                                    )
                                        .arguments(Component.text(target)))
                                    return@executes 0
                                }
                                it.source.sendMessage(Component.translatable("msg.whitelisted.command.add.success")
                                    .arguments(Component.text(target)))
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
    }
}