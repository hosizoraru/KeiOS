package os.kei.ui.page.main.about.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideAlertIcon
import os.kei.ui.page.main.os.appLucideAppWindowIcon
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.os.appLucideFilterIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.osLucideSettingsIcon
import os.kei.ui.page.main.about.model.AboutComponentEntry
import os.kei.ui.page.main.about.model.AboutComponentType
import os.kei.ui.page.main.about.model.AboutPermissionEntry
import os.kei.ui.page.main.about.ui.AboutCompactInfoRow
import os.kei.ui.page.main.about.ui.AboutCompactPillRow
import os.kei.ui.page.main.about.ui.AboutSectionCard
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import java.util.Locale

@Composable
fun AboutRuntimeStatusCardSection(
    cardColor: Color,
    accent: Color,
    shizukuReady: Boolean,
    readyColor: Color,
    notReadyColor: Color,
    subtitleColor: Color,
    notificationPermissionGranted: Boolean,
    shizukuDetailMap: Map<String, String>,
    permissionCount: Int,
    componentCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCheckShizuku: () -> Unit
) {
    val titleColor = if (shizukuReady) readyColor else notReadyColor
    val selinuxRaw = shizukuDetailMap["Shizuku getenforce"] ?: stringResource(R.string.common_na)
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_runtime_title),
        subtitle = stringResource(R.string.about_card_runtime_subtitle),
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideInfoIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
        ) {
            AboutCompactPillRow(
                title = stringResource(R.string.about_runtime_label_notification_permission),
                label = if (notificationPermissionGranted) {
                    stringResource(R.string.common_status_authorized)
                } else {
                    stringResource(R.string.common_status_unauthorized)
                },
                titleIcon = appLucideAlertIcon(),
                color = if (notificationPermissionGranted) readyColor else notReadyColor
            )
            AboutCompactPillRow(
                title = stringResource(R.string.about_runtime_label_selinux),
                label = selinuxLabelText(selinuxRaw),
                titleIcon = appLucideLockIcon(),
                color = selinuxStatusColor(selinuxRaw),
                onClick = onCheckShizuku
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_runtime_label_uname),
                value = shizukuDetailMap["Shizuku uname"] ?: stringResource(R.string.common_na),
                titleIcon = appLucideNotesIcon(),
                valueColor = accent,
                onClick = onCheckShizuku
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_runtime_label_permission_count),
                value = permissionCount.toString(),
                titleIcon = appLucideListIcon()
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_runtime_label_component_count),
                value = componentCount.toString(),
                titleIcon = appLucideLayersIcon()
            )
        }
    }
}

