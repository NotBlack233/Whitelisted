package me.not_black.whitelisted.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.api.WhitelistAPI
import me.not_black.whitelisted.api.WhitelistAPI.Result
import me.not_black.whitelisted.util.toUuid
import me.not_black.whitelisted.util.toUuidOrNull
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.long
import org.http4k.lens.string
import org.http4k.lens.uuid
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.asServer
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@Suppress("FunctionName")
fun WhitelistedServer(port: Int, host: String) = WhitelistedApp().asServer(UndertowWithHost(port, host))

@Suppress("FunctionName")
fun WhitelistedApp(): HttpHandler = CatchLensFailure.then(
    routes(
        "/ping" bind GET to { Response(Status.OK) },
        "/query" bind GET to ::handleQuery,
        "/list" bind GET to ::handleList,
        "/add" bind GET to ::handleAdd,
        "/remove" bind GET to ::handleRemove
    )
)

private fun handleAdd(request: Request): Response {
    if (!validateToken(request))
        return invalidTokenResponse

    val (uuid, name) = request.queryUuidAndName()
    val result = when {
        uuid != null -> WhitelistAPI.addToWhitelist(uuid)
        name != null -> WhitelistAPI.addToWhitelist(name)
        else -> return noArgumentResponse
    }
    return when (result) {
        Result.OK -> ResponseJson(true, JsonNull).toOkResponse()
        Result.MOJANG_API_ERROR -> ResponseJson(false, JsonNull, errorMessage = "Mojang API error").toResponse(Status.INTERNAL_SERVER_ERROR)
        Result.MOJANG_API_NOT_FOUND -> ResponseJson(false, JsonNull, errorMessage = "Mojang API not found").toResponse(Status.NOT_FOUND)
        Result.DB_ERROR -> ResponseJson(false, JsonNull, errorMessage = "Database error").toResponse(Status.INTERNAL_SERVER_ERROR)
        Result.DUPLICATE -> ResponseJson(false, JsonNull, errorMessage = "Duplicate entry").toResponse(Status.BAD_REQUEST)
        else -> ResponseJson(false, JsonNull, "Unknown error").toResponse(Status.INTERNAL_SERVER_ERROR)
    }
}

private fun handleRemove(request: Request): Response {
    if (!validateToken(request))
        return invalidTokenResponse

    val (uuid, name) = request.queryUuidAndName()
    val result = when {
        uuid != null -> WhitelistAPI.removeFromWhitelist(uuid)
        name != null -> WhitelistAPI.removeFromWhitelist(name)
        else -> return noArgumentResponse
    }
    return when (result) {
        Result.OK -> ResponseJson(true, JsonNull).toOkResponse()
        Result.MOJANG_API_ERROR -> ResponseJson(false, JsonNull, errorMessage = "Mojang API error").toResponse(Status.INTERNAL_SERVER_ERROR)
        Result.MOJANG_API_NOT_FOUND -> ResponseJson(false, JsonNull, errorMessage = "Mojang API not found").toResponse(Status.NOT_FOUND)
        Result.DB_ERROR -> ResponseJson(false, JsonNull, errorMessage = "Database error").toResponse(Status.INTERNAL_SERVER_ERROR)
        else -> ResponseJson(false, JsonNull, "Unknown error").toResponse(Status.INTERNAL_SERVER_ERROR)
    }
}

private fun handleList(request: Request): Response {
    if (!validateToken(request))
        return invalidTokenResponse

    val list = WhitelistAPI.getAll()
    return ResponseJson(true, Json.encodeToJsonElement(list)).toOkResponse()
}

private fun handleQuery(request: Request): Response {
    if (!validateToken(request))
        return invalidTokenResponse

    val (uuid, name) = request.queryUuidAndName()
    val timestamp = Query.long().optional("timestamp")(request)

    return if (uuid != null || name != null || timestamp != null) ResponseJson(
        true, JsonPrimitive(WhitelistAPI.inWhitelist(uuid, name, timestamp))
    ).toOkResponse() else noArgumentResponse
}

private fun validateToken(request: Request): Boolean {
    val configToken = Whitelisted.inst.config.httpServer.token
    return if (configToken != null) {
        val token = Query.string().optional("token")(request) ?: return false
        token == configToken
    } else true
}

private fun Request.queryUuidAndName(): Pair<Uuid?, String?> =
    Query.string().optional("uuid")(this)?.toUuidOrNull() to
            Query.string().optional("name")(this)

private val invalidTokenResponse by lazy { ResponseJson(false,
    JsonNull, errorMessage = "Invalid token").toResponse(Status.FORBIDDEN) }

private val noArgumentResponse by lazy { ResponseJson(false,
    JsonNull, "At least one argument is required").toResponse(Status.BAD_REQUEST) }

@Serializable
private data class ResponseJson(val ok: Boolean, val data: JsonElement, val errorMessage: String? = null) {
    fun toOkResponse(): Response = Response(Status.OK).body(Json.encodeToString(this))
    fun toResponse(status: Status): Response = Response(status).body(Json.encodeToString(this))
}