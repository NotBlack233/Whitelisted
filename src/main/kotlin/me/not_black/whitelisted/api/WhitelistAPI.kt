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
        return if (resp != Status.OK) {
            Result.MOJANG_API_ERROR
        } else {
            profile?.uuid?.let { addToWhitelist(ProfileEntry(it, name, System.currentTimeMillis())) }
                ?: Result.UNKNOWN_ERROR
        }
    }

    fun addToWhitelist(uuid: Uuid): Result {
        val (profile, resp) = MojangAPI.getProfile(uuid)
        return if (resp != Status.OK) {
            Result.MOJANG_API_ERROR
        } else {
            profile?.name?.let { addToWhitelist(ProfileEntry(uuid, it, System.currentTimeMillis())) }
                ?: Result.UNKNOWN_ERROR
        }
    }

    fun addToWhitelist(profile: ProfileEntry): Result {
        if (whitelist.exists(profile.uuid, profile.name))
            return Result.DUPLICATE

        val byUuid = whitelist.find(profile.uuid)
        val byName = whitelist.find(profile.name)
        byUuid?.let(::warnAndDelete)
        byName?.let(::warnAndDelete)

        return if (whitelist.insert(profile)) Result.OK else Result.DB_ERROR
    }

    fun inWhitelist(uuid: Uuid? = null, name: String? = null, timestamp: Long? = null) =
        whitelist.exists(uuid, name, timestamp)

    fun inWhitelist(profile: ProfileEntry) = whitelist.exists(profile.uuid, profile.name, profile.timestamp)

    fun removeFromWhitelist(uuid: Uuid): Result = if (whitelist.delete(uuid)) Result.OK else Result.NOT_FOUND

    fun removeFromWhitelist(name: String): Result = if (whitelist.delete(name)) Result.OK else Result.NOT_FOUND

    fun getAll(): List<ProfileEntry> = whitelist.getAll()

    private fun warnAndDelete(profile: ProfileEntry) {
        logger.warn("Deleting possibly outdated profile uuid=${profile.uuid}, name=${profile.name} from whitelist")
        whitelist.delete(profile.uuid)
    }

    private interface AddResult

    enum class Result(val message: String) {
        OK("OK"),
        NOT_FOUND("Not found"),
        DUPLICATE("Duplicated profile"),
        MOJANG_API_ERROR("Error response from Mojang API"),
        DB_ERROR("Database error"),
        UNKNOWN_ERROR("Unknown error")
    }
}