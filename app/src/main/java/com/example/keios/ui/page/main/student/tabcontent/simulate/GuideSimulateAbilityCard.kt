package com.example.keios.ui.page.main.student.tabcontent.simulate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.buildGuideTabCopyPayload
import com.example.keios.ui.page.main.student.guideTabCopyable
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideSimulateAbilityCard(
    data: GuideSimulateData,
    backdrop: LayerBackdrop
) {
    var selectedAbility by rememberSaveable(
        data.initialRows.size,
        data.maxRows.size
    ) { mutableStateOf("最大培养") }
    val selectedRows = if (selectedAbility == "最大培养") data.maxRows else data.initialRows
    val selectedHint = if (selectedAbility == "最大培养") data.maxHint else data.initialHint
    val initialValueByKey = remember(data.initialRows) {
        linkedMapOf<String, String>().apply {
            data.initialRows.forEach { row ->
                val key = normalizeProfileFieldKey(row.key)
                if (key.isBlank() || row.value.isBlank()) return@forEach
                putIfAbsent(key, row.value.trim())
            }
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "角色能力",
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
                listOf("初始能力", "最大培养").forEach { option ->
                    val selected = selectedAbility == option
                    GlassTextButton(
                        backdrop = backdrop,
                        text = option,
                        textColor = if (selected) Color(0xFF2563EB) else MiuixTheme.colorScheme.onBackgroundVariant,
                        containerColor = if (selected) Color(0x443B82F6) else null,
                        variant = GlassVariant.Compact,
                        onClick = { selectedAbility = option }
                    )
                }
            }
            selectedHint.takeIf { it.isNotBlank() }?.let { hint ->
                Text(
                    text = hint.trim('*').trim(),
                    color = Color(0xFF60A5FA),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.guideTabCopyable(
                        buildGuideTabCopyPayload("角色能力说明", hint)
                    )
                )
            }
            if (selectedRows.isNotEmpty()) {
                selectedRows.forEach { row ->
                    val deltaText = if (selectedAbility == "最大培养") {
                        buildSimulateMaxDeltaText(
                            maxValue = row.value,
                            initialValue = initialValueByKey[normalizeProfileFieldKey(row.key)]
                        )
                    } else {
                        ""
                    }
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop,
                        valueDelta = deltaText
                    )
                }
            } else {
                Text(
                    text = "暂无能力数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}
