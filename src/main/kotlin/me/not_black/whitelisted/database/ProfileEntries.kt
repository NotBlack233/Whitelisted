package me.not_black.whitelisted.database

import org.jetbrains.exposed.v1.core.Table

class ProfileEntries(name: String) : Table(name) {
    val uuid = varchar("uuid", 36)  // 标记为主键列
    val name = varchar("name", 100)
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(uuid)
}