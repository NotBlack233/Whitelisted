package me.not_black.whitelisted.util

import kotlin.uuid.Uuid

fun String.toUuid(): Uuid = Uuid.parse(this)

fun String.toUuidOrNull(): Uuid? = try { toUuid() } catch (_: Exception) { null }