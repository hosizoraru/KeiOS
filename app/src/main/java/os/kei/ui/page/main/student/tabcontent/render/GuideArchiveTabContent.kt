package os.kei.ui.page.main.student.tabcontent.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.GuideRemoteImage
import os.kei.ui.page.main.student.buildCombatMetaItems
import os.kei.ui.page.main.student.buildProfileMetaItems
import os.kei.ui.page.main.student.isNpcSatelliteGuideSource
import os.kei.ui.page.main.student.section.GuideCombatMetaTile
import os.kei.ui.page.main.student.section.GuideProfileMetaLine
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderGuideArchiveTabContent(
    info: BaStudentGuideInfo?
) {
    item {
        val guide = info
        val profileItems = remember(
            guide?.sourceUrl,
            guide?.syncedAtMs
        ) {
            guide?.buildProfileMetaItems().orEmpty()
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = Color(0x223B82F6),
                contentColor = MiuixTheme.colorScheme.onBackground
            ),
            onClick = {}
        ) {
            if (guide != null) {
                val useNpcPortraitTopCrop = remember(guide.sourceUrl, guide.syncedAtMs) {
                    isNpcSatelliteGuideSource(guide.sourceUrl)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.width(112.dp)) {
                            if (guide.imageUrl.isNotBlank()) {
                                GuideRemoteImage(
                                    imageUrl = guide.imageUrl,
                                    imageHeight = 152.dp,
                                    cropAlignment = if (useNpcPortraitTopCrop) {
                                        Alignment.TopCenter
                                    } else {
                                        Alignment.Center
                                    }
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.guide_gallery_no_image),
                                    color = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                            }
                        }
                        val rarityItem = profileItems.firstOrNull { meta ->
                            meta.title == "稀有度" || meta.title == "星级"
                        }
                        val academyItem = profileItems.firstOrNull { meta ->
                            meta.title == "学院"
                        }
                        val clubItem = profileItems.firstOrNull { meta ->
                            meta.title == "所属社团" || meta.title == "社团"
                        }
                        val alignedItems = listOfNotNull(rarityItem, academyItem, clubItem)
                        val extraItems = profileItems.filterNot { item ->
                            alignedItems.any { it.title == item.title && it.value == item.value }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                        ) {
                            rarityItem?.let { item ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth()
                                ) {
                                    GuideProfileMetaLine(item)
                                }
                            }
                            academyItem?.let { item ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth()
                                ) {
                                    GuideProfileMetaLine(item)
                                }
                            }
                            clubItem?.let { item ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                ) {
                                    GuideProfileMetaLine(item)
                                }
                            }
                            if (alignedItems.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    profileItems.forEach { item ->
                                        GuideProfileMetaLine(item)
                                    }
                                }
                            } else if (extraItems.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    extraItems.forEach { item ->
                                        GuideProfileMetaLine(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    item { Spacer(modifier = Modifier.height(10.dp)) }
    item {
        val guide = info
        val combatItems = remember(
            guide?.sourceUrl,
            guide?.syncedAtMs
        ) {
            guide?.buildCombatMetaItems().orEmpty()
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = Color(0x223B82F6),
                contentColor = MiuixTheme.colorScheme.onBackground
            ),
            onClick = {}
        ) {
            if (guide != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    combatItems.forEach { item ->
                        GuideCombatMetaTile(
                            item = item,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
