package me.not_black.whitelisted.database

import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.config.entry.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database

fun connect(config: DatabaseConfig): Database {
    return if (config.sqlite) {
        Database.connect("jdbc:sqlite:${Whitelisted.inst.dataDirectory.resolve(
            config.urlOrName ?: throw IllegalArgumentException("Database name must be set")
        ).toAbsolutePath()}", "org.sqlite.JDBC")
    } else {
        Database.connect(
            config.urlOrName ?: throw IllegalArgumentException("Database URL must be set"),
            config.driver ?: throw IllegalArgumentException("Database Driver must be set"),
            config.username ?: "",
            config.password ?: "",
        )
    }
}