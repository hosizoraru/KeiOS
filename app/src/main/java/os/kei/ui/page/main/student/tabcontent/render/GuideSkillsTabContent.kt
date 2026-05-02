package os.kei.ui.page.main.student.tabcontent.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.section.GuideSkillCardItem
import os.kei.ui.page.main.student.section.GuideWeaponCardItem
import os.kei.ui.page.main.student.skillCardsForDisplay
import os.kei.ui.page.main.student.weaponCardForDisplay
import os.kei.ui.page.main.widget.glass.LiquidInfoBlock
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderGuideSkillsTabContent(
    tabLabel: String,
    info: BaStudentGuideInfo?,
    error: String?,
    backdrop: LayerBackdrop,
    accent: Color
) {
    val guide = info
    if (guide == null) {
        item {
            LiquidInfoBlock(
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

    val skillCards = guide.skillCardsForDisplay()
    val weaponCard = guide.weaponCardForDisplay()

    if (!error.isNullOrBlank()) {
        item {
            GuideLiquidCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = Color(0x223B82F6),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = error.orEmpty(),
                        color = MiuixTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (skillCards.isNotEmpty()) {
        skillCards.forEachIndexed { index, card ->
            item {
                GuideSkillCardItem(
                    card = card,
                    backdrop = backdrop
                )
            }
            if (index < skillCards.lastIndex) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }
        }
    } else {
        item {
            GuideLiquidCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = Color(0x223B82F6),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.guide_skill_cards_empty),
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            }
        }
    }

    weaponCard?.let { weapon ->
        item { Spacer(modifier = Modifier.height(10.dp)) }
        item {
            GuideWeaponCardItem(
                card = weapon,
                backdrop = backdrop
            )
        }
    }
}
