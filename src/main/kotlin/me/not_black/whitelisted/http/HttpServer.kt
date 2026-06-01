package me.not_black.whitelisted.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.api.WhitelistAPI
import me.not_black.whitelisted.exception.mojangapi.MojangAPINotFoundException
import me.not_black.whitelisted.exception.mojangapi.MojangAPITooManyRequestsException
import me.not_black.whitelisted.exception.whitelist.WhitelistDuplicateEntryException
import me.not_black.whitelisted.exception.whitelist.WhitelistNotFoundException
import me.not_black.whitelisted.util.toUuidOrNull
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.long
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.asServer
import kotlin.uuid.Uuid

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
    return try {
        val profile = when {
            uuid != null -> WhitelistAPI.addToWhitelist(uuid)
            name != null -> WhitelistAPI.addToWhitelist(name)
            else -> return noArgumentResponse
        }
        ResponseJson(true, Json.encodeToJsonElement(profile)).toOkResponse()
    } catch (_: MojangAPINotFoundException) {
        ResponseJson(false, JsonNull, errorCode = ErrorCode.MOJANG_API_NOT_FOUND, errorMessage = "Mojang API not found").toResponse(Status.NOT_FOUND)
    } catch (_: MojangAPITooManyRequestsException) {
        ResponseJson(false, JsonNull, errorCode = ErrorCode.MOJANG_API_TOO_MANY_REQUESTS, errorMessage = "Mojang API too many requests").toResponse(Status.TOO_MANY_REQUESTS)
    } catch (_: WhitelistDuplicateEntryException) {
        ResponseJson(false, JsonNull, errorCode = ErrorCode.WHITELIST_DUPLICATE_ENTRY, errorMessage = "Duplicate entry").toResponse(Status.BAD_REQUEST)
    } catch (e: Exception) {
        ResponseJson(false, JsonNull, errorCode = ErrorCode.UNKNOWN, errorMessage = e.message).toResponse(Status.INTERNAL_SERVER_ERROR)
    }
}

private fun handleRemove(request: Request): Response {
    if (!validateToken(request))
        return invalidTokenResponse

    val (uuid, name) = request.queryUuidAndName()
    return try {
        when {
            uuid != null -> WhitelistAPI.removeFromWhitelist(uuid)
            name != null -> WhitelistAPI.removeFromWhitelist(name)
            else -> return noArgumentResponse
        }
        ResponseJson(true, JsonNull).toOkResponse()
    } catch (_: WhitelistNotFoundException) {
        ResponseJson(false, JsonNull, errorCode = ErrorCode.WHITELIST_NOT_FOUND, errorMessage = "Database not found").toResponse(Status.NOT_FOUND)
    } catch (e: Exception) {
        ResponseJson(false, JsonNull, errorCode = ErrorCode.UNKNOWN, errorMessage = e.message).toResponse(Status.INTERNAL_SERVER_ERROR)
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
    JsonNull, errorCode = ErrorCode.INVALID_TOKEN, errorMessage = "Invalid token").toResponse(Status.FORBIDDEN) }

private val noArgumentResponse by lazy { ResponseJson(false,
    JsonNull, errorCode = ErrorCode.INVALID_ARGUMENT, errorMessage = "At least one argument is required").toResponse(Status.BAD_REQUEST) }

@Serializable
private data class ResponseJson(val ok: Boolean, val data: JsonElement, val errorCode: Int? = null, val errorMessage: String? = null) {
    fun toOkResponse(): Response = Response(Status.OK).body(Json.encodeToString(this))
    fun toResponse(status: Status): Response = Response(status).body(Json.encodeToString(this))
}

private object ErrorCode {
    const val UNKNOWN = 0
    const val INVALID_TOKEN = 101
    const val INVALID_ARGUMENT = 102
    const val MOJANG_API_NOT_FOUND = 1001
    const val MOJANG_API_TOO_MANY_REQUESTS = 1002
    const val WHITELIST_DUPLICATE_ENTRY = 1003
    const val WHITELIST_NOT_FOUND = 1004
}