package com.example.keios.ui.page.main.student.tabcontent.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.GuideRemoteIcon
import com.example.keios.ui.page.main.student.buildGuideTabCopyPayload
import com.example.keios.ui.page.main.student.guideTabCopyable
import com.example.keios.ui.page.main.student.tabcontent.profile.GuideProfileSectionHeader
import com.example.keios.ui.page.main.student.tabcontent.profile.SameNameRoleItem
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.support.CopyModeSelectionContainer
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideSameNameRoleSection(
    sameNameRoleHint: String,
    sameNameRoleItems: List<SameNameRoleItem>,
    backdrop: LayerBackdrop,
    onOpenGuide: (String) -> Unit
) {
    GuideProfileSectionHeader(title = "相关同名角色")
    sameNameRoleHint.takeIf { it.isNotBlank() }?.let { hint ->
        CopyModeSelectionContainer {
            Text(
                text = hint,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.guideTabCopyable(
                    buildGuideTabCopyPayload("相关同名角色", hint)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (sameNameRoleItems.isEmpty()) {
        Text(
            text = "暂无同名角色条目。",
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }

    sameNameRoleItems.forEachIndexed { index, role ->
        if (index > 0) {
            Spacer(modifier = Modifier.height(4.dp))
        }
        val roleCopyPayload = buildString {
            append(role.name.ifBlank { "同名角色" })
            role.linkUrl.trim().takeIf { it.isNotBlank() }?.let { link ->
                append('\n')
                append(link)
            }
        }
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideTabCopyable(
                        buildGuideTabCopyPayload(
                            "同名角色",
                            roleCopyPayload
                        )
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewImage = role.imageUrl.trim()
                if (previewImage.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = previewImage,
                        iconWidth = 68.dp,
                        iconHeight = 68.dp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val link = role.linkUrl.trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = role.name.ifBlank { "同名角色" },
                            color = MiuixTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip
                        )
                        if (link.isNotBlank()) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "图鉴",
                                textColor = Color(0xFF3B82F6),
                                variant = GlassVariant.Compact,
                                onClick = { onOpenGuide(link) }
                            )
                        }
                    }
                    if (link.isBlank()) {
                        Text(
                            text = "暂无可跳转链接",
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
