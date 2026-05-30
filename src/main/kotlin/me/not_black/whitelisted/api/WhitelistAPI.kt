package me.not_black.whitelisted.api

import me.not_black.whitelisted.database.profile.ProfileEntry
import me.not_black.whitelisted.database.profile.ProfileEntryManager
import org.http4k.core.Status
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object WhitelistAPI {
    private val logger = LoggerFactory.getLogger("whitelisted-api")
    private val whitelist get() = ProfileEntryManager.whitelist

    fun addToWhitelist(name: String): Result {
        val (profile, resp) = MojangAPI.getProfile(name)
        return when (resp) {
            Status.OK -> profile?.let { addToWhitelist(ProfileEntry(it.uuid, it.name, System.currentTimeMillis())) }
                ?: Result.UNKNOWN_ERROR
            Status.NOT_FOUND -> Result.MOJANG_API_NOT_FOUND
            else -> Result.MOJANG_API_ERROR
        }
    }

    fun addToWhitelist(uuid: Uuid): Result {
        val (profile, resp) = MojangAPI.getProfile(uuid)
        return when (resp) {
            Status.OK -> profile?.let { addToWhitelist(ProfileEntry(it.uuid, it.name, System.currentTimeMillis())) }
                ?: Result.UNKNOWN_ERROR
            Status.NOT_FOUND -> Result.MOJANG_API_NOT_FOUND
            else -> Result.MOJANG_API_ERROR
        }
    }

    private fun addToWhitelist(profile: ProfileEntry): Result {
        if (whitelist.exists(profile.uuid, profile.name))
            return Result.DUPLICATE

        val byUuid = whitelist.find(profile.uuid)
        val byName = whitelist.find(profile.name)
        byUuid?.let(::warnAndDelete)
        byName?.let(::warnAndDelete)

        return if (whitelist.insert(profile)) Result.OK else Result.DB_ERROR
    }

    fun inWhitelist(uuid: Uuid? = null, name: String? = null, timestamp: Long? = null) =
        whitelist.exists(uuid, name, timestamp, caseSensitive = false)

    private fun inWhitelist(profile: ProfileEntry) = whitelist.exists(profile.uuid, profile.name, profile.timestamp)

    fun removeFromWhitelist(uuid: Uuid): Result = if (whitelist.delete(uuid)) Result.OK else Result.DB_NOT_FOUND

    fun removeFromWhitelist(name: String): Result = if (whitelist.delete(name, caseSensitive = false)) Result.OK else Result.DB_NOT_FOUND

    fun getAll(): List<ProfileEntry> = whitelist.getAll()

    private fun warnAndDelete(profile: ProfileEntry) {
        logger.warn("Deleting possibly outdated profile uuid=${profile.uuid}, name=${profile.name} from whitelist")
        whitelist.delete(profile.uuid)
    }

    enum class Result(val message: String) {
        OK("OK"),
        DB_NOT_FOUND("Not found in database"),
        DUPLICATE("Duplicated profile"),
        MOJANG_API_NOT_FOUND("Not found from Mojang API"),
        MOJANG_API_ERROR("Error response from Mojang API"),
        DB_ERROR("Database error"),
        UNKNOWN_ERROR("Unknown error")
    }
}