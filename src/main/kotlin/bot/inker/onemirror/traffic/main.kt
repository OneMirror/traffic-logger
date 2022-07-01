package bot.inker.onemirror.traffic

import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.Methods
import io.undertow.util.StatusCodes
import io.undertow.websockets.core.*
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

fun main(args: Array<String>) {
    setupIo()
    Undertow.builder()
        .addHttpListener(OneMirrorProperties["port"].toInt(),OneMirrorProperties["host"],::handle)
        .build()
        .start()
}

private val logger by lazy { LoggerFactory.getLogger("logger-handler") }

private fun handle(exchange: HttpServerExchange){
    when(exchange.requestMethod){
        Methods.GET -> handleGet(exchange)
        Methods.POST -> handlePost(exchange)
        else -> {
            logger.warn("[{}] exchange.requestMethod({}) not allowed",
                exchange.connection.peerAddress,
                exchange.requestMethod
            )
            exchange.statusCode = StatusCodes.NO_CONTENT
            exchange.endExchange()
        }
    }
}

fun handleGet(exchange: HttpServerExchange) {
    val name = auth(exchange)?:return
    Handlers.websocket({ exchange, channel ->
        WebSockets.sendText(
            ByteBuffer.wrap("Hello, OneMirror traffic logger server.".toByteArray(StandardCharsets.UTF_8)),
            channel,
            null
        )
        channel.receiveSetter.set(object:AbstractReceiveListener(){
            override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) {
                LoggerWriter.log(name, message.data)
            }

            override fun onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage) {
                val data = message.data
                val out = ByteArrayOutputStream(data.resource.sumOf { it.remaining() })
                data.resource.forEach { out.write(it.array()) }
                LoggerWriter.log(name, out.toString(StandardCharsets.UTF_8))
            }
        })
        channel.resumeReceives()
    },{ exchange->
        val message = exchange.queryParameters["message"]?.firstOrNull()
        if (message != null) {
            LoggerWriter.log(name, message)
        }
        exchange.statusCode = StatusCodes.NO_CONTENT
        exchange.endExchange()
    }).handleRequest(exchange)
}

private fun handlePost(exchange: HttpServerExchange){
    val name = auth(exchange)?:return

    exchange.requestReceiver.receiveFullBytes { exchange, message ->
        LoggerWriter.log(name, message.toString(StandardCharsets.UTF_8))
        exchange.statusCode = StatusCodes.NO_CONTENT
        exchange.endExchange()
    }
}

private fun auth(exchange: HttpServerExchange):String?{
    val authorization = exchange.requestHeaders.getFirst(Headers.AUTHORIZATION)
    if(authorization == null){
        logger.warn("[{}] exchange.requestHeaders.getFirst(Headers.AUTHORIZATION) == null", exchange.connection.peerAddress)
        exchange.statusCode = StatusCodes.NO_CONTENT
        exchange.endExchange()
        return null
    }
    if (!authorization.startsWith("Basic ")) {
        logger.warn("[{}] !authorization.startsWith(\"Basic \")", exchange.connection.peerAddress)
        exchange.statusCode = StatusCodes.NO_CONTENT
        exchange.endExchange()
        return null
    }
    val authorizationEncoded = authorization.substring(/* "Basic ".length */ 6)
    val authorizationContent = try {
        Base64.getDecoder()
            .decode(authorizationEncoded)
            .toString(StandardCharsets.UTF_8)
    }catch (e:IllegalStateException){
        logger.warn("[{}] authorizationEncoded failed to parse as base64", exchange.connection.peerAddress, e)
        exchange.statusCode = StatusCodes.NO_CONTENT
        exchange.endExchange()
        return null
    }

    val name = authorizationContent.substringBefore(':')
    val secret = authorizationContent.substringAfter(':')
    if (name.isEmpty() || secret.isEmpty() || !SecurityManager.auth(name, secret)) {
        logger.warn("[{}] SecurityManager.auth(name, secret) == false", exchange.connection.peerAddress)
        exchange.statusCode = StatusCodes.NO_CONTENT
        exchange.endExchange()
        return null
    }
    return name
}
private fun setupIo(){
    System.setOut(
        IoBuilder.forLogger(LogManager.getLogger("STDOUT"))
            .setLevel(Level.INFO)
            .buildPrintStream()
    )
    System.setErr(
        IoBuilder.forLogger(LogManager.getLogger("STDERR"))
            .setLevel(Level.ERROR)
            .buildPrintStream()
    )
}