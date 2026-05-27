package me.not_black.whitelisted.database.profile

import kotlinx.serialization.Serializable
import me.not_black.whitelisted.util.UuidSerializer
import kotlin.uuid.Uuid

@Serializable
data class ProfileEntry(@Serializable(with = UuidSerializer::class) val uuid: Uuid, val name: String, val timestamp: Long)