@Composable
fun AboutPermissionCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    readyColor: Color,
    notReadyColor: Color,
    entries: List<AboutPermissionEntry>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_permission_title),
        subtitle = stringResource(R.string.about_card_permission_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideLockIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        if (entries.isEmpty()) {
            AboutCompactInfoRow(
                title = stringResource(R.string.about_label_status),
                value = stringResource(R.string.about_permission_empty)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)) {
                entries.forEachIndexed { index, entry ->
                    AboutPermissionEntryView(
                        entry = entry,
                        accent = accent,
                        grantedColor = readyColor,
                        deniedColor = notReadyColor
                    )
                    if (index < entries.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AboutComponentCardSection(
    cardColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    accent: Color,
    entries: List<AboutComponentEntry>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_component_title),
        subtitle = stringResource(R.string.about_card_component_subtitle),
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideListIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        if (entries.isEmpty()) {
            AboutCompactInfoRow(
                title = stringResource(R.string.about_label_status),
                value = stringResource(R.string.about_component_empty)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)) {
                entries.forEachIndexed { index, entry ->
                    AboutComponentEntryView(
                        entry = entry,
                        accent = accent,
                        exportedColor = Color(0xFFB26A00),
                        internalColor = titleColor
                    )
                    if (index < entries.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutPermissionEntryView(
    entry: AboutPermissionEntry,
    accent: Color,
    grantedColor: Color,
    deniedColor: Color
) {
    val statusColor = if (entry.granted) grantedColor else deniedColor
    val statusLabel = if (entry.granted) {
        stringResource(R.string.common_status_authorized)
    } else {
        stringResource(R.string.common_status_unauthorized)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricCardTextGap)
    ) {
        AboutCompactInfoRow(
            title = stringResource(R.string.about_permission_label_permission),
            value = entry.title,
            titleIcon = appLucideLockIcon(),
            valueColor = accent
        )
        AboutCompactPillRow(
            title = stringResource(R.string.about_permission_label_granted),
            label = statusLabel,
            titleIcon = if (entry.granted) appLucideConfirmIcon() else appLucideCloseIcon(),
            color = statusColor
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_permission_label_system_name),
            value = entry.name,
            titleIcon = appLucideNotesIcon()
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_permission_label_purpose),
            value = entry.purpose,
            titleIcon = appLucideConfigIcon()
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_permission_label_used_in),
            value = entry.usedIn,
            titleIcon = appLucideLayersIcon()
        )
    }
}

@Composable
private fun AboutComponentEntryView(
    entry: AboutComponentEntry,
    accent: Color,
    exportedColor: Color,
    internalColor: Color
) {
    val exportColor = if (entry.exported) exportedColor else internalColor
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricCardTextGap)
    ) {
        AboutCompactInfoRow(
            title = stringResource(entry.type.titleRes),
            value = entry.name,
            titleIcon = componentTypeIcon(entry.type),
            valueColor = accent
        )
        AboutCompactPillRow(
            title = stringResource(R.string.about_component_label_export_state),
            label = if (entry.exported) {
                stringResource(R.string.about_component_state_exported)
            } else {
                stringResource(R.string.about_component_state_internal)
            },
            titleIcon = if (entry.exported) appLucideAlertIcon() else appLucideLockIcon(),
            color = exportColor
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_permission_label_purpose),
            value = entry.purpose,
            titleIcon = appLucideConfigIcon()
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_permission_label_used_in),
            value = entry.usedIn,
            titleIcon = appLucideLayersIcon()
        )
        entry.extra.forEach { extra ->
            AboutCompactInfoRow(
                title = stringResource(extra.labelRes),
                value = extra.value,
                titleIcon = componentExtraIcon(extra.labelRes)
            )
        }
    }
}

@Composable
private fun componentTypeIcon(type: AboutComponentType): ImageVector {
    return when (type) {
        AboutComponentType.Service -> osLucideSettingsIcon()
        AboutComponentType.Receiver -> appLucideRefreshIcon()
        AboutComponentType.Provider -> appLucideAppWindowIcon()
    }
}

@Composable
private fun componentExtraIcon(labelRes: Int): ImageVector {
    return when (labelRes) {
        R.string.about_component_label_class -> appLucideNotesIcon()
        R.string.about_component_label_fgs_type -> appLucideFilterIcon()
        R.string.about_component_label_authority -> appLucideInfoIcon()
        else -> appLucideInfoIcon()
    }
}

private fun normalizeLower(value: String): String = value.trim().lowercase(Locale.ROOT)

@Composable
private fun selinuxLabelText(selinuxState: String): String {
    return when (normalizeLower(selinuxState)) {
        "enforcing" -> stringResource(R.string.common_status_selinux_enforcing)
        "permissive" -> stringResource(R.string.common_status_selinux_permissive)
        "disabled" -> stringResource(R.string.common_status_selinux_disabled)
        "n/a", "", "unknown" -> stringResource(R.string.common_unknown)
        else -> selinuxState.ifBlank { stringResource(R.string.common_unknown) }
    }
}

private fun selinuxStatusColor(selinuxState: String): Color {
    return when (normalizeLower(selinuxState)) {
        "enforcing" -> Color(0xFF2E7D32)
        "permissive" -> Color(0xFFB26A00)
        "disabled" -> Color(0xFFC62828)
        "n/a", "", "unknown" -> Color(0xFF6B7280)
        else -> Color(0xFF2563EB)
    }
}
