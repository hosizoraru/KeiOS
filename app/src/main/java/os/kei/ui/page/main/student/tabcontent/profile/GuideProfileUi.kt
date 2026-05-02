package os.kei.ui.page.main.student.tabcontent.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.GuideRemoteIcon
import os.kei.ui.page.main.student.GuideRemoteImage
import os.kei.ui.page.main.student.buildGuideTabCopyPayload
import os.kei.ui.page.main.student.extractGuideWebLinks
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.student.guideLocalizedValue
import os.kei.ui.page.main.student.guideTabCopyable
import os.kei.ui.page.main.student.rememberGuideTabCopyAction
import os.kei.ui.page.main.student.stripGuideWebLinks
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import os.kei.ui.page.main.widget.support.copyModeAwareRow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideProfileSectionHeader(
    title: String
) {
    val displayTitle = guideLocalizedLabel(title)
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = displayTitle,
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun GuideProfileInfoRows(
    rows: List<BaGuideRow>,
    rowContent: @Composable (BaGuideRow) -> Unit
) {
    rows.forEach { row ->
        rowContent(row)
    }
}

@Composable
internal fun GuideProfileInfoItem(
    key: String,
    value: String,
    onClick: (() -> Unit)? = null,
    valueColor: Color? = null,
    preferCapsule: Boolean = true
) {
    val displayKey = guideLocalizedLabel(key)
    val displayValue = guideLocalizedValue(value).ifBlank { "-" }
    val rowCopyAction =
        rememberGuideTabCopyAction(buildGuideTabCopyPayload(displayKey, displayValue))
    val showCapsule = preferCapsule && shouldUseProfileValueCapsule(
        displayKey,
        displayValue,
        onClick
    )
    CopyModeSelectionContainer {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .copyModeAwareRow(
                    copyPayload = buildGuideTabCopyPayload(displayKey, displayValue),
                    onClick = onClick,
                    onLongClick = rowCopyAction
                )
        ) {
            val keyMaxWidth = adaptiveProfileKeyMaxWidth(
                key = displayKey,
                value = displayValue,
                containerWidth = maxWidth
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = displayKey,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.widthIn(min = 52.dp, max = keyMaxWidth),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (showCapsule) {
                        GuideProfileValueCapsule(
                            label = displayValue,
                            tint = valueColor ?: Color(0xFF5FA8FF),
                            onClick = onClick,
                            onLongClick = rowCopyAction
                        )
                    } else {
                        Text(
                            text = displayValue,
                            color = valueColor ?: MiuixTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Medium,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GuideProfileValueCapsule(
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val localBackdrop = rememberLayerBackdrop()
    val activeBackdrop = localBackdrop.takeIf { LocalLiquidControlsEnabled.current }
    val shape = ContinuousCapsule
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.copyModeAwareRow(
            copyPayload = buildGuideTabCopyPayload("", label),
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    val capsuleModifier = Modifier
        .clip(shape)
        .then(
            if (activeBackdrop == null) {
                Modifier.background(tint.copy(alpha = if (isDark) 0.20f else 0.16f))
            } else {
                Modifier
            }
        )
        .border(
            width = 0.8.dp,
            color = tint.copy(alpha = if (isDark) 0.42f else 0.46f),
            shape = shape
        )
        .then(clickModifier)
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            color = if (isDark) tint else tint.copy(alpha = 0.92f),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
    Box {
        if (activeBackdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(localBackdrop)
            )
            LiquidSurface(
                backdrop = activeBackdrop,
                modifier = capsuleModifier,
                shape = shape,
                isInteractive = false,
                surfaceColor = tint.copy(alpha = if (isDark) 0.20f else 0.16f),
                blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Compact),
                lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Compact),
                shadow = false
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        } else {
            Box(
                modifier = capsuleModifier
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun GuideProfileRowsSection(
    rows: List<BaGuideRow>,
    emptyText: String,
    imageHeight: Dp = 96.dp
) {
    if (rows.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    val visibleRows = rows
        .take(120)
        .mapNotNull { row ->
            val cleanedValue = sanitizeProfileFieldValue(row.key, row.value)
            val isPlaceholderValue = isProfileValuePlaceholder(cleanedValue)
            val hasImage = row.imageUrl.isNotBlank() || row.imageUrls.any { it.isNotBlank() }
            val shouldDropRow =
                (isProfileInstructionPlaceholder(row.value) && isPlaceholderValue) ||
                    (isPlaceholderValue && !hasImage)
            if (shouldDropRow) {
                null
            } else {
                row.copy(value = cleanedValue)
            }
        }
    if (visibleRows.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    visibleRows.forEachIndexed { index, row ->
        val hasImage = row.imageUrl.isNotBlank()
        val value = row.value
            .takeIf { it.isNotBlank() && it != "图片" }
            ?: if (hasImage) stringResource(R.string.guide_label_see_image_below) else "-"
        val displayValue = guideLocalizedValue(value)
        GuideProfileInfoItem(
            key = row.key,
            value = displayValue,
            preferCapsule = false
        )
        if (hasImage) {
            Spacer(modifier = Modifier.height(6.dp))
            GuideRemoteImage(
                imageUrl = row.imageUrl,
                imageHeight = imageHeight
            )
        }
        if (index < visibleRows.lastIndex) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
internal fun GuideGalleryRelatedLinkRows(
    rows: List<BaGuideRow>,
    onOpenExternal: (String) -> Unit
) {
    if (rows.isEmpty()) {
        Text(
            text = stringResource(R.string.guide_gallery_no_related_links),
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }

    rows.forEachIndexed { index, row ->
        val links = extractGuideWebLinks(row.value)
        if (links.isEmpty()) return@forEachIndexed
        val noteText = stripGuideWebLinks(row.value)
        val keyText = guideLocalizedLabel(row.key, R.string.guide_gallery_related_link)
        val rowCopyPayload = buildGuideTabCopyPayload(
            key = keyText,
            value = buildString {
                if (noteText.isNotBlank()) {
                    append(noteText)
                }
                if (links.isNotEmpty()) {
                    if (isNotEmpty()) append('\n')
                    append(links.joinToString("\n"))
                }
            }.ifBlank { "-" }
        )
        val rowCopyAction = rememberGuideTabCopyAction(rowCopyPayload)

        CopyModeSelectionContainer {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
            ) {
                val keyMaxWidth = adaptiveProfileKeyMaxWidth(
                    key = keyText,
                    value = links.first(),
                    containerWidth = maxWidth
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .copyModeAwareRow(
                            copyPayload = rowCopyPayload,
                            onLongClick = rowCopyAction
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = keyText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.widthIn(min = 52.dp, max = keyMaxWidth),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (noteText.isNotBlank()) {
                            Text(
                                text = noteText,
                                color = MiuixTheme.colorScheme.onBackground,
                                textAlign = TextAlign.End,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip
                            )
                        }
                        links.forEach { link ->
                            val linkCopyAction =
                                rememberGuideTabCopyAction(buildGuideTabCopyPayload(keyText, link))
                            Text(
                                text = link,
                                color = Color(0xFF3B82F6),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.copyModeAwareRow(
                                    copyPayload = buildGuideTabCopyPayload(keyText, link),
                                    onClick = { onOpenExternal(link) },
                                    onLongClick = linkCopyAction
                                )
                            )
                        }
                    }
                }
            }
        }
        if (index < rows.lastIndex) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GuideGiftPreferenceGrid(
    items: List<GiftPreferenceItem>
) {
    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.guide_profile_no_gifts),
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }
    val isDark = isSystemInDarkTheme()
    val horizontalSpacing = 4.dp
    val minCardWidth = 78.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= (minCardWidth * 3 + horizontalSpacing * 2) -> 3
            maxWidth >= (minCardWidth * 2 + horizontalSpacing) -> 2
            else -> 1
        }
        val cardWidth = ((maxWidth - horizontalSpacing * (columns - 1)) / columns)
            .coerceAtLeast(72.dp)
        val giftBoxHeight = (cardWidth * 0.66f).coerceIn(56.dp, 76.dp)
        val giftIconWidth = (cardWidth + 4.dp).coerceIn(74.dp, 122.dp)
        val giftIconHeight = (giftBoxHeight + 2.dp).coerceAtLeast(48.dp)
        val emojiIconSize = (cardWidth * 0.16f).coerceIn(13.dp, 18.dp)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .guideTabCopyable(
                            buildGuideTabCopyPayload(
                                stringResource(R.string.guide_profile_gift),
                                item.label
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(giftBoxHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x163B82F6))
                            .border(
                                width = 0.8.dp,
                                color = Color(0x243B82F6),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        GuideRemoteIcon(
                            imageUrl = item.giftImageUrl,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-3).dp),
                            iconWidth = giftIconWidth,
                            iconHeight = giftIconHeight
                        )
                        if (item.emojiImageUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 3.dp, end = 3.dp)
                                    .clip(ContinuousCapsule)
                                    .background(
                                        if (isDark) Color(0x663B82F6) else Color(0xCCEFF6FF)
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = if (isDark) Color(0x553B82F6) else Color(0x553BA8FF),
                                        shape = ContinuousCapsule
                                    )
                                    .padding(horizontal = 3.dp, vertical = 3.dp)
                            ) {
                                GuideRemoteIcon(
                                    imageUrl = item.emojiImageUrl,
                                    iconWidth = emojiIconSize,
                                    iconHeight = emojiIconSize
                                )
                            }
                        }
                    }
                    Text(
                        text = item.label,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
