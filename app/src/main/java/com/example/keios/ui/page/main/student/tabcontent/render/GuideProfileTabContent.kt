package com.example.keios.ui.page.main.student.tabcontent.render

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
import com.example.keios.ui.page.main.student.tabcontent.profile.GuideGiftPreferenceGrid
import com.example.keios.ui.page.main.student.tabcontent.profile.GuideProfileInfoItem
import com.example.keios.ui.page.main.student.tabcontent.profile.GuideProfileInfoRows
import com.example.keios.ui.page.main.student.tabcontent.profile.GuideProfileRowsSection
import com.example.keios.ui.page.main.student.tabcontent.profile.GuideProfileSectionHeader
import com.example.keios.ui.page.main.student.tabcontent.profile.extractProfileExternalLink
import com.example.keios.ui.page.main.student.tabcontent.profile.fallbackProfileLinkTitle
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey
import com.example.keios.ui.page.main.student.tabcontent.profile.profileLinkTitleCache
import com.example.keios.ui.page.main.student.tabcontent.profile.profileRoleReferenceFieldKey
import com.example.keios.ui.page.main.student.tabcontent.profile.resolveProfileLinkTitle
import com.example.keios.ui.page.main.widget.glass.FrostedBlock
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderGuideProfileTabContent(
    tabLabel: String,
    info: BaStudentGuideInfo?,
    error: String?,
    backdrop: LayerBackdrop,
    accent: Color,
    context: Context,
    sourceUrl: String,
    galleryCacheRevision: Int,
    onOpenExternal: (String) -> Unit,
    onOpenGuide: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit
) {
    val guide = info
    if (guide == null) {
        item {
            FrostedBlock(
                backdrop = backdrop,
                title = tabLabel,
                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                accent = accent,
                content = {
                    error?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MiuixTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
        return
    }

    val headerState = buildGuideProfileTabHeaderState(guide)

    if (!error.isNullOrBlank()) {
        guideProfileCard {
            Text(
                text = error.orEmpty(),
                color = MiuixTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (headerState.nicknameRows.isNotEmpty()) {
        guideProfileCard {
            GuideProfileSectionHeader(title = "学生昵称")
            GuideProfileInfoRows(rows = headerState.nicknameRows) { row ->
                GuideProfileInfoItem(
                    key = row.key.ifBlank { "信息" },
                    value = row.value.ifBlank { "-" }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (headerState.studentInfoRows.isNotEmpty()) {
        guideProfileCard {
            GuideProfileSectionHeader(title = "学生信息")
            GuideProfileInfoRows(rows = headerState.studentInfoRows) { row ->
                val normalizedKey = normalizeProfileFieldKey(row.key)
                if (normalizedKey == profileRoleReferenceFieldKey) {
                    val externalLink = remember(row.value) {
                        extractProfileExternalLink(row.value)
                    }
                    val resolvedTitle by produceState(
                        initialValue = if (externalLink.isNotBlank()) {
                            profileLinkTitleCache[externalLink].orEmpty()
                        } else {
                            ""
                        },
                        key1 = externalLink
                    ) {
                        value = if (externalLink.isBlank()) {
                            ""
                        } else {
                            withContext(Dispatchers.IO) {
                                resolveProfileLinkTitle(externalLink)
                            }
                        }
                    }
                    val displayValue = when {
                        externalLink.isBlank() -> row.value.ifBlank { "-" }
                        resolvedTitle.isNotBlank() -> resolvedTitle
                        else -> fallbackProfileLinkTitle(externalLink)
                    }
                    GuideProfileInfoItem(
                        key = row.key.ifBlank { "信息" },
                        value = displayValue,
                        onClick = externalLink.takeIf { it.isNotBlank() }?.let { link ->
                            { onOpenExternal(link) }
                        },
                        valueColor = if (externalLink.isNotBlank()) {
                            Color(0xFF5FA8FF)
                        } else {
                            null
                        },
                        preferCapsule = false
                    )
                } else {
                    GuideProfileInfoItem(
                        key = row.key.ifBlank { "信息" },
                        value = row.value.ifBlank { "-" }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (headerState.hobbyRows.isNotEmpty()) {
        guideProfileCard {
            GuideProfileSectionHeader(title = "学生爱好")
            GuideProfileInfoRows(rows = headerState.hobbyRows) { row ->
                GuideProfileInfoItem(
                    key = row.key.ifBlank { "信息" },
                    value = row.value.ifBlank { "-" },
                    preferCapsule = false
                )
            }
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (headerState.giftPreferenceItems.isNotEmpty()) {
        guideProfileCard {
            GuideProfileSectionHeader(title = "礼物偏好")
            GuideGiftPreferenceGrid(items = headerState.giftPreferenceItems)
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    guideProfileCard {
        GuideSameNameRoleSection(
            sameNameRoleHint = headerState.sameNameRoleHint,
            sameNameRoleItems = headerState.sameNameRoleItems,
            backdrop = backdrop,
            onOpenGuide = onOpenGuide
        )
    }
    item { Spacer(modifier = Modifier.height(10.dp)) }

    if (headerState.normalProfileRows.isNotEmpty()) {
        guideProfileCard {
            GuideProfileRowsSection(
                rows = headerState.normalProfileRows,
                emptyText = "暂未解析到学生档案数据。"
            )
        }
    } else if (
        headerState.nicknameRows.isEmpty() &&
        headerState.studentInfoRows.isEmpty() &&
        headerState.hobbyRows.isEmpty() &&
        headerState.giftPreferenceItems.isEmpty()
    ) {
        guideProfileCard {
            Text(
                text = "暂未解析到学生档案数据。",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        }
    }

    renderGuideProfileMediaGroup(
        title = "巧克力",
        infoRows = headerState.chocolateInfoRows,
        galleryItems = headerState.chocolateGalleryItems,
        backdrop = backdrop,
        context = context,
        sourceUrl = sourceUrl,
        galleryCacheRevision = galleryCacheRevision,
        onOpenExternal = onOpenExternal,
        onSaveMedia = onSaveMedia,
        preferCapsule = true
    )

    renderGuideProfileMediaGroup(
        title = "互动家具",
        infoRows = headerState.furnitureInfoRows,
        galleryItems = headerState.furnitureGalleryItems,
        backdrop = backdrop,
        context = context,
        sourceUrl = sourceUrl,
        galleryCacheRevision = galleryCacheRevision,
        onOpenExternal = onOpenExternal,
        onSaveMedia = onSaveMedia,
        preferCapsule = false
    )
}
