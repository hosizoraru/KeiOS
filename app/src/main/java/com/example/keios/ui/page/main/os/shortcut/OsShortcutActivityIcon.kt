package com.example.keios.ui.page.main

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

private object ActivityIconBitmapCache {
    private val cache = ConcurrentHashMap<String, Bitmap>()
    private val missingKeys = ConcurrentHashMap.newKeySet<String>()

    fun get(key: String): Bitmap? = cache[key]

    fun isMissing(key: String): Boolean = missingKeys.contains(key)

    fun put(key: String, bitmap: Bitmap?) {
        if (bitmap == null) {
            missingKeys.add(key)
            return
        }
        cache[key] = bitmap
    }
}

@Composable
internal fun ShortcutActivityIcon(
    packageName: String,
    className: String,
    size: Dp,
    fallbackToPackageIcon: Boolean = true
) {
    val context = LocalContext.current
    val normalizedPackageName = packageName.trim()
    val normalizedClassName = className.trim()
    val iconCacheKey = "$normalizedPackageName#$normalizedClassName"

    val bitmapState = produceState<Bitmap?>(
        initialValue = ActivityIconBitmapCache.get(iconCacheKey),
        normalizedPackageName,
        normalizedClassName
    ) {
        if (normalizedPackageName.isBlank()) return@produceState
        if (ActivityIconBitmapCache.isMissing(iconCacheKey)) return@produceState
        if (value != null) return@produceState

        value = withContext(Dispatchers.IO) {
            val activityBitmap = loadActivityIconBitmap(
                context = context,
                packageName = normalizedPackageName,
                className = normalizedClassName
            )
            ActivityIconBitmapCache.put(iconCacheKey, activityBitmap)
            activityBitmap
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = iconCacheKey,
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule)
        )
    } else if (fallbackToPackageIcon && normalizedPackageName.isNotBlank()) {
        AppIcon(packageName = normalizedPackageName, size = size)
    } else {
        Box(
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.github_strategy_app_fallback),
                color = MiuixTheme.colorScheme.primary,
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight
            )
        }
    }
}

private fun loadActivityIconBitmap(
    context: Context,
    packageName: String,
    className: String
): Bitmap? {
    if (className.isBlank()) return null
    val normalizedClassName = if (className.startsWith(".")) {
        "$packageName$className"
    } else {
        className
    }
    val componentName = ComponentName(packageName, normalizedClassName)
    val iconDrawable = runCatching {
        context.packageManager.getActivityIcon(componentName)
    }.getOrNull() ?: return null

    val sizePx = max(36, (context.resources.displayMetrics.density * 40f).toInt())
    return iconDrawable.toBitmap(sizePx)
}

private fun Drawable.toBitmap(sizePx: Int): Bitmap {
    if (this is BitmapDrawable) {
        bitmap?.let { bitmapValue ->
            if (bitmapValue.width == sizePx && bitmapValue.height == sizePx) {
                return bitmapValue
            }
            return Bitmap.createScaledBitmap(bitmapValue, sizePx, sizePx, true)
        }
    }

    val width = if (intrinsicWidth > 0) intrinsicWidth else sizePx
    val height = if (intrinsicHeight > 0) intrinsicHeight else sizePx
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return if (width == sizePx && height == sizePx) {
        bitmap
    } else {
        Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    }
}
