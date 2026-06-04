package me.not_black.whitelisted.api.profile

import kotlinx.serialization.Serializable
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.database.profile.ProfileEntry
import kotlin.uuid.Uuid

interface ProfileAPI {
    fun getProfile(name: String): ProfileEntry

    fun getProfile(uuid: Uuid): ProfileEntry

    fun clearCache()

    /**
     * @param timestamp timestamp to be validated
     */
    fun expired(timestamp: Long): Boolean =
        System.currentTimeMillis() - timestamp > Whitelisted.inst.config.profileAPI.cacheExpireTime * 1000

    @Serializable
    data class ReceivedJson(val id: String, val name: String)
}