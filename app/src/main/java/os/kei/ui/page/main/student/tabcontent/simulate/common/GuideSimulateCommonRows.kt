package os.kei.ui.page.main.student.tabcontent.simulate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.GuideRemoteIcon
import os.kei.ui.page.main.student.buildGuideTabCopyPayload
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.student.guideLocalizedValue
import os.kei.ui.page.main.student.guideTabCopyable
import os.kei.ui.page.main.student.rememberGuideTabCopyAction
import os.kei.ui.page.main.student.tabcontent.simulate.isSimulateSubHeader
import os.kei.ui.page.main.student.tabcontent.simulate.simulateStatGlyphForKey
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import os.kei.ui.page.main.widget.support.copyModeAwareRow
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideSimulateCardTitleRow(
    title: String,
    capsule: String,
    backdrop: LayerBackdrop
) {
    val displayTitle = guideLocalizedLabel(title)
    val copyPayload = remember(displayTitle, capsule) {
        buildGuideTabCopyPayload(displayTitle, capsule.ifBlank { "-" })
    }
    CopyModeSelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .guideTabCopyable(copyPayload),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTitle,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            capsule.takeIf { it.isNotBlank() }?.let { label ->
                GuideSimulateInlineCapsule(
                    text = label,
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
internal fun GuideSimulateInlineCapsule(
    text: String,
    backdrop: LayerBackdrop
) {
    val displayText = guideLocalizedLabel(text)
    GlassTextButton(
        backdrop = backdrop,
        text = displayText,
        enabled = false,
        textColor = Color(0xFF60A5FA),
        variant = GlassVariant.Compact,
        onClick = {}
    )
}

@Composable
internal fun GuideSimulateRowItem(
    row: BaGuideRow,
    backdrop: LayerBackdrop,
    valueDelta: String = ""
) {
    val key = row.key.trim()
    val displayKey = guideLocalizedLabel(key)
    val value = guideLocalizedValue(row.value.trim())
    val rowCopyAction =
        rememberGuideTabCopyAction(buildGuideTabCopyPayload(displayKey, value.ifBlank { "-" }))
    val iconUrl = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
    val statGlyph = simulateStatGlyphForKey(key)
    if (isSimulateSubHeader(key)) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier.copyModeAwareRow(
                    copyPayload = buildGuideTabCopyPayload(displayKey, value.ifBlank { "-" }),
                    onLongClick = rowCopyAction
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 24.dp,
                        iconHeight = 24.dp
                    )
                }
                GlassTextButton(
                    backdrop = backdrop,
                    text = displayKey,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
            }
        }
        return
    }

    val valueColor = when {
        value.contains("%") -> Color(0xFF5FA8FF)
        value.matches(Regex("""(?i)^T\d+.*$""")) -> Color(0xFF5FA8FF)
        value.matches(Regex("""(?i)^Lv\d+.*$""")) -> Color(0xFF5FA8FF)
        key.contains("COST", ignoreCase = true) -> Color(0xFF5FA8FF)
        else -> MiuixTheme.colorScheme.onBackground
    }

    CopyModeSelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .copyModeAwareRow(
                    copyPayload = buildGuideTabCopyPayload(displayKey, value.ifBlank { "-" }),
                    onLongClick = rowCopyAction
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(0.45f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 24.dp,
                        iconHeight = 24.dp
                    )
                } else if (statGlyph != null) {
                    Text(
                        text = statGlyph,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = displayKey,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.weight(0.55f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { "-" },
                    color = valueColor,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                valueDelta.takeIf { it.isNotBlank() }?.let { delta ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = delta,
                        color = Color(0xFFE3B547),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
