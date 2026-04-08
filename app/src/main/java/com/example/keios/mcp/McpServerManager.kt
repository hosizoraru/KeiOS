package com.example.keios.mcp

import android.content.Context
import com.tencent.mmkv.MMKV
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class McpLogEntry(
    val time: String,
    val level: String,
    val message: String
)

data class McpServerUiState(
    val running: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 38888,
    val endpointPath: String = "/mcp",
    val allowExternal: Boolean = false,
    val addresses: List<String> = emptyList(),
    val lastError: String? = null,
    val tools: List<McpToolMeta> = emptyList(),
    val authToken: String = "",
    val serverName: String = "KeiOS MCP",
    val connectedClients: Int = 0,
    val logs: List<McpLogEntry> = emptyList()
) {
    val localEndpoint: String
        get() = "http://127.0.0.1:$port$endpointPath"

    val lanEndpoints: List<String>
        get() = addresses.map { "http://$it:$port$endpointPath" }
}

class McpServerManager(
    private val appContext: Context,
    private val localMcpService: LocalMcpService
) {
    private object Prefs {
        private const val KV_ID = "mcp_server_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SERVER_NAME = "server_name"
        private val random = SecureRandom()

        private fun kv() = MMKV.mmkvWithID(KV_ID)

        fun authToken(): String {
            val cached = kv().decodeString(KEY_AUTH_TOKEN).orEmpty()
            if (cached.isNotBlank()) return cached
            val generated = generateToken()
            kv().encode(KEY_AUTH_TOKEN, generated)
            return generated
        }

        fun serverName(): String {
            return kv().decodeString(KEY_SERVER_NAME, "KeiOS MCP").orEmpty().ifBlank { "KeiOS MCP" }
        }

        fun saveAuthToken(token: String) {
            kv().encode(KEY_AUTH_TOKEN, token)
        }

        fun saveServerName(name: String) {
            kv().encode(KEY_SERVER_NAME, name)
        }

        fun regenerateToken(): String {
            val token = generateToken()
            saveAuthToken(token)
            return token
        }

        private fun generateToken(): String {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            return bytes.joinToString("") { b -> "%02x".format(b) }
        }
    }

    private var engine: EmbeddedServer<*, *>? = null
    private var monitorJob: Job? = null
    private var lastConnectedCount: Int = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(
        McpServerUiState(
            tools = localMcpService.listLocalTools(),
            authToken = Prefs.authToken(),
            serverName = Prefs.serverName()
        )
    )
    val uiState: StateFlow<McpServerUiState> = _uiState.asStateFlow()

    init {
        localMcpService.bindMcpStateProvider { _uiState.value }
    }

    @Synchronized
    fun start(port: Int, allowExternal: Boolean): Result<Unit> {
        if (port !in 1..65535) {
            val message = "端口无效: $port"
            _uiState.value = _uiState.value.copy(lastError = message)
            return Result.failure(IllegalArgumentException(message))
        }
        return runCatching {
            val host = if (allowExternal) "0.0.0.0" else "127.0.0.1"
            val server = localMcpService.getOrCreateServer()
            val token = _uiState.value.authToken
            val newEngine = embeddedServer(
                factory = CIO,
                host = host,
                port = port
            ) {
                intercept(ApplicationCallPipeline.Plugins) {
                    val appCall = context
                    if (!appCall.request.path().startsWith("/mcp")) return@intercept
                    val authHeader = appCall.request.headers["Authorization"] ?: ""
                    if (authHeader != "Bearer $token") {
                        appendLog("WARN", "Rejected unauthorized request to /mcp")
                        appCall.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                        finish()
                    }
                }
                mcpStreamableHttp(path = "/mcp") { server }
            }
            stopInternal()
            newEngine.start(wait = false)
            engine = newEngine
            lastConnectedCount = 0
            startSessionMonitor(server)
            _uiState.value = _uiState.value.copy(
                running = true,
                host = host,
                port = port,
                allowExternal = allowExternal,
                addresses = if (allowExternal) ipv4Addresses() else emptyList(),
                connectedClients = 0,
                lastError = null
            )
            syncKeepAliveNotification(forceStart = true)
            appendLog("INFO", "MCP server started on $host:$port/mcp")
        }.onFailure {
            _uiState.value = _uiState.value.copy(
                running = false,
                lastError = it.message ?: it.javaClass.simpleName
            )
            appendLog("ERROR", "Failed to start server: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    @Synchronized
    fun regenerateAuthToken() {
        val token = Prefs.regenerateToken()
        _uiState.value = _uiState.value.copy(authToken = token)
        appendLog("INFO", "Authorization token regenerated")
    }

    @Synchronized
    fun updateServerName(name: String) {
        val fixed = name.trim().ifBlank { "KeiOS MCP" }
        Prefs.saveServerName(fixed)
        _uiState.value = _uiState.value.copy(serverName = fixed)
    }

    fun buildConfigJson(url: String? = null): String {
        val state = _uiState.value
        val endpoint = url ?: state.localEndpoint
        val escapedName = state.serverName.replace("\"", "\\\"")
        return """
{
  "mcpServers": {
    "$escapedName": {
      "type": "streamablehttp",
      "url": "$endpoint",
      "headers": {
        "Authorization": "Bearer ${state.authToken}"
      }
    }
  }
}
        """.trim()
    }

    @Synchronized
    fun stop() {
        stopInternal()
        _uiState.value = _uiState.value.copy(
            running = false,
            connectedClients = 0,
            lastError = null
        )
        runCatching { McpKeepAliveService.stop(appContext) }
        appendLog("INFO", "MCP server stopped")
    }

    @Synchronized
    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }

    @Synchronized
    fun refreshNow() {
        val running = _uiState.value.running
        if (_uiState.value.allowExternal) {
            _uiState.value = _uiState.value.copy(addresses = ipv4Addresses())
        }
        if (running) {
            val sessions = runCatching { localMcpService.getOrCreateServer().sessions.size }.getOrDefault(0)
            _uiState.value = _uiState.value.copy(connectedClients = sessions)
            syncKeepAliveNotification(forceStart = false)
            appendLog("INFO", "Snapshot refreshed: clients=$sessions")
        } else {
            appendLog("INFO", "Snapshot refreshed: server stopped")
        }
    }

    @Synchronized
    fun refreshAddresses() {
        if (!_uiState.value.allowExternal) return
        _uiState.value = _uiState.value.copy(addresses = ipv4Addresses())
    }

    @Synchronized
    fun sendTestNotification(): Result<Unit> {
        val state = _uiState.value
        return runCatching {
            McpNotificationHelper.notifyTest(
                context = appContext,
                running = state.running,
                port = state.port,
                path = state.endpointPath,
                clients = state.connectedClients
            )
            appendLog("INFO", "Test notification sent")
        }.onFailure {
            appendLog("ERROR", "Send test notification failed: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    @Synchronized
    private fun stopInternal() {
        monitorJob?.cancel()
        monitorJob = null
        lastConnectedCount = 0
        val current = engine ?: return
        runCatching { current.stop(gracePeriodMillis = 500, timeoutMillis = 2_000) }
        engine = null
    }

    private fun startSessionMonitor(server: io.modelcontextprotocol.kotlin.sdk.server.Server) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                val count = runCatching { server.sessions.size }.getOrDefault(0)
                if (count != lastConnectedCount) {
                    val old = lastConnectedCount
                    lastConnectedCount = count
                    _uiState.value = _uiState.value.copy(connectedClients = count)
                    syncKeepAliveNotification(forceStart = false)
                    if (count > old) {
                        appendLog("INFO", "Client connected, online=$count")
                    } else {
                        appendLog("INFO", "Client disconnected, online=$count")
                    }
                }
                delay(1200)
            }
        }
    }

    private fun appendLog(level: String, message: String) {
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = McpLogEntry(time = now, level = level, message = message)
        val current = _uiState.value.logs
        _uiState.value = _uiState.value.copy(logs = (current + entry).takeLast(120))
    }

    private fun ipv4Addresses(): List<String> {
        return runCatching {
            val result = mutableListOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching emptyList()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                if (!network.isUp || network.isLoopback) continue
                val addresses = network.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        result += addr.hostAddress.orEmpty()
                    }
                }
            }
            result.distinct()
        }.getOrDefault(emptyList())
    }

    private fun syncKeepAliveNotification(forceStart: Boolean) {
        val state = _uiState.value
        if (!state.running) return
        runCatching {
            McpKeepAliveService.startOrUpdate(
                context = appContext,
                running = state.running,
                port = state.port,
                path = state.endpointPath,
                clients = state.connectedClients,
                forceStart = forceStart
            )
        }.onFailure {
            appendLog("WARN", "KeepAlive notification update failed: ${it.message ?: it.javaClass.simpleName}")
        }
    }
}
