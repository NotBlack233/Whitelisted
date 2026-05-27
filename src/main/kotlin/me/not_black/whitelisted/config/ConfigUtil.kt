package me.not_black.whitelisted.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.reflect.KClass

inline fun <reified T : Any> MutableMap<String, JsonElement>.entryGetOrPut(key: String, default: JsonPrimitive, clazz: KClass<T>): T {
    if (get(key) == null)
        put(key, default)
    when (val element = get(key)) {
        is JsonPrimitive -> return when (clazz) {
            String::class -> element.toString() as T
            Boolean::class -> element.boolean as T
            Long::class -> element.long as T
            Double::class -> element.double as T
            Float::class -> element.float as T
            Int::class -> element.int as T
            else -> throw IllegalArgumentException()
        }
        else -> throw IllegalArgumentException()
    }
}

inline fun <reified T : Any> JsonElement.getPrimitiveValueOrNull(key: String, clazz: KClass<T>): T? {
    if (this !is JsonObject || this[key] !is JsonPrimitive)
        return null
    val content = (this[key] as JsonPrimitive).content
    return when (clazz) {
        String::class -> content as T
        Boolean::class -> content.toBooleanStrictOrNull() as T?
        Long::class -> content.toLongOrNull() as T?
        Double::class -> content.toDoubleOrNull() as T?
        Float::class -> content.toFloatOrNull() as T?
        Int::class -> content.toIntOrNull() as T?
        else -> throw IllegalArgumentException("Illegal class")
    }
}

//val JsonElement.intOrNull: Int? get() = (this as? JsonPrimitive)?.intOrNull
//val JsonElement.longOrNull: Long? get() = (this as? JsonPrimitive)?.longOrNull
//val JsonElement.doubleOrNull: Double? get() = (this as? JsonPrimitive)?.doubleOrNull
//val JsonElement.floatOrNull: Float? get() = (this as? JsonPrimitive)?.floatOrNull
//val JsonElement.booleanOrNull: Boolean? get() = (this as? JsonPrimitive)?.booleanOrNull

