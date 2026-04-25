package os.kei.ui.page.main.mcp

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.mcp.server.McpServerManager
import os.kei.mcp.server.McpServerUiState
import os.kei.ui.page.main.mcp.util.buildMcpLogsExportJson

internal data class McpServiceDraft(
    val serverName: String,
    val portText: String,
    val allowExternal: Boolean
)

internal sealed interface McpToggleServerResult {
    data object Started : McpToggleServerResult
    data object Stopped : McpToggleServerResult
    data object InvalidPort : McpToggleServerResult
    data class Failed(val reason: String?) : McpToggleServerResult
}

internal sealed interface McpSaveConfigResult {
    data object Success : McpSaveConfigResult
    data object InvalidPort : McpSaveConfigResult
    data class Failed(val reason: String?) : McpSaveConfigResult
}

internal class McpPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun toggleServer(
        manager: McpServerManager,
        draft: McpServiceDraft
    ): McpToggleServerResult {
        if (manager.uiState.value.running) {
            withContext(ioDispatcher) {
                manager.stop()
            }
            return McpToggleServerResult.Stopped
        }
        val port = draft.portText.toIntOrNull() ?: return McpToggleServerResult.InvalidPort
        return withContext(ioDispatcher) {
            manager.updateServerName(draft.serverName)
            manager.start(port = port, allowExternal = draft.allowExternal)
                .onSuccess { manager.refreshAddresses() }
                .fold(
                    onSuccess = { McpToggleServerResult.Started },
                    onFailure = { McpToggleServerResult.Failed(it.message ?: it.javaClass.simpleName) }
                )
        }
    }

    suspend fun saveConfig(
        manager: McpServerManager,
        draft: McpServiceDraft
    ): McpSaveConfigResult {
        val port = draft.portText.toIntOrNull() ?: return McpSaveConfigResult.InvalidPort
        return withContext(ioDispatcher) {
            manager.updateServerName(draft.serverName)
            manager.updatePort(port).onFailure {
                return@withContext McpSaveConfigResult.Failed(it.message ?: it.javaClass.simpleName)
            }
            manager.updateAllowExternal(draft.allowExternal).onFailure {
                return@withContext McpSaveConfigResult.Failed(it.message ?: it.javaClass.simpleName)
            }
            McpSaveConfigResult.Success
        }
    }

    suspend fun resetConfigPreservingToken(manager: McpServerManager): Boolean {
        return withContext(ioDispatcher) {
            manager.resetServerConfigPreservingToken()
        }
    }

    suspend fun resetToken(manager: McpServerManager) {
        withContext(ioDispatcher) {
            manager.regenerateAuthToken()
        }
    }

    suspend fun sendTestNotification(manager: McpServerManager): Result<Unit> {
        return withContext(ioDispatcher) {
            manager.sendTestNotification()
        }
    }

    suspend fun refreshNow(manager: McpServerManager) {
        withContext(defaultDispatcher) {
            manager.refreshNow()
        }
    }

    suspend fun clearLogs(manager: McpServerManager) {
        withContext(defaultDispatcher) {
            manager.clearLogs()
        }
    }

    suspend fun buildConfigJson(
        manager: McpServerManager,
        serverState: McpServerUiState,
        draft: McpServiceDraft
    ): String {
        return withContext(ioDispatcher) {
            val port = draft.portText.toIntOrNull() ?: serverState.port
            val endpoint = if (draft.allowExternal && serverState.addresses.isNotEmpty()) {
                "http://${serverState.addresses.first()}:$port${serverState.endpointPath}"
            } else {
                "http://127.0.0.1:$port${serverState.endpointPath}"
            }
            manager.buildConfigJson(
                url = endpoint,
                includeJsonContentTypeHeader = draft.allowExternal
            )
        }
    }

    suspend fun exportLogs(
        contentResolver: ContentResolver,
        uri: Uri,
        generatedAt: String,
        state: McpServerUiState
    ) {
        val exportContent = withContext(defaultDispatcher) {
            buildMcpLogsExportJson(
                generatedAt = generatedAt,
                state = state
            )
        }
        withContext(ioDispatcher) {
            contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                checkNotNull(writer) { "openOutputStream returned null" }
                writer.write(exportContent)
            }
        }
    }
}
