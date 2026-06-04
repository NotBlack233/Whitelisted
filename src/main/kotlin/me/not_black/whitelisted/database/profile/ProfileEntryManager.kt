package me.not_black.whitelisted.database.profile

import me.not_black.whitelisted.database.ProfileEntries
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

class ProfileEntryManager(private val table: String, private val db: Database) {
    private val logger by lazy { LoggerFactory.getLogger("whitelisted-db-$table") }
    private val profileEntries = ProfileEntries(table)

    init {
        transaction(db) {
            SchemaUtils.create(profileEntries)
        }
    }

    fun insert(entry: ProfileEntry): Boolean = transaction(db) {
        val exists = profileEntries.selectAll().where { profileEntries.uuid eq entry.uuid.toString() }.any()
        if (exists) {
            logger.debug("Insertion failed: duplicate entry {} exists", entry.uuid)
            return@transaction false
        }
        profileEntries.insert {
            it[uuid] = entry.uuid.toString()
            it[name] = entry.name
            it[timestamp] = entry.timestamp
        }
        logger.debug("Inserted entry {} successfully", entry.uuid)
        true
    }

    fun find(uuid: Uuid): ProfileEntry? = transaction(db) {
        profileEntries.selectAll().where { profileEntries.uuid eq uuid.toString() }
            .mapNotNull { row ->
                ProfileEntry(
                    uuid = Uuid.parse(row[profileEntries.uuid]),  // 字符串转 Uuid
                    name = row[profileEntries.name],
                    timestamp = row[profileEntries.timestamp]
                )
            }
            .singleOrNull()
    }

    fun find(name: String, caseSensitive: Boolean = true): ProfileEntry? = transaction(db) {
        profileEntries.selectAll().where {
            if (caseSensitive)
                profileEntries.name eq name
            else
                LowerCase(profileEntries.name) eq name.lowercase()
        }
            .mapNotNull { row ->
                ProfileEntry(
                    uuid = Uuid.parse(row[profileEntries.uuid]),  // 字符串转 Uuid
                    name = row[profileEntries.name],
                    timestamp = row[profileEntries.timestamp]
                )
            }
            .singleOrNull()
    }

    fun getAll(): List<ProfileEntry> = transaction(db) {
        profileEntries.selectAll()
            .map { row ->
                ProfileEntry(
                    uuid = Uuid.parse(row[profileEntries.uuid]),
                    name = row[profileEntries.name],
                    timestamp = row[profileEntries.timestamp]
                )
            }
    }

    fun update(entry: ProfileEntry): Boolean = transaction(db) {
        val updatedRows = profileEntries.update({ profileEntries.uuid eq entry.uuid.toString() }) {
            it[name] = entry.name
            it[timestamp] = entry.timestamp
        }
        (updatedRows > 0).also { if (it) logger.debug("Updated entry {} successfully", entry.uuid) }
    }

    fun delete(uuid: Uuid): Boolean = transaction(db) {
        val deletedRows = profileEntries.deleteWhere { profileEntries.uuid eq uuid.toString() }
        (deletedRows > 0).also { if (it) logger.debug("Deleted entry {} successfully", uuid) }
    }

    fun delete(name: String, caseSensitive: Boolean = true): Boolean = transaction(db) {
        val deletedRows = profileEntries.deleteWhere { if (caseSensitive) profileEntries.name eq name else LowerCase(profileEntries.name) eq name.lowercase() }
        (deletedRows > 0).also { if (it) logger.debug("Deleted entry '{}' successfully by name", name) }
    }

    fun exists(uuid: Uuid? = null, name: String? = null, timestamp: Long? = null, caseSensitive: Boolean = true): Boolean = transaction(db) {
        val query = profileEntries.selectAll()
        uuid?.let { query.andWhere { profileEntries.uuid eq it.toString() } }
        name?.let { query.andWhere { if (caseSensitive) profileEntries.name eq name else LowerCase(profileEntries.name) eq name.lowercase() } }
        timestamp?.let { query.andWhere { profileEntries.timestamp eq timestamp } }
        query.any()
    }

    fun deleteAll() = transaction(db) {
        profileEntries.deleteAll()
    }
}