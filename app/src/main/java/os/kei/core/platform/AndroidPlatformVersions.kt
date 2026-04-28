package os.kei.core.platform

import android.annotation.SuppressLint
import android.os.Build

object AndroidPlatformVersions {
    const val ANDROID_16_API_LEVEL = 36
    const val ANDROID_17_API_LEVEL = 37
    const val ANDROID_16_FULL_API_LEVEL = 3_600_000
    const val ANDROID_16_1_FULL_API_LEVEL = 3_600_001
    const val ANDROID_17_FULL_API_LEVEL = 3_700_000

    val isAtLeastAndroid16: Boolean
        get() = Build.VERSION.SDK_INT >= ANDROID_16_API_LEVEL

    val isAtLeastAndroid17: Boolean
        get() = Build.VERSION.SDK_INT >= ANDROID_17_API_LEVEL

    val sdkIntFull: Int
        @SuppressLint("NewApi")
        get() = if (isAtLeastAndroid16) {
            Build.VERSION.SDK_INT_FULL
        } else {
            Build.VERSION.SDK_INT * 100_000
        }

    fun isAtLeastFull(fullApiLevel: Int): Boolean {
        return sdkIntFull >= fullApiLevel
    }
}
