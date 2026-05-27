package me.not_black.whitelisted.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.not_black.whitelisted.Whitelisted
import me.not_black.whitelisted.api.WhitelistAPI
import me.not_black.whitelisted.crypto.AES256GCM
import me.not_black.whitelisted.crypto.ECDH
import me.not_black.whitelisted.util.toUuid
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.asServer
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKey
import kotlin.io.encoding.Base64

@Suppress("FunctionName")
fun WhitelistedServer(port: Int, host: String) = WhitelistedApp().asServer(UndertowWithHost(port, host))

@Suppress("FunctionName")
fun WhitelistedApp(): HttpHandler = CatchLensFailure.then(
    routes(
        "/ping" bind GET to { Response(Status.OK) },
        "/pubkey" bind GET to { Response(Status.OK).body(Base64.encode(Whitelisted.inst.keyPair.public.encoded)) },
        "/query" bind GET to ::handleQuery,
    )
)

private fun handleQuery(request: Request): Response {
//    fun invalidBase64Response(s: String) = Response(Status.BAD_REQUEST).body(encryptB64(Json.encodeToString(ResponseJson(false, errorMessage = "Invalid base64 $s"))))
//    val invalidKeySpecResponse by lazy { Response(Status.BAD_REQUEST).body(Json.encodeToString(ResponseJson(false, errorMessage = "Invalid key specification"))) }
//    val malformedJsonResponse by lazy { Response(Status.BAD_REQUEST).body(Json.encodeToString(ResponseJson(false, errorMessage = "Malformed JSON payload"))) }

    val peerPublicKey = try {
        ECDH.publicKeyFromBase64(Query.string().required("key")(request))
    } catch (_: Exception) {
        return Response(Status.BAD_REQUEST)
    }
    val key = ECDH.ecdhSharedSecret(Whitelisted.inst.keyPair.private, peerPublicKey)
    val payloadString = try {
        AES256GCM.decrypt(Query.string().required("payload")(request), key).toString(Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        return Response(Status.BAD_REQUEST).body(encryptB64(Json.encodeToString(ResponseJson(false, errorMessage = "Invalid base64 payload")), key))
    }
    val payload = try {
        Json.decodeFromString<QueryPayload>(payloadString)
    } catch (_: Exception) {
        return Response(Status.BAD_REQUEST).body(encryptB64(Json.encodeToString(ResponseJson(false, errorMessage = "Malformed JSON payload")), key))
    }
    if (payload.token != Whitelisted.inst.config.httpServer.token)
        return Response(Status.FORBIDDEN).body(encryptB64(Json.encodeToString(ResponseJson(false, errorMessage = "Invalid token")), key))

    return when {
        payload.uuid != null || payload.name != null || payload.timestamp != null -> Response(Status.OK).body(
            Json.encodeToString(ResponseJson(true, payload = encryptB64(
                WhitelistAPI.inWhitelist(payload.uuid?.toUuid(), payload.name, payload.timestamp).toString(),
                key
            )))
        )
        else -> Response(Status.OK).body(Base64.encode(AES256GCM.encrypt(Json.encodeToString(WhitelistAPI.getAll()).toByteArray(), key)))
    }
}

private fun encryptB64(b: String, key: SecretKey): String = Base64.encode(AES256GCM.encrypt(b.toByteArray(), key))

@Serializable
private data class ResponseJson(val ok: Boolean, val payload: String? = null, val errorMessage: String? = null)

@Serializable
private data class QueryPayload(val token: String? = null, val uuid: String? = null, val name: String? = null, val timestamp: Long? = null)
