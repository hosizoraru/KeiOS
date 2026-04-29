package os.kei.mcp.server

import android.content.Context
import os.kei.core.system.ShizukuApiUtils

internal class McpToolEnvironment(
    val appContext: Context,
    val shizukuApiUtils: ShizukuApiUtils,
    val appVersionName: String,
    val appVersionCode: Long,
    val appPackageName: String,
    val appLabel: String,
    private val stateProvider: () -> McpServerUiState?
) {
    fun currentState(): McpServerUiState? = stateProvider()
}
