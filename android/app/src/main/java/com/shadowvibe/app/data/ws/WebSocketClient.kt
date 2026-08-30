package com.shadowvibe.app.data.ws

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class StompClient(private val okHttpClient: OkHttpClient) {

    private var url = "ws://192.168.0.61:1010/ws-mobile"
    private var webSocket: WebSocket? = null
    private var connected = false
    private var subscriptions = mutableMapOf<String, Pair<String, (String) -> Unit>>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var onConnectedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var cookies: String = ""

    private var heartbeatRunnable: Runnable? = null
    private var reconnectRunnable: Runnable? = null
    private var reconnectDelay = 1000L
    private val maxReconnectDelay = 30000L
    private var shouldReconnect = true

    fun setUrl(newUrl: String) {
        url = newUrl
    }

    fun connect(cookies: String, onConnected: () -> Unit, onError: (String) -> Unit) {
        this.cookies = cookies
        this.onConnectedCallback = onConnected
        this.onErrorCallback = onError
        this.shouldReconnect = true
        this.reconnectDelay = 1000L
        doConnect()
    }

    private fun doConnect() {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (cookies.isNotBlank()) {
                    addHeader("Cookie", cookies)
                }
            }
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@StompClient.webSocket = webSocket
                sendStompConnect()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingFrame(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleIncomingFrame(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                mainHandler.post {
                    onErrorCallback?.invoke(t.message ?: "WebSocket connection failed")
                }
                handleDisconnect()
            }
        })
    }

    private fun sendStompConnect() {
        val frame = buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.2\n")
            append("heart-beat:10000,10000\n")
            if (cookies.isNotBlank()) {
                append("cookie:$cookies\n")
            }
            append("\n")
            append("\u0000")
        }
        webSocket?.send(frame)
    }

    private fun handleIncomingFrame(text: String) {
        val trimmed = text.trimEnd('\u0000')
        if (trimmed.isBlank()) return

        val commandEnd = trimmed.indexOf('\n')
        if (commandEnd == -1) return

        val command = trimmed.substring(0, commandEnd).trim()
        when (command) {
            "CONNECTED" -> handleConnected(trimmed)
            "MESSAGE" -> handleStompMessage(trimmed)
            "HEARTBEAT" -> { /* server heartbeat received, no action needed */ }
            "ERROR" -> handleStompError(trimmed)
            "RECEIPT" -> { /* receipt received */ }
        }
    }

    private fun handleConnected(frame: String) {
        connected = true
        reconnectDelay = 1000L
        startHeartbeat()
        resubscribeAll()
        mainHandler.post {
            onConnectedCallback?.invoke()
        }
    }

    private fun handleStompMessage(frame: String) {
        val bodyStart = frame.indexOf("\n\n")
        if (bodyStart == -1) return

        val headersPart = frame.substring(0, bodyStart)
        val body = frame.substring(bodyStart + 2).trim()

        val destination = parseHeader(headersPart, "destination") ?: return

        subscriptions.values
            .filter { it.first == destination }
            .forEach { handler ->
                mainHandler.post { handler.second(body) }
            }
    }

    private fun handleStompError(frame: String) {
        val bodyStart = frame.indexOf("\n\n")
        val body = if (bodyStart != -1) frame.substring(bodyStart + 2).trim() else ""
        mainHandler.post {
            onErrorCallback?.invoke("STOMP ERROR: $body")
        }
    }

    private fun parseHeader(headersBlock: String, key: String): String? {
        return headersBlock.lines()
            .firstOrNull { line ->
                line.startsWith("$key:")
            }
            ?.substringAfter(":")
            ?.trim()
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatRunnable = object : Runnable {
            override fun run() {
                if (connected) {
                    webSocket?.send("\n")
                    mainHandler.postDelayed(this, 10000)
                }
            }
        }
        mainHandler.postDelayed(heartbeatRunnable!!, 10000)
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { mainHandler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    private fun resubscribeAll() {
        subscriptions.forEach { (subId, entry) ->
            val destination = entry.first
            sendSubscribeFrame(subId, destination)
        }
    }

    private fun sendSubscribeFrame(subId: String, destination: String) {
        val frame = buildString {
            append("SUBSCRIBE\n")
            append("id:$subId\n")
            append("destination:$destination\n")
            append("\n")
            append("\u0000")
        }
        webSocket?.send(frame)
    }

    fun subscribe(destination: String, handler: (String) -> Unit): String {
        val subscriptionId = UUID.randomUUID().toString()
        subscriptions[subscriptionId] = Pair(destination, handler)
        if (connected) {
            sendSubscribeFrame(subscriptionId, destination)
        }
        return subscriptionId
    }

    fun unsubscribe(subscriptionId: String) {
        if (!subscriptions.containsKey(subscriptionId)) return

        val destination = subscriptions[subscriptionId]?.first
        subscriptions.remove(subscriptionId)

        if (connected && destination != null) {
            val frame = buildString {
                append("UNSUBSCRIBE\n")
                append("id:$subscriptionId\n")
                append("\n")
                append("\u0000")
            }
            webSocket?.send(frame)
        }
    }

    fun send(destination: String, body: String, headers: Map<String, String> = emptyMap()) {
        if (!connected) return

        val frame = buildString {
            append("SEND\n")
            append("destination:$destination\n")
            headers.forEach { (key, value) ->
                append("$key:$value\n")
            }
            append("\n")
            append(body)
            append("\u0000")
        }
        webSocket?.send(frame)
    }

    fun disconnect() {
        shouldReconnect = false
        stopHeartbeat()
        cancelReconnect()
        connected = false
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun isConnected(): Boolean = connected

    private fun handleDisconnect() {
        connected = false
        stopHeartbeat()
        if (shouldReconnect) {
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        cancelReconnect()
        reconnectRunnable = Runnable {
            doConnect()
        }
        mainHandler.postDelayed(reconnectRunnable!!, reconnectDelay)
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(maxReconnectDelay)
    }

    private fun cancelReconnect() {
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = null
    }
}
