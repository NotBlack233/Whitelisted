package me.not_black.whitelisted.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.database.profile.ProfileEntry
import me.not_black.whitelisted.database.profile.ProfileEntryManager
import me.not_black.whitelisted.exception.mojangapi.MojangAPIException
import me.not_black.whitelisted.exception.mojangapi.MojangAPINotFoundException
import me.not_black.whitelisted.exception.mojangapi.MojangAPITooManyRequestsException
import me.not_black.whitelisted.util.toUuid
import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object MojangAPI {
    private val mojangAPIConfig get() = Whitelisted.inst.config.mojangAPI
    private val httpClient = ApacheClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = LoggerFactory.getLogger("whitelisted-mojang_api")
    private val cache get() = ProfileEntryManager.cache

    /**
     * @param name player name
     * @throws MojangAPIException
     */
    fun getProfile(name: String): ProfileEntry {
        val cacheEntry = cache.find(name, caseSensitive = false)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry

        val resp = httpClient(
            Request(
                GET,
                Uri.of(mojangAPIConfig.apiServer).relative("/users/profiles/minecraft/$name")
            )
        )
        return handleResponse(resp).also { it.let(if (expired) cache::update else cache::insert) }
    }

    /**
     * @param uuid player UUID
     * @throws MojangAPIException
     */
    fun getProfile(uuid: Uuid): ProfileEntry {
        val cacheEntry = cache.find(uuid)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry

        val resp = httpClient(
            Request(
                GET,
                Uri.of(mojangAPIConfig.apiServer).relative("/session/minecraft/profile/${uuid.toHexString()}")
            )
        )
        return handleResponse(resp).also { it.let(if (expired) cache::update else cache::insert) }
    }

    /**
     * @throws MojangAPIException
     */
    private fun handleResponse(resp: Response): ProfileEntry = when (resp.status) {
        Status.OK -> {
            val receivedJson = json.decodeFromString<ReceivedJson>(resp.bodyString())
            val entry = ProfileEntry(receivedJson.id.toUuid(), receivedJson.name, System.currentTimeMillis())
            if (cache.exists(receivedJson.id.toUuid())) cache.update(entry) else cache.insert(entry)
            entry
        }
        Status.NOT_FOUND -> throw MojangAPINotFoundException()
        Status.TOO_MANY_REQUESTS -> {
            logger.warn("Too many requests from Mojang API")
            throw MojangAPITooManyRequestsException()
        }
        else -> throw MojangAPIException("Unexpected response from Mojang API: ${resp.status}")
    }

    /**
     * @param timestamp timestamp to be validated
     */
    private fun expired(timestamp: Long): Boolean
        = System.currentTimeMillis() - timestamp > mojangAPIConfig.cacheExpireTime

    @Serializable
    private data class ReceivedJson(val id: String, val name: String)
}