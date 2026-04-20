package com.example.keios.ui.page.main.student.catalog.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideCatalogEntryAvatar(
    imageUrl: String,
    fallbackRes: Int
) {
    if (imageUrl.isBlank()) {
        BaGuideCatalogEntryAvatarFallback(iconRes = fallbackRes)
    } else {
        BaGuideCatalogEntryAvatarImage(
            imageUrl = imageUrl,
            fallbackRes = fallbackRes
        )
    }
}

@Composable
private fun BaGuideCatalogEntryAvatarImage(
    imageUrl: String,
    fallbackRes: Int
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = BaGuideCatalogIconCache.get(imageUrl), imageUrl) {
        value = withContext(Dispatchers.IO) { BaGuideCatalogIconCache.getOrLoad(context, imageUrl) }
    }
    val rendered = bitmap
    if (rendered == null) {
        BaGuideCatalogEntryAvatarFallback(iconRes = fallbackRes)
        return
    }
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun BaGuideCatalogEntryAvatarFallback(iconRes: Int) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}
