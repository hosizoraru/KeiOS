package os.kei.ui.page.main.student.tabcontent.simulate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.GuideRemoteImage
import os.kei.ui.page.main.student.buildGuideTabCopyPayload
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.student.guideTabCopyable
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideSimulateBondCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val groups = buildSimulateBondGroups(rows)
    val levelCapsule = extractSimulateLevelCapsule(hint)
    val bondCharacterLabel = stringResource(R.string.guide_simulate_bond_character)

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = levelCapsule,
                backdrop = backdrop
            )

            if (groups.isNotEmpty()) {
                groups.forEach { group ->
                    val displayRoleLabel = guideLocalizedLabel(
                        group.roleLabel,
                        R.string.guide_simulate_bond_character
                    )
                    val groupCopyPayload = buildGuideTabCopyPayload(
                        bondCharacterLabel,
                        displayRoleLabel.ifBlank { bondCharacterLabel }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideTabCopyable(groupCopyPayload),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (group.iconUrl.isNotBlank()) {
                            Box(modifier = Modifier.width(112.dp)) {
                                GuideRemoteImage(
                                    imageUrl = group.iconUrl,
                                    imageHeight = 86.dp
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayRoleLabel,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            group.statRows.forEach { stat ->
                                GuideSimulateRowItem(
                                    row = stat,
                                    backdrop = backdrop
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.guide_simulate_empty_bond),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

@Composable
internal fun GuideSimulateWeaponCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val viewData = buildSimulateWeaponViewData(rows)
    val levelCapsule = extractSimulateLevelCapsule(hint)

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = levelCapsule,
                backdrop = backdrop
            )

            if (viewData.imageUrl.isNotBlank() || viewData.statRows.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (viewData.imageUrl.isNotBlank()) {
                        Box(modifier = Modifier.width(112.dp)) {
                            GuideRemoteImage(
                                imageUrl = viewData.imageUrl,
                                imageHeight = 72.dp
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewData.statRows.forEach { row ->
                            GuideSimulateRowItem(
                                row = row,
                                backdrop = backdrop
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.guide_simulate_empty_unique_weapon),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}
