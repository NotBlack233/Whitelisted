package me.not_black.whitelisted.api.profile.impl

import kotlinx.serialization.encodeToString
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

object YggdrasilAPI : ProfileAPI {
    private val config get() = Whitelisted.inst.config.profileAPI
    private val cache = ProfileEntryManager("yggdrasil_cache", Whitelisted.inst.cacheDb)
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = LoggerFactory.getLogger("whitelisted-yggdrasil_api")
    private val httpClient = ApacheClient()

    override fun getProfile(name: String): ProfileEntry {
        val cacheEntry = cache.find(name, caseSensitive = false)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry

        val resp = httpClient(
            Request(
                Method.POST,
                Uri.of(config.url!!).appendToPath("/api/profiles/minecraft"),
            ).body(Json.encodeToString(arrayOf(name))).header("Content-Type", "application/json")
        )
        handleResponse(resp)
        val received = json.decodeFromString<Array<ProfileAPI.ReceivedJson>>(resp.bodyString()).getOrNull(0)
            ?: throw ProfileAPINotFoundException()
        return ProfileEntry(received.id.toUuid(), received.name, System.currentTimeMillis()).also {
            it.let(if (expired) cache::update else cache::insert)
        }
    }

    override fun getProfile(uuid: Uuid): ProfileEntry {
        val cacheEntry = cache.find(uuid)
        val expired = if (cacheEntry == null) false else expired(cacheEntry.timestamp)
        if (!expired && cacheEntry != null)
            return cacheEntry

        val resp = httpClient(
            Request(
                Method.GET,
                Uri.of(config.url!!).appendToPath("/sessionserver/session/minecraft/profile/${uuid.toHexString()}"),
            ).query("unsigned", "true")
        )
        handleResponse(resp)
        val received = json.decodeFromString<ProfileAPI.ReceivedJson>(resp.bodyString())
        return ProfileEntry(received.id.toUuid(), received.name, System.currentTimeMillis()).also {
            it.let(if (expired) cache::update else cache::insert)
        }
    }

    override fun clearCache() {
        cache.deleteAll()
    }

    private fun handleResponse(resp: Response) = when (resp.status) {
        Status.OK -> Unit
        Status.NO_CONTENT, Status.NOT_FOUND -> throw ProfileAPINotFoundException()
        Status.TOO_MANY_REQUESTS -> {
            logger.warn("Too many requests from Yggdrasil API")
            throw ProfileAPITooManyRequestsException()
        }
        else -> throw ProfileAPIException("Unexpected response from Yggdrasil API: ${resp.status}")
    }
}