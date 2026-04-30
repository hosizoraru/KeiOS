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
import os.kei.ui.page.main.student.tabcontent.simulate.GuideSimulateAbilityCard
import os.kei.ui.page.main.student.tabcontent.simulate.GuideSimulateBondCard
import os.kei.ui.page.main.student.tabcontent.simulate.GuideSimulateEquipmentCard
import os.kei.ui.page.main.student.tabcontent.simulate.GuideSimulateSectionCard
import os.kei.ui.page.main.student.tabcontent.simulate.GuideSimulateUnlockCard
import os.kei.ui.page.main.student.tabcontent.simulate.GuideSimulateWeaponCard
import os.kei.ui.page.main.student.tabcontent.simulate.buildGuideSimulateData
import os.kei.ui.page.main.student.simulateRowsForDisplay
import os.kei.ui.page.main.widget.glass.FrostedBlock
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderGuideSimulateTabContent(
    tabLabel: String,
    info: BaStudentGuideInfo?,
    error: String?,
    backdrop: LayerBackdrop,
    accent: Color
) {
    val guide = info
    if (guide == null) {
        item {
            FrostedBlock(
                backdrop = backdrop,
                title = tabLabel,
                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                accent = accent
            )
        }
        return
    }

    val simulateRows = guide.simulateRowsForDisplay()
    val simulateData = buildGuideSimulateData(simulateRows)

    if (!error.isNullOrBlank()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = Color(0x223B82F6),
                    contentColor = MiuixTheme.colorScheme.onBackground
                ),
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

    val hasAnySectionData = simulateData.initialRows.isNotEmpty() ||
        simulateData.maxRows.isNotEmpty() ||
        simulateData.weaponRows.isNotEmpty() ||
        simulateData.equipmentRows.isNotEmpty() ||
        simulateData.favorRows.isNotEmpty() ||
        simulateData.unlockRows.isNotEmpty() ||
        simulateData.bondRows.isNotEmpty()

    if (hasAnySectionData) {
        item {
            GuideSimulateAbilityCard(
                data = simulateData,
                backdrop = backdrop
            )
        }

        val sectionCards = listOf(
            Triple("专武", simulateData.weaponRows, simulateData.weaponHint),
            Triple("装备", simulateData.equipmentRows, simulateData.equipmentHint),
            Triple("爱用品", simulateData.favorRows, simulateData.favorHint),
            Triple("能力解放", simulateData.unlockRows, simulateData.unlockHint),
            Triple("羁绊等级奖励", simulateData.bondRows, simulateData.bondHint)
        )

        sectionCards.forEach { (title, rows, hint) ->
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                when (title) {
                    "专武" -> GuideSimulateWeaponCard(
                        title = title,
                        rows = rows,
                        hint = hint,
                        backdrop = backdrop
                    )

                    "装备" -> GuideSimulateEquipmentCard(
                        title = title,
                        rows = rows,
                        hint = hint,
                        backdrop = backdrop
                    )

                    "能力解放" -> GuideSimulateUnlockCard(
                        title = title,
                        rows = rows,
                        hint = hint,
                        backdrop = backdrop
                    )

                    "羁绊等级奖励" -> GuideSimulateBondCard(
                        title = title,
                        rows = rows,
                        hint = hint,
                        backdrop = backdrop
                    )

                    else -> GuideSimulateSectionCard(
                        title = title,
                        rows = rows,
                        hint = hint,
                        backdrop = backdrop
                    )
                }
            }
        }
    } else {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = Color(0x223B82F6),
                    contentColor = MiuixTheme.colorScheme.onBackground
                ),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.guide_simulate_empty),
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            }
        }
    }
}
