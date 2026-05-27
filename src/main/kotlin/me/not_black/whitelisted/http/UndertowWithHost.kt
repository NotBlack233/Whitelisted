package me.not_black.whitelisted.http

import io.undertow.Undertow
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.buildHttp4kUndertowServer
import org.http4k.server.buildUndertowHandlers
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

class UndertowWithHost(private val port: Int = 8000, private val host: String = "0.0.0.0", override val stopMode: StopMode
) : PolyServerConfig {
    constructor(port: Int = 8000, host: String = "0.0.0.0") : this(port, host, Immediate)

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        val (httpHandler, multiProtocolHandler) = buildUndertowHandlers(http, ws, sse, stopMode)

        return Undertow.builder()
            .addHttpListener(port, host)
            .setWorkerThreads(32 * Runtime.getRuntime().availableProcessors())
            .setHandler(multiProtocolHandler)
            .buildHttp4kUndertowServer(httpHandler, stopMode, port)
    }
}