package me.not_black.whitelisted.api

import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.api.profile.ProfileAPI
import me.not_black.whitelisted.api.profile.impl.MojangAPI
import me.not_black.whitelisted.database.profile.ProfileEntry
import me.not_black.whitelisted.database.profile.ProfileEntryManager
import me.not_black.whitelisted.exception.whitelist.WhitelistDuplicateEntryException
import me.not_black.whitelisted.exception.whitelist.WhitelistNotFoundException
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object WhitelistAPI {
    private val logger = LoggerFactory.getLogger("whitelisted-api")
    private val whitelist = ProfileEntryManager("whitelist", Whitelisted.inst.whitelistDb)
    var profileAPI: ProfileAPI = MojangAPI

    /**
     * @throws WhitelistDuplicateEntryException
     */
    fun addToWhitelist(name: String) = addToWhitelist(profileAPI.getProfile(name))

    /**
     * @throws WhitelistDuplicateEntryException
     */
    fun addToWhitelist(uuid: Uuid) = addToWhitelist(profileAPI.getProfile(uuid))

    /**
     * @throws WhitelistDuplicateEntryException
     */
    private fun addToWhitelist(profile: ProfileEntry): ProfileEntry {
        if (whitelist.exists(profile.uuid, profile.name))
            throw WhitelistDuplicateEntryException("Duplicate entry uuid=${profile.uuid}, name=${profile.name}")

        val byUuid = whitelist.find(profile.uuid)
        val byName = whitelist.find(profile.name)
        byUuid?.let(::warnAndDelete)
        byName?.let(::warnAndDelete)

        assert(whitelist.insert(profile))
        return profile
    }

    fun inWhitelist(uuid: Uuid? = null, name: String? = null, timestamp: Long? = null) =
        whitelist.exists(uuid, name, timestamp, caseSensitive = false)

    /**
     * @throws WhitelistNotFoundException
     */
    fun removeFromWhitelist(uuid: Uuid) {
        if (!whitelist.delete(uuid))
            throw WhitelistNotFoundException("UUID not found: $uuid")
    }

    /**
     * @throws WhitelistNotFoundException
     */
    fun removeFromWhitelist(name: String) {
        if (!whitelist.delete(name, caseSensitive = false))
            throw WhitelistNotFoundException("Name not found: $name")
    }

    fun getAll() = whitelist.getAll()

    private fun warnAndDelete(profile: ProfileEntry) {
        logger.warn("Deleting possibly outdated profile uuid=${profile.uuid}, name=${profile.name} from whitelist")
        whitelist.delete(profile.uuid)
    }
}