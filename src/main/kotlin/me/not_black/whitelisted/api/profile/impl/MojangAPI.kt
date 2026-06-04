package me.not_black.whitelisted.api.profile.impl

import kotlinx.serialization.json.Json
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.api.profile.ProfileAPI
import me.not_black.whitelisted.database.profile.ProfileEntry
import me.not_black.whitelisted.database.profile.ProfileEntryManager
import me.not_black.whitelisted.exception.profileapi.ProfileAPIException
import me.not_black.whitelisted.exception.profileapi.ProfileAPINotFoundException
import me.not_black.whitelisted.exception.profileapi.ProfileAPITooManyRequestsException
import me.not_black.whitelisted.util.toUuid
import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object MojangAPI : ProfileAPI {
    const val API_URL: String = "https://api.mojang.com/"
    const val SESSION_URL: String = "https://sessionserver.mojang.com/"

    private val httpClient = ApacheClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = LoggerFactory.getLogger("whitelisted-mojang_api")
    private val cache = ProfileEntryManager("mojang_cache", Whitelisted.inst.cacheDb)

    /**
     * @param name player name
     * @throws ProfileAPIException
     */
    override fun getProfile(name: String): ProfileEntry {
        val cacheEntry = cache.find(name, caseSensitive = false)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry

        val resp = httpClient(
            Request(
                Method.GET,
                Uri.of(API_URL).appendToPath("/users/profiles/minecraft/$name")
            )
        )
        return handleResponse(resp).also { it.let(if (expired) cache::update else cache::insert) }
    }

    /**
     * @param uuid player UUID
     * @throws ProfileAPIException
     */
    override fun getProfile(uuid: Uuid): ProfileEntry {
        val cacheEntry = cache.find(uuid)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry

        val resp = httpClient(
            Request(
                Method.GET,
                Uri.of(SESSION_URL).appendToPath("/session/minecraft/profile/${uuid.toHexString()}")
            )
        )
        return handleResponse(resp).also { it.let(if (expired) cache::update else cache::insert) }
    }

    override fun clearCache() {
        cache.deleteAll()
    }

    /**
     * @throws ProfileAPIException
     */
    private fun handleResponse(resp: Response): ProfileEntry = when (resp.status) {
        Status.OK -> {
            val receivedJson = json.decodeFromString<ProfileAPI.ReceivedJson>(resp.bodyString())
            val entry = ProfileEntry(receivedJson.id.toUuid(), receivedJson.name, System.currentTimeMillis())
            if (cache.exists(receivedJson.id.toUuid())) cache.update(entry) else cache.insert(entry)
            entry
        }
        Status.NOT_FOUND -> throw ProfileAPINotFoundException()
        Status.TOO_MANY_REQUESTS -> {
            logger.warn("Too many requests from Mojang API")
            throw ProfileAPITooManyRequestsException()
        }
        else -> throw ProfileAPIException("Unexpected response from Mojang API: ${resp.status}")
    }
}