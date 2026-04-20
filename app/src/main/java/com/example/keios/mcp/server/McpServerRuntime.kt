package com.example.keios.mcp.server

import android.content.Context
import com.example.keios.core.system.ShizukuApiUtils

object McpServerRuntime {
    @Volatile
    private var cachedManager: McpServerManager? = null

    private val lock = Any()

    fun getOrCreate(context: Context): McpServerManager {
        cachedManager?.let { return it }
        return synchronized(lock) {
            cachedManager?.let { return@synchronized it }
            val appContext = context.applicationContext
            val appLabel = runCatching {
                appContext.packageManager.getApplicationLabel(appContext.applicationInfo).toString()
            }.getOrDefault("KeiOS")
            val packageInfo = runCatching {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }.getOrNull()
            val localMcpService = LocalMcpService(
                appContext = appContext,
                shizukuApiUtils = ShizukuApiUtils(),
                appVersionName = packageInfo?.versionName ?: "unknown",
                appVersionCode = packageInfo?.longVersionCode ?: -1L,
                appPackageName = appContext.packageName,
                appLabel = appLabel
            )
            McpServerManager(
                appContext = appContext,
                localMcpService = localMcpService
            ).also { manager ->
                cachedManager = manager
            }
        }
    }
}
