package os.kei.core.security

import android.content.Context
import os.kei.core.platform.AndroidPlatformVersions

object AdvancedProtectionCompat {
    private const val ADVANCED_PROTECTION_MANAGER_CLASS =
        "android.security.advancedprotection.AdvancedProtectionManager"

    fun isAvailable(): Boolean {
        return AndroidPlatformVersions.isAtLeastAndroid17
    }

    fun isEnabled(context: Context): Boolean? {
        if (!isAvailable()) return null
        return runCatching {
            val managerClass = Class.forName(ADVANCED_PROTECTION_MANAGER_CLASS)
            val manager = context.getSystemService(managerClass) ?: return@runCatching null
            managerClass.getMethod("isAdvancedProtectionEnabled").invoke(manager) as? Boolean
        }.getOrNull()
    }
}
