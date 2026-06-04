package me.not_black.whitelisted.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import me.not_black.whitelisted.api.WhitelistAPI
import me.not_black.whitelisted.exception.profileapi.ProfileAPINotFoundException
import me.not_black.whitelisted.exception.profileapi.ProfileAPITooManyRequestsException
import me.not_black.whitelisted.exception.whitelist.WhitelistDuplicateEntryException
import me.not_black.whitelisted.exception.whitelist.WhitelistNotFoundException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.uuid.Uuid

object WhitelistCommand : LiteralArgumentBuilder<CommandSource>("whitelist") {
    init {
        requires { it.hasPermission("whitelisted.whitelist") }
            .executes {
                it.source.sendMessage(Component.translatable("msg.whitelisted.command.whitelist.usage")
                    .color(NamedTextColor.RED))
                Command.SINGLE_SUCCESS
            }
            .then(
                BrigadierCommand.literalArgumentBuilder("add")
                    .requires { it.hasPermission("whitelisted.whitelist.add") }
                    .then(
                        BrigadierCommand.requiredArgumentBuilder("target", StringArgumentType.word())
                            .executes {
                                val target = StringArgumentType.getString(it, "target")
                                val asUuid = Uuid.parseOrNull(target)
                                val errorMessage: String? = try {
                                    if (asUuid != null) {
                                        WhitelistAPI.addToWhitelist(asUuid)
                                    } else {
                                        WhitelistAPI.addToWhitelist(target)
                                    }
                                    null
                                } catch (_: ProfileAPINotFoundException) {
                                    "msg.whitelisted.command.whitelist.mojang_api_not_found"
                                } catch (_: ProfileAPITooManyRequestsException) {
                                    "msg.whitelisted.command.whitelist.mojang_api_too_many_requests"
                                } catch (_: WhitelistDuplicateEntryException) {
                                    "msg.whitelisted.command.whitelist.add.duplicate"
                                } catch (_: Exception) {
                                    "msg.whitelisted.command.whitelist.error"
                                }
                                if (errorMessage != null) {
                                    it.source.sendMessage(Component.translatable(errorMessage).arguments(Component.text(target)).color(NamedTextColor.RED))
                                } else {
                                    it.source.sendMessage(Component.translatable("msg.whitelisted.command.whitelist.add.success")
                                        .arguments(Component.text(target)))
                                }
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                BrigadierCommand.literalArgumentBuilder("remove")
                    .requires { it.hasPermission("whitelisted.whitelist.remove") }
                    .then(
                        BrigadierCommand.requiredArgumentBuilder("target", StringArgumentType.word())
                            .executes {
                                val target = StringArgumentType.getString(it, "target")
                                val asUuid = Uuid.parseOrNull(target)
                                val errorMessage: String? = try {
                                    if (asUuid != null) {
                                        WhitelistAPI.removeFromWhitelist(asUuid)
                                    } else {
                                        WhitelistAPI.removeFromWhitelist(target)
                                    }
                                    null
                                } catch (_: WhitelistNotFoundException) {
                                    "msg.whitelisted.command.whitelist.remove.not_found"
                                } catch (_: Exception) {
                                    "msg.whitelisted.command.whitelist.error"
                                }
                                if (errorMessage != null) {
                                    it.source.sendMessage(Component.translatable(errorMessage).arguments(Component.text(target)).color(NamedTextColor.RED))
                                } else {
                                    it.source.sendMessage(Component.translatable("msg.whitelisted.command.whitelist.remove.success")
                                        .arguments(Component.text(target)))
                                }
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                BrigadierCommand.literalArgumentBuilder("list")
                    .requires { it.hasPermission("whitelisted.whitelist.list") }
                    .executes {
                        for (profile in WhitelistAPI.getAll()) {
                            it.source.sendMessage(Component.text("name=${profile.name}, uuid=${profile.uuid}"))
                        }
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                BrigadierCommand.literalArgumentBuilder("query")
                    .requires { it.hasPermission("whitelisted.whitelist.query") }
                    .then(
                        BrigadierCommand.requiredArgumentBuilder("target", StringArgumentType.word())
                            .executes {
                                val target = StringArgumentType.getString(it, "target")
                                val asUuid = Uuid.parseOrNull(target)
                                val result = if (asUuid != null) {
                                    WhitelistAPI.inWhitelist(uuid = asUuid)
                                } else {
                                    WhitelistAPI.inWhitelist(name = target)
                                }
                                it.source.sendMessage(Component.translatable(
                                    "msg.whitelisted.command.whitelist.query.$result",
                                    Component.text(target)
                                ))
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
    }
}