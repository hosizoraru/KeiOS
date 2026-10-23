package os.kei.core.notification.identity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt
import os.kei.core.notification.R
import os.kei.core.prefs.LauncherIconDesign
import os.kei.core.prefs.UiPrefs

object NotificationAppIconResolver {
    private const val LARGE_ICON_SIZE_DP = 48
    private val largeIconCache = ConcurrentHashMap<String, Bitmap>()
    @Volatile
    private var runtimeDesign: LauncherIconDesign? = null

    @DrawableRes
    fun smallIconResId(): Int = R.drawable.ic_kei_notification_small

    fun largeIconBitmap(context: Context): Bitmap? {
        val design = currentDesign()
        val cacheKey = "${context.packageName}|${design.storageId}|${context.resources.displayMetrics.densityDpi}"
        largeIconCache[cacheKey]?.let { return it }
        // Own the vector state before rendering into the notification bitmap cache.
        val drawable = resolveActiveLauncherDrawable(context)?.mutate() ?: return null
        return drawable.renderLargeIcon(context).also { bitmap ->
            largeIconCache[cacheKey] = bitmap
        }
    }

    fun applyDesign(design: LauncherIconDesign) {
        if (runtimeDesign == design) return
        runtimeDesign = design
        largeIconCache.clear()
    }

    private fun currentDesign(): LauncherIconDesign =
        runtimeDesign
            ?: runCatching { UiPrefs.getLauncherIconDesign() }
                .getOrDefault(LauncherIconDesign.Android)

    private fun resolveActiveLauncherDrawable(context: Context): Drawable? {
        val packageManager = context.packageManager
        val launcherIntent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.packageName)
        return packageManager
            .queryIntentActivities(launcherIntent, 0)
            .firstOrNull()
            ?.loadIcon(packageManager)
    }

    private fun Drawable.renderLargeIcon(context: Context): Bitmap {
        val targetSize =
            max(
                1,
                (context.resources.displayMetrics.density * LARGE_ICON_SIZE_DP).roundToInt(),
            )
        if (this is BitmapDrawable && bitmap.width == targetSize && bitmap.height == targetSize) {
            return bitmap
        }
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val previousBounds = Rect(bounds)
        setBounds(0, 0, targetSize, targetSize)
        draw(canvas)
        setBounds(previousBounds)
        return result
    }
}
