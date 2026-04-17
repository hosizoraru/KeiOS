package com.example.keios.ui.page.main.student

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.keios.R
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.buildTextCopyPayload
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import com.example.keios.ui.page.main.widget.rememberLightTextCopyAction
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Composable
internal fun GuideProfileSectionHeader(
    title: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
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
    val displayKey = key.ifBlank { "信息" }
    val displayValue = value.ifBlank { "-" }
    val rowCopyAction = rememberGuideTabCopyAction(buildGuideTabCopyPayload(displayKey, displayValue))
    val showCapsule = preferCapsule && shouldUseProfileValueCapsule(displayKey, displayValue, onClick)
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
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.copyModeAwareRow(
            copyPayload = buildGuideTabCopyPayload("", label),
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .then(clickModifier)
            .background(tint.copy(alpha = if (isDark) 0.20f else 0.16f))
            .border(
                width = 0.8.dp,
                color = tint.copy(alpha = if (isDark) 0.42f else 0.46f),
                shape = ContinuousCapsule
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isDark) tint else tint.copy(alpha = 0.92f),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun GuideProfileRowsSection(
    rows: List<BaGuideRow>,
    emptyText: String,
    imageHeight: androidx.compose.ui.unit.Dp = 96.dp
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
            ?: if (hasImage) "见下图" else "-"
        GuideProfileInfoItem(
            key = row.key.ifBlank { "信息" },
            value = value,
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
            text = "暂无影画相关链接。",
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }

    rows.forEachIndexed { index, row ->
        val links = extractGuideWebLinks(row.value)
        if (links.isEmpty()) return@forEachIndexed
        val noteText = stripGuideWebLinks(row.value)
        val keyText = row.key.ifBlank { "影画链接" }
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
                            val linkCopyAction = rememberGuideTabCopyAction(buildGuideTabCopyPayload(keyText, link))
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
            text = "暂无礼物偏好条目。",
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
                        .guideTabCopyable(buildGuideTabCopyPayload("礼物", item.label)),
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
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 3.dp)
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

internal fun extractOrderedNumbers(raw: String): List<Int> {
    if (raw.isBlank()) return emptyList()
    return Regex("""\d+""")
        .findAll(raw)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
}

internal fun sortKeyNumbers(raw: String): Pair<Int, Int> {
    val numbers = extractOrderedNumbers(raw)
    if (numbers.isEmpty()) return -1 to -1
    val first = numbers.getOrElse(0) { -1 }
    val second = numbers.getOrElse(1) { -1 }
    return first to second
}

internal fun sortProfileRowsByKeyNumbers(rows: List<BaGuideRow>): List<BaGuideRow> {
    return rows.sortedWith(
        compareBy<BaGuideRow>(
            { sortKeyNumbers(it.key).first },
            { sortKeyNumbers(it.key).second },
            { normalizeProfileFieldKey(it.key) }
        )
    )
}

internal fun sortGalleryItemsByTitleNumbers(items: List<BaGuideGalleryItem>): List<BaGuideGalleryItem> {
    return items.sortedWith(
        compareBy<BaGuideGalleryItem>(
            { sortKeyNumbers(normalizeGalleryTitle(it.title)).first },
            { sortKeyNumbers(normalizeGalleryTitle(it.title)).second },
            { normalizeGalleryTitle(it.title) }
        )
    )
}

internal data class ProfileFieldSpec(
    val title: String,
    val aliases: List<String>,
    val hideWhenEmpty: Boolean = false
)

internal data class SameNameRoleItem(
    val name: String,
    val linkUrl: String,
    val imageUrl: String
)

internal data class GiftPreferenceItem(
    val label: String,
    val giftImageUrl: String,
    val emojiImageUrl: String
)

internal val profileNicknameFieldSpecs = listOf(
    ProfileFieldSpec("角色名称", listOf("角色名称")),
    ProfileFieldSpec("全名", listOf("全名")),
    ProfileFieldSpec("假名注音", listOf("假名注音", "假名注明")),
    ProfileFieldSpec("繁中译名", listOf("繁中译名")),
    ProfileFieldSpec("简中译名", listOf("简中译名"))
)

internal val profileStudentInfoFieldSpecs = listOf(
    ProfileFieldSpec("年龄", listOf("年龄")),
    ProfileFieldSpec("生日", listOf("生日")),
    ProfileFieldSpec("身高", listOf("身高")),
    ProfileFieldSpec("画师", listOf("画师", "原画师")),
    ProfileFieldSpec("实装日期", listOf("实装日期", "首次登场日期")),
    ProfileFieldSpec("声优", listOf("声优")),
    ProfileFieldSpec("角色考据", listOf("角色考据"), hideWhenEmpty = true),
    ProfileFieldSpec("设计", listOf("设计", "设计师"), hideWhenEmpty = true)
)

internal val profileHobbyFieldSpecs = listOf(
    ProfileFieldSpec("兴趣爱好", listOf("兴趣爱好")),
    ProfileFieldSpec("个人简介", listOf("个人简介")),
    ProfileFieldSpec("MomoTalk状态消息", listOf("MomoTalk状态消息", "Momotalk状态消息")),
    ProfileFieldSpec("MomoTalk解锁等级", listOf("MomoTalk解锁等级", "Momotalk解锁等级"), hideWhenEmpty = true)
)

internal val profileStructuredFieldSpecs = profileNicknameFieldSpecs + profileStudentInfoFieldSpecs + profileHobbyFieldSpecs

internal fun normalizeProfileFieldKey(raw: String): String {
    return raw
        .replace(" ", "")
        .replace("　", "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
        .lowercase()
}

internal val profileRoleReferenceFieldKey = normalizeProfileFieldKey("角色考据")
internal val relatedSameNameRoleHeaderKey = normalizeProfileFieldKey("相关同名角色")
internal val sameNameRoleNameRowKey = normalizeProfileFieldKey("同名角色名称")
internal val giftPreferenceRowPrefixKey = normalizeProfileFieldKey("礼物偏好礼物")
internal val profileSectionHeaderKeys = setOf("介绍", "学生信息", "信息")
    .map(::normalizeProfileFieldKey)
    .toSet()
internal val profileCapsuleFieldKeys = setOf(
    "角色名称", "年龄", "生日", "身高",
    "实装日期", "MomoTalk解锁等级",
    "繁中译名", "简中译名", "假名注音", "假名注明"
).map(::normalizeProfileFieldKey).toSet()
internal val profileLongTextFieldKeys = setOf(
    "全名", "个人简介", "兴趣爱好", "MomoTalk状态消息"
).map(::normalizeProfileFieldKey).toSet()
internal val profileLinkTitleCache = ConcurrentHashMap<String, String>()

internal fun shouldUseProfileValueCapsule(
    key: String,
    value: String,
    onClick: (() -> Unit)?
): Boolean {
    if (onClick != null) return false
    if (isProfileValuePlaceholder(value)) return false
    val normalizedKey = normalizeProfileFieldKey(key)
    if (value.length > 12 || value.contains('\n')) return false
    if (value.contains("http", ignoreCase = true)) return false
    if (value.contains("/") || value.contains(" / ")) return false
    if (value.contains("：") || value.contains(":")) return false
    if (normalizedKey in profileLongTextFieldKeys) return false
    if (normalizedKey in profileCapsuleFieldKeys) return true
    return value.length <= 8 && !value.contains(" ")
}

internal fun adaptiveProfileKeyMaxWidth(
    key: String,
    value: String,
    containerWidth: Dp
): Dp {
    val keyLength = key.trim().length
    val baseWidth = when {
        keyLength >= 12 -> 148.dp
        keyLength >= 10 -> 138.dp
        keyLength >= 8 -> 124.dp
        keyLength >= 6 -> 110.dp
        else -> 94.dp
    }
    val valuePenalty = when {
        value.length >= 64 -> 24.dp
        value.length >= 40 -> 16.dp
        value.length >= 24 -> 8.dp
        else -> 0.dp
    }
    val preferred = (baseWidth - valuePenalty).coerceAtLeast(84.dp)
    val containerLimit = (containerWidth * 0.48f).coerceAtLeast(84.dp)
    return preferred.coerceAtMost(containerLimit)
}

internal fun isSameNameRoleRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key == relatedSameNameRoleHeaderKey || key == sameNameRoleNameRowKey
}

internal fun isGiftPreferenceProfileRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key.startsWith(giftPreferenceRowPrefixKey)
}

internal fun buildGiftPreferenceItems(rows: List<BaGuideRow>): List<GiftPreferenceItem> {
    if (rows.isEmpty()) return emptyList()
    return rows.mapIndexedNotNull { index, row ->
        val normalizedImages = buildList {
            add(row.imageUrl.trim())
            addAll(row.imageUrls.map { it.trim() })
        }.filter { candidate ->
            isRenderableGalleryImageUrl(candidate)
        }.distinct()
        val giftImage = normalizedImages.firstOrNull().orEmpty()
        if (giftImage.isBlank()) return@mapIndexedNotNull null
        val emojiImage = normalizedImages.firstOrNull { candidate ->
            candidate != giftImage
        }.orEmpty()
        val fallbackIndex = extractOrderedNumbers(row.key).firstOrNull() ?: (index + 1)
        val label = row.value
            .trim()
            .takeIf { it.isNotBlank() && !isProfileValuePlaceholder(it) }
            ?: "礼物$fallbackIndex"
        GiftPreferenceItem(
            label = label,
            giftImageUrl = giftImage,
            emojiImageUrl = emojiImage
        )
    }.distinctBy { item ->
        "${item.giftImageUrl}|${item.emojiImageUrl}|${item.label.trim()}"
    }
}

internal fun splitRoleRowTokens(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .split(Regex("""\s*(?:/|／|\||｜|\n)\s*"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

internal val sameNameGuideEmbeddedLinkRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
internal val sameNameGuidePathPattern = Regex("""^/(?:ba/tj/\d+(?:\.html)?|ba/\d+(?:\.html)?|v1/content/detail/\d+)$""")

internal fun sanitizeSameNameLinkToken(raw: String): String {
    return raw.trim().trimEnd(')', ']', '}', ',', '。', '，', ';', '；')
}

internal fun extractSameNameGuideLink(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""

    val directCandidates = buildList {
        val cleaned = sanitizeSameNameLinkToken(source)
        if (cleaned.startsWith("http://", ignoreCase = true) || cleaned.startsWith("https://", ignoreCase = true)) {
            add(cleaned)
        } else if (cleaned.startsWith("www.", ignoreCase = true)) {
            add("https://$cleaned")
        } else if (cleaned.matches(Regex("""^\d{4,}$"""))) {
            add("https://www.gamekee.com/ba/tj/$cleaned.html")
        } else if (cleaned.startsWith("/") && sameNameGuidePathPattern.matches(cleaned)) {
            add(normalizeGuideUrl(cleaned))
        }
        addAll(
            sameNameGuideEmbeddedLinkRegex.findAll(source).map { match ->
                sanitizeSameNameLinkToken(match.value)
            }
        )
    }.distinct()

    for (candidate in directCandidates) {
        val normalized = normalizeGuideUrl(candidate)
        if (normalized.isBlank()) continue
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: continue
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty()
        val hostAccepted = host == "www.gamekee.com" || host == "gamekee.com"
        val pathAccepted = sameNameGuidePathPattern.matches(path)
        if (!hostAccepted || !pathAccepted) continue
        val contentId = extractGuideContentIdFromUrl(normalized) ?: continue
        if (contentId <= 0L) continue
        return "https://www.gamekee.com/ba/tj/$contentId.html"
    }
    return ""
}

internal val sameNameRoleHintKeywords = listOf(
    "暂无同名角色",
    "未填写",
    "占位",
    "说明",
    "备注",
    "复制",
    "不用写",
    "暂时没",
    "待补充"
)

internal fun isSameNameRoleHintText(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    val compact = value
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
    if (compact.length >= 20) return true
    return sameNameRoleHintKeywords.any { keyword ->
        compact.contains(keyword.lowercase())
    }
}

internal fun extractSameNameRoleHint(row: BaGuideRow): String? {
    if (normalizeProfileFieldKey(row.key) != relatedSameNameRoleHeaderKey) return null
    val rawValue = row.value.trim().trim('*')
    if (rawValue.isBlank()) return null
    if (isProfileValuePlaceholder(rawValue)) return null
    val hasLink = extractProfileExternalLink(rawValue).isNotBlank()
    val hasImage = buildList {
        add(row.imageUrl.trim())
        addAll(row.imageUrls.map { it.trim() })
    }.any { candidate ->
        candidate.isNotBlank() && isRenderableGalleryImageUrl(candidate)
    }
    if (hasLink || hasImage) return null
    if (!isSameNameRoleHintText(rawValue)) return null
    return rawValue
}

internal fun buildSameNameRoleItems(rows: List<BaGuideRow>): List<SameNameRoleItem> {
    if (rows.isEmpty()) return emptyList()
    val items = rows.mapNotNull { row ->
        val normalizedKey = normalizeProfileFieldKey(row.key)
        if (normalizedKey != sameNameRoleNameRowKey && normalizedKey != relatedSameNameRoleHeaderKey) {
            return@mapNotNull null
        }
        val tokens = splitRoleRowTokens(row.value)
        val link = sequence<String> {
            tokens.forEach { yield(it) }
            yield(row.value)
        }.map { token -> extractSameNameGuideLink(token) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val name = tokens.firstOrNull { token ->
            token.isNotBlank() &&
                !isProfileValuePlaceholder(token) &&
                extractProfileExternalLink(token).isBlank() &&
                !isSameNameRoleHintText(token)
        }.orEmpty()
        val image = (row.imageUrls + row.imageUrl)
            .firstOrNull { candidate -> isRenderableGalleryImageUrl(candidate) }
            .orEmpty()
        if (name.isBlank() && link.isBlank() && image.isBlank()) {
            return@mapNotNull null
        }
        if (name.isBlank() && link.isBlank() && isSameNameRoleHintText(row.value)) {
            return@mapNotNull null
        }
        val fallbackName = when {
            link.isNotBlank() -> fallbackProfileLinkTitle(link)
            else -> "同名角色"
        }
        SameNameRoleItem(
            name = name.ifBlank { fallbackName },
            linkUrl = link,
            imageUrl = image
        )
    }

    return items.distinctBy { item ->
        "${item.name.trim()}|${item.linkUrl.trim()}|${item.imageUrl.trim()}"
    }
}

internal fun isProfileSectionHeaderRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key in profileSectionHeaderKeys
}

internal val galleryRelatedProfileLinkKeyTokens = listOf(
    "影画相关链接",
    "相关链接",
    "来源链接",
    "个人账号主页",
    "账号主页",
    "个人主页",
    "主页链接",
    "主页"
).map(::normalizeProfileFieldKey)

internal fun isGalleryRelatedProfileLinkRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (key.isBlank()) return false
    val hasGalleryLinkKey = galleryRelatedProfileLinkKeyTokens.any { token ->
        token.isNotBlank() && key.contains(token)
    }
    if (!hasGalleryLinkKey) return false
    val linkSource = buildString {
        append(row.value)
        append(' ')
        append(row.key)
    }
    return containsGuideWebLink(linkSource)
}

internal fun extractProfileExternalLink(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""

    val direct = when {
        source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true) -> source
        source.startsWith("www.", ignoreCase = true) -> "https://$source"
        source.startsWith("/") -> normalizeGuideUrl(source)
        else -> ""
    }
    if (direct.isNotBlank()) {
        val scheme = runCatching { Uri.parse(direct).scheme.orEmpty() }.getOrDefault("")
        if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
            return direct
        }
    }

    val embedded = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        .find(source)
        ?.value
        .orEmpty()
        .trimEnd(')', ']', '}', ',', '。', '，')
    if (embedded.isBlank()) return ""

    val embeddedScheme = runCatching { Uri.parse(embedded).scheme.orEmpty() }.getOrDefault("")
    return if (embeddedScheme.equals("http", ignoreCase = true) || embeddedScheme.equals("https", ignoreCase = true)) {
        embedded
    } else {
        ""
    }
}

internal fun resolveProfileLinkTitle(url: String): String {
    if (url.isBlank()) return ""
    profileLinkTitleCache[url]?.let { return it }
    val title = runCatching { fetchProfileLinkTitle(url) }.getOrDefault("")
    profileLinkTitleCache[url] = title
    return title
}

internal fun fetchProfileLinkTitle(url: String): String {
    if (url.isBlank()) return ""
    val html = GameKeeFetchHelper.fetchHtml(
        pathOrUrl = url,
        refererPath = "/ba/"
    )
    if (html.isBlank()) return ""

    val ogTitle = Regex(
        pattern = """<meta\s+[^>]*(?:property|name)\s*=\s*["']og:title["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(html)?.groupValues?.getOrNull(1).orEmpty()

    val titleTag = Regex(
        pattern = """<title[^>]*>(.*?)</title>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(html)?.groupValues?.getOrNull(1).orEmpty()

    return cleanProfileLinkTitle(
        if (ogTitle.isNotBlank()) ogTitle else titleTag
    )
}

internal fun cleanProfileLinkTitle(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun fallbackProfileLinkTitle(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull()
    val lastSegment = uri?.lastPathSegment?.trim().orEmpty()
    if (lastSegment.isNotBlank()) return lastSegment
    val host = uri?.host?.trim().orEmpty()
    if (host.isNotBlank()) return host
    return "打开链接"
}

internal fun isProfileValuePlaceholder(value: String): Boolean {
    val normalized = value.trim()
    val compact = normalized
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
    if (normalized.isBlank()) return true
    if (compact.matches(Regex("""^[\\/|｜／,，;；:：._\-—~·*]+$"""))) return true
    return normalized == "-" ||
        normalized == "—" ||
        normalized == "--" ||
        normalized == "暂无" ||
        normalized == "无" ||
        compact == "n" ||
        compact == "null" ||
        compact == "undefined" ||
        compact == "nan"
}

internal val profileInstructionNoteRegex = Regex("""(?:<-|←)?\s*(?:这个|这里|此处|这条)?\s*不用写""")

internal fun stripProfileInstructionNotes(raw: String): String {
    if (raw.isBlank()) return ""
    if (!profileInstructionNoteRegex.containsMatchIn(raw)) return raw.trim()

    val segments = raw
        .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
        .map { segment ->
            profileInstructionNoteRegex
                .replace(segment, "")
                .trim()
                .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
                .trim()
        }
        .filter { it.isNotBlank() }
    if (segments.isNotEmpty()) {
        return segments.joinToString(" / ").trim()
    }
    return profileInstructionNoteRegex
        .replace(raw, "")
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

internal fun isProfileInstructionPlaceholder(value: String): Boolean {
    if (value.isBlank()) return false
    val normalized = value.trim()
    if (!profileInstructionNoteRegex.containsMatchIn(normalized)) return false
    val stripped = stripProfileInstructionNotes(normalized)
    return isProfileValuePlaceholder(stripped)
}

internal fun stripProfileCopyHint(raw: String): String {
    if (raw.isBlank()) return ""
    val hintRegex = Regex("""(?:<-|←)?\s*大部分时候可以去别的图鉴复制""")
    if (!hintRegex.containsMatchIn(raw)) return raw.trim()

    val segments = raw
        .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
        .map { it.trim() }
        .filter { part ->
            part.isNotBlank() && !hintRegex.containsMatchIn(part)
        }
    if (segments.isNotEmpty()) {
        return segments.joinToString(" / ").trim()
    }
    return raw
        .replace(hintRegex, "")
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

internal val profileInlineNoteStripFieldKeys = setOf(
    "角色考据",
    "设计",
    "MomoTalk解锁等级",
    "Momotalk解锁等级"
).map(::normalizeProfileFieldKey).toSet()

internal fun sanitizeProfileFieldValue(key: String, value: String): String {
    if (value.isBlank()) return ""
    val normalizedKey = normalizeProfileFieldKey(key)
    var cleaned = value.trim()
    if (normalizedKey == normalizeProfileFieldKey("声优")) {
        cleaned = stripProfileCopyHint(cleaned)
    }
    if (normalizedKey in profileInlineNoteStripFieldKeys) {
        cleaned = stripGuideInlineNotes(cleaned)
    }
    cleaned = stripProfileInstructionNotes(cleaned)
    cleaned = cleaned
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
    return cleaned
}

internal fun isProfileRowAliasMatch(row: BaGuideRow, aliases: List<String>): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (key.isBlank()) return false
    return aliases.any { alias ->
        key == normalizeProfileFieldKey(alias)
    }
}

internal fun buildProfileCardRows(rows: List<BaGuideRow>, specs: List<ProfileFieldSpec>): List<BaGuideRow> {
    return buildList {
        specs.forEach { spec ->
            val matched = rows.firstOrNull { row ->
                isProfileRowAliasMatch(row, spec.aliases)
            } ?: return@forEach
            val normalizedValue = sanitizeProfileFieldValue(spec.title, matched.value)
            if (isProfileInstructionPlaceholder(matched.value) && isProfileValuePlaceholder(normalizedValue)) {
                return@forEach
            }
            if (spec.hideWhenEmpty && isProfileValuePlaceholder(normalizedValue)) {
                return@forEach
            }
            add(matched.copy(key = spec.title, value = normalizedValue))
        }
    }
}

internal fun isStructuredProfileCardRow(row: BaGuideRow): Boolean {
    return profileStructuredFieldSpecs.any { spec ->
        isProfileRowAliasMatch(row, spec.aliases)
    }
}

internal val topDataStatKeys = setOf(
    "攻击力", "防御力", "生命值", "治愈力",
    "命中值", "闪避值", "暴击值", "暴击伤害",
    "稳定值", "射程", "群控强化力", "群控抵抗力",
    "装弹数", "防御无视值", "受恢复率", "COST恢复力"
)
internal val normalizedTopDataStatKeys = topDataStatKeys.map(::normalizeProfileFieldKey).toSet()

internal fun splitGuideCompositeValues(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .replace("／", "/")
        .replace("|", "/")
        .replace("｜", "/")
        .split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" && it != "—" }
}

internal fun isSkillMigratedProfileRow(
    row: BaGuideRow,
    hasTopDataHeader: Boolean,
    hasInitialDataHeader: Boolean
): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    val value = normalizeProfileFieldKey(row.value)
    if (Regex("""^附加属性\d+$""").matches(key)) return true
    if (key == normalizeProfileFieldKey("初始数据")) return true
    if (key == normalizeProfileFieldKey("顶级数据")) return true
    if (key == normalizeProfileFieldKey("25级")) return true
    if (Regex("""^t\d+$""", RegexOption.IGNORE_CASE).matches(key)) return true
    if (Regex("""^t\d+(效果|所需升级材料|技能图标)$""", RegexOption.IGNORE_CASE).matches(key)) return true
    if (
        isLikelySimulateStatLabel(row.key) &&
        isLikelySimulateStatLabel(row.value) &&
        !Regex("""\d""").containsMatchIn(value)
    ) {
        return true
    }
    if ((hasTopDataHeader || hasInitialDataHeader) && key in normalizedTopDataStatKeys) return true
    return false
}
