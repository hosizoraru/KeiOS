package os.kei.core.platform

import android.content.pm.ActivityInfo

internal object MainWindowOrientationPolicy {
    private const val LARGE_WINDOW_MIN_SMALLEST_WIDTH_DP = 600

    /**
     * Re-evaluate after folding or resizing without treating landscape width as a large window.
     * Android 16+ can ignore orientation requests on large displays; this expresses the app's
     * preference without opting out of the platform's adaptive-window rules.
     */
    fun requestFor(
        smallestScreenWidthDp: Int,
        currentRequestedOrientation: Int,
    ): Int? {
        if (smallestScreenWidthDp <= 0) return null
        val desiredOrientation =
            if (smallestScreenWidthDp >= LARGE_WINDOW_MIN_SMALLEST_WIDTH_DP) {
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        return desiredOrientation.takeUnless { it == currentRequestedOrientation }
    }
}
