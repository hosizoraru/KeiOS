package os.kei.mcp.server

import android.content.Context
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.mcp.service.McpKeepAliveService
import os.kei.mcp.notification.McpNotificationHelper
import com.tencent.mmkv.MMKV
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
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
    val runningSinceEpochMs: Long = 0L,
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

private data class McpPrefsSnapshot(
    val authToken: String = "",
    val serverName: String = "KeiOS MCP",
    val port: Int = 38888,
    val allowExternal: Boolean = false
)

class McpServerManager(
    private val appContext: Context,
    private val localMcpService: LocalMcpService
) {
    companion object {
        private const val TAG = "McpServerManager"
        private const val DEFAULT_SERVER_NAME = "KeiOS MCP"
        private const val DEFAULT_PORT = 38888

        fun loadSavedCacheSummary(context: Context): String {
            val snapshot = Prefs.loadSnapshot()
            val tokenState = context.getString(
                if (snapshot.authToken.isBlank()) {
                    R.string.settings_cache_entry_mcp_token_empty
                } else {
                    R.string.settings_cache_entry_mcp_token_saved
                }
            )
            val network = context.getString(
                if (snapshot.allowExternal) {
                    R.string.settings_cache_entry_mcp_network_lan
                } else {
                    R.string.settings_cache_entry_mcp_network_local
                }
            )
            return context.getString(
                R.string.settings_cache_entry_mcp_detail,
                snapshot.port,
                network,
                tokenState
            )
        }

        fun clearSavedCacheOnly() {
            Prefs.clear()
        }

        fun storageFootprintBytes(): Long = Prefs.storageFootprintBytes()

        fun actualDataBytes(): Long = Prefs.actualDataBytes()

        fun configBytesEstimated(): Long = Prefs.configBytesEstimated()
    }

    private object Prefs {
        private const val KV_ID = "mcp_server_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SERVER_NAME = "server_name"
        private const val KEY_PORT = "port"
        private const val KEY_ALLOW_EXTERNAL = "allow_external"
        private val random = SecureRandom()
        private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

        private fun kv() = store

        fun loadSnapshot(): McpPrefsSnapshot {
            val store = kv()
            val port = store.decodeInt(KEY_PORT, DEFAULT_PORT).let { value ->
                if (value in 1..65535) value else DEFAULT_PORT
            }
            return McpPrefsSnapshot(
                authToken = store.decodeString(KEY_AUTH_TOKEN).orEmpty(),
                serverName = store.decodeString(KEY_SERVER_NAME, DEFAULT_SERVER_NAME).orEmpty().ifBlank { DEFAULT_SERVER_NAME },
                port = port,
                allowExternal = store.decodeBool(KEY_ALLOW_EXTERNAL, false)
            )
        }

        fun ensureAuthToken(current: String): String {
            if (current.isNotBlank()) return current
            val generated = generateToken()
            saveAuthToken(generated)
            return generated
        }

        fun saveAuthToken(token: String) {
            kv().encode(KEY_AUTH_TOKEN, token)
        }

        fun saveServerName(name: String) {
            kv().encode(KEY_SERVER_NAME, name)
        }

        fun savePort(port: Int) {
            if (port in 1..65535) {
                kv().encode(KEY_PORT, port)
            }
        }

        fun saveAllowExternal(allowExternal: Boolean) {
            kv().encode(KEY_ALLOW_EXTERNAL, allowExternal)
        }

        fun regenerateToken(): String {
            val token = generateToken()
            saveAuthToken(token)
            return token
        }

        fun clear() {
            val store = kv()
            store.removeValueForKey(KEY_AUTH_TOKEN)
            store.removeValueForKey(KEY_SERVER_NAME)
            store.removeValueForKey(KEY_PORT)
            store.removeValueForKey(KEY_ALLOW_EXTERNAL)
            store.trim()
        }

        fun storageFootprintBytes(): Long = kv().totalSize()

        fun actualDataBytes(): Long = kv().actualSize()

        fun configBytesEstimated(): Long {
            val snapshot = loadSnapshot()
            return (snapshot.authToken.length + snapshot.serverName.length).toLong() * 2 + 32L
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
    private val initialPrefsSnapshot = Prefs.loadSnapshot()
    private val _uiState = MutableStateFlow(
        McpServerUiState(
            port = initialPrefsSnapshot.port,
            allowExternal = initialPrefsSnapshot.allowExternal,
            tools = localMcpService.listLocalTools(),
            authToken = initialPrefsSnapshot.authToken,
            serverName = initialPrefsSnapshot.serverName
        )
    )
    val uiState: StateFlow<McpServerUiState> = _uiState.asStateFlow()

    init {
        localMcpService.bindMcpStateProvider { _uiState.value }
    }

    @Synchronized
    fun start(port: Int, allowExternal: Boolean): Result<Unit> {
        if (port !in 1..65535) {
            val message = "${appContext.getString(R.string.common_port_invalid)}: $port"
            _uiState.value = _uiState.value.copy(lastError = message)
            return Result.failure(IllegalArgumentException(message))
        }
        return runCatching {
            ensureAuthToken()
            val host = if (allowExternal) "0.0.0.0" else "127.0.0.1"
            val current = _uiState.value
            if (current.running && current.port == port && current.allowExternal == allowExternal) {
                McpServerRuntimeRegistry.registerRunning(this)
                refreshNow()
                syncKeepAliveNotification(forceStart = false)
                appendLog("INFO", "MCP server already running on $host:$port")
                return@runCatching
            }
            stopInternal()
            ensurePortAvailable(host, port)

            val server = localMcpService.getOrCreateServer()
            val newEngine = embeddedServer(
                factory = CIO,
                host = host,
                port = port
            ) {
                intercept(ApplicationCallPipeline.Plugins) {
                    val appCall = context
                    val requestPath = appCall.request.path()
                    if (!requestPath.startsWith("/mcp")) return@intercept

                    val authHeaderRaw = appCall.request.headers["Authorization"].orEmpty()
                    val providedToken = extractBearerToken(authHeaderRaw)
                    val expectedToken = _uiState.value.authToken
                    if (providedToken != expectedToken) {
                        val mode = describeAuthHeader(authHeaderRaw)
                        val message = "Rejected unauthorized request: path=$requestPath auth=$mode"
                        appendLog("WARN", message)
                        AppLogger.w(TAG, message)
                        appCall.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                        finish()
                        return@intercept
                    }
                }
                mcpStreamableHttp(path = "/mcp") { server }
            }
            newEngine.start(wait = false)
            engine = newEngine
            McpServerRuntimeRegistry.registerRunning(this)
            lastConnectedCount = 0
            startSessionMonitor(server)
            _uiState.value = _uiState.value.copy(
                running = true,
                runningSinceEpochMs = System.currentTimeMillis(),
                host = host,
                port = port,
                allowExternal = allowExternal,
                addresses = if (allowExternal) ipv4Addresses() else emptyList(),
                connectedClients = 0,
                lastError = null
            )
            Prefs.savePort(port)
            Prefs.saveAllowExternal(allowExternal)
            syncKeepAliveNotification(forceStart = true)
            appendLog("INFO", "MCP server started on $host:$port/mcp")
        }.onFailure {
            _uiState.value = _uiState.value.copy(
                running = false,
                runningSinceEpochMs = 0L,
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
        val fixed = name.trim().ifBlank { DEFAULT_SERVER_NAME }
        Prefs.saveServerName(fixed)
        _uiState.value = _uiState.value.copy(serverName = fixed)
    }

    @Synchronized
    fun resetServerConfigPreservingToken(): Boolean {
        Prefs.saveServerName(DEFAULT_SERVER_NAME)
        Prefs.savePort(DEFAULT_PORT)
        Prefs.saveAllowExternal(false)
        val running = _uiState.value.running
        _uiState.value = if (running) {
            _uiState.value.copy(
                serverName = DEFAULT_SERVER_NAME,
                lastError = null
            )
        } else {
            _uiState.value.copy(
                serverName = DEFAULT_SERVER_NAME,
                port = DEFAULT_PORT,
                allowExternal = false,
                addresses = emptyList(),
                lastError = null
            )
        }
        if (running) {
            syncKeepAliveNotification(forceStart = false)
        }
        appendLog("INFO", "Server config reset to defaults")
        return running
    }

    @Synchronized
    fun updatePort(port: Int): Result<Unit> {
        if (port !in 1..65535) {
            val message = "${appContext.getString(R.string.common_port_invalid)}: $port"
            _uiState.value = _uiState.value.copy(lastError = message)
            return Result.failure(IllegalArgumentException(message))
        }
        Prefs.savePort(port)
        val current = _uiState.value
        _uiState.value = if (current.running) {
            current.copy(lastError = null)
        } else {
            current.copy(port = port, lastError = null)
        }
        return Result.success(Unit)
    }

    @Synchronized
    fun updateAllowExternal(allowExternal: Boolean): Result<Unit> {
        Prefs.saveAllowExternal(allowExternal)
        val current = _uiState.value
        _uiState.value = if (current.running) {
            current.copy(lastError = null)
        } else {
            current.copy(allowExternal = allowExternal, lastError = null)
        }
        return Result.success(Unit)
    }

    fun getSkillMarkdown(): String {
        return localMcpService.getSkillMarkdownForUi()
    }

    fun buildConfigJson(
        url: String? = null,
        includeJsonContentTypeHeader: Boolean = _uiState.value.allowExternal
    ): String {
        val authToken = ensureAuthToken()
        val state = _uiState.value
        val endpoint = url ?: state.localEndpoint
        val escapedName = state.serverName.replace("\"", "\\\"")
        val headersText = if (includeJsonContentTypeHeader) {
            """
        "Authorization": "Bearer $authToken",
        "Content-Type": "application/json"
            """.trimIndent()
        } else {
            """
        "Authorization": "Bearer $authToken"
            """.trimIndent()
        }
        return """
{
  "mcpServers": {
    "$escapedName": {
      "type": "streamablehttp",
      "url": "$endpoint",
      "headers": {
${headersText.prependIndent("        ")}
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
            runningSinceEpochMs = 0L,
            connectedClients = 0,
            lastError = null
        )
        runCatching { McpKeepAliveService.Companion.stop(appContext) }
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
    fun refreshNotificationNow() {
        if (!_uiState.value.running) return
        syncKeepAliveNotification(forceStart = false)
    }

    @Synchronized
    fun sendTestNotification(): Result<Unit> {
        val state = _uiState.value
        return runCatching {
            McpNotificationHelper.notifyTest(
                context = appContext,
                serverName = state.serverName,
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
        McpServerRuntimeRegistry.clearRunning(this)
    }

    private fun startSessionMonitor(server: Server) {
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

    private fun ensurePortAvailable(host: String, port: Int) {
        runCatching {
            ServerSocket().use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(host, port))
            }
        }.getOrElse {
            throw IllegalStateException(
                appContext.getString(R.string.mcp_error_port_in_use, port, it.message.orEmpty()),
                it
            )
        }
    }

    private fun extractBearerToken(rawHeader: String): String {
        if (rawHeader.isBlank()) return ""
        val parts = rawHeader.trim().split(Regex("\\s+"), limit = 2)
        if (parts.size < 2 || !parts[0].equals("Bearer", ignoreCase = true)) return ""
        return parts[1].trim().trim('"')
    }

    private fun describeAuthHeader(rawHeader: String): String {
        if (rawHeader.isBlank()) return "missing"
        val token = extractBearerToken(rawHeader)
        if (token.isBlank()) return "invalid-format"
        return "bearer(len=${token.length})"
    }

    private fun syncKeepAliveNotification(forceStart: Boolean) {
        val state = _uiState.value
        if (!state.running) return
        runCatching {
            McpKeepAliveService.Companion.startOrUpdate(
                context = appContext,
                serverName = state.serverName,
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

    @Synchronized
    private fun ensureAuthToken(): String {
        val current = _uiState.value.authToken
        val token = Prefs.ensureAuthToken(current)
        if (token != current) {
            _uiState.value = _uiState.value.copy(authToken = token)
        }
        return token
    }
}
