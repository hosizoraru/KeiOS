package os.kei.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object LocalNetworkPermissionCompat {
    fun requiredPermissionOrNull(): String? {
        return when {
            AndroidPlatformVersions.isAtLeastAndroid17 -> Manifest.permission.ACCESS_LOCAL_NETWORK
            AndroidPlatformVersions.isAtLeastAndroid16 -> Manifest.permission.NEARBY_WIFI_DEVICES
            else -> null
        }
    }

    fun hasPermission(context: Context): Boolean {
        val permission = requiredPermissionOrNull() ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
