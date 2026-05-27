package me.not_black.whitelisted.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.database.profile.ProfileEntry
import me.not_black.whitelisted.database.profile.ProfileEntryManager
import me.not_black.whitelisted.util.toUuid
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.relative
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object MojangAPI {
    private val mojangAPIConfig get() = Whitelisted.inst.config.mojangAPI
    private val httpClient = ApacheClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = LoggerFactory.getLogger("whitelisted-mojang_api")
    private val cache get() = ProfileEntryManager.cache

    fun getProfile(name: String): Pair<ProfileEntry?, Status> {
        val cacheEntry = cache.find(name)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry to Status.OK

        val resp = httpClient(
            Request(
                GET,
                Uri.of(mojangAPIConfig.apiServer).relative("/users/profiles/minecraft/$name")
            )
        )
        return handleResponse(resp).also { it.first?.let(if (expired) cache::update else cache::insert) }
    }

    fun getProfile(uuid: Uuid): Pair<ProfileEntry?, Status> {
        val cacheEntry = cache.find(uuid)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry to Status.OK

        val resp = httpClient(
            Request(
                GET,
                Uri.of(mojangAPIConfig.apiServer).relative("/session/minecraft/profile/${uuid.toHexString()}")
            )
        )
        return handleResponse(resp).also { it.first?.let(if (expired) cache::update else cache::insert) }
    }

    private fun handleResponse(resp: Response): Pair<ProfileEntry?, Status> = when (resp.status) {
        Status.OK -> {
            val receivedJson = json.decodeFromString<ReceivedJson>(resp.bodyString())
            val entry = ProfileEntry(receivedJson.id.toUuid(), receivedJson.name, System.currentTimeMillis())
            if (cache.exists(receivedJson.id.toUuid())) cache.update(entry) else cache.insert(entry)
            entry
        }
        Status.NOT_FOUND -> {
            null
        }
        Status.TOO_MANY_REQUESTS -> {
            logger.warn("Too many requests from Mojang API")
            null
        }
        else -> {
            logger.warn("Unexpected response from Mojang API: ${resp.status}")
            null
        }
    } to resp.status

    private fun expired(timestamp: Long): Boolean
        = System.currentTimeMillis() - timestamp > mojangAPIConfig.cacheExpireTime

    @Serializable
    private data class ReceivedJson(val id: String, val name: String)
}