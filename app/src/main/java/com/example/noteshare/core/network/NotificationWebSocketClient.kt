package com.example.noteshare.core.network

import com.example.noteshare.BuildConfig
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.feature.notification.domain.model.NotificationPush
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableSharedFlow<NotificationPush>(extraBufferCapacity = 10)
    val notifications: SharedFlow<NotificationPush> = _notifications.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    fun connect() {
        scope.launch {
            val token = tokenManager.tokenFlow.firstOrNull()
            if (token.isNullOrEmpty()) return@launch

            val wsUrl = BuildConfig.BASE_URL
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .trimEnd('/') + "ws/notification?token=$token"

            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    _connectionState.tryEmit(true)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val wrapper = json.decodeFromString<WebSocketMessage>(text)
                        if (wrapper.type == "NOTIFICATION" && wrapper.data != null) {
                            val dataStr = wrapper.data.toString()
                            val push = json.decodeFromString<NotificationPush>(dataStr)
                            _notifications.tryEmit(push)
                        }
                    } catch (e: Exception) {
                        // ignore parse errors
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    _connectionState.tryEmit(false)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    _connectionState.tryEmit(false)
                }
            })
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "App closing")
        webSocket = null
    }
}

@kotlinx.serialization.Serializable
private data class WebSocketMessage(
    val type: String,
    val data: kotlinx.serialization.json.JsonElement? = null
)
