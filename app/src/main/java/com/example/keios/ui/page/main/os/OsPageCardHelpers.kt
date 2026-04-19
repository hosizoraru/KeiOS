package com.example.keios.ui.page.main

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.keios.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun visibleSectionKinds(visibleCards: Set<OsSectionCard>): Set<SectionKind> = buildSet {
    if (visibleCards.contains(OsSectionCard.SYSTEM)) add(SectionKind.SYSTEM)
    if (visibleCards.contains(OsSectionCard.SECURE)) add(SectionKind.SECURE)
    if (visibleCards.contains(OsSectionCard.GLOBAL)) add(SectionKind.GLOBAL)
    if (visibleCards.contains(OsSectionCard.ANDROID)) add(SectionKind.ANDROID)
    if (visibleCards.contains(OsSectionCard.JAVA)) add(SectionKind.JAVA)
    if (visibleCards.contains(OsSectionCard.LINUX)) add(SectionKind.LINUX)
}

internal fun isCardVisible(visibleCards: Set<OsSectionCard>, card: OsSectionCard): Boolean {
    return visibleCards.contains(card)
}

internal fun sectionSubtitle(
    sectionStates: Map<SectionKind, SectionState>,
    context: Context,
    section: SectionKind,
    size: Int
): String {
    val state = sectionStates[section] ?: SectionState()
    return when {
        state.loading -> context.getString(R.string.common_loading)
        !state.loadedFresh && state.rows.isNotEmpty() -> {
            context.getString(R.string.common_item_count_cached, size)
        }
        !state.loadedFresh && state.rows.isEmpty() -> context.getString(R.string.common_not_loaded)
        else -> context.getString(R.string.common_item_count, size)
    }
}

internal fun sectionKindByCard(card: OsSectionCard): SectionKind? = when (card) {
    OsSectionCard.TOP_INFO -> null
    OsSectionCard.SHELL_RUNNER -> null
    OsSectionCard.GOOGLE_SYSTEM_SERVICE -> null
    OsSectionCard.SYSTEM -> SectionKind.SYSTEM
    OsSectionCard.SECURE -> SectionKind.SECURE
    OsSectionCard.GLOBAL -> SectionKind.GLOBAL
    OsSectionCard.ANDROID -> SectionKind.ANDROID
    OsSectionCard.JAVA -> SectionKind.JAVA
    OsSectionCard.LINUX -> SectionKind.LINUX
}

internal fun currentRowsForCard(
    card: OsSectionCard,
    sectionStates: Map<SectionKind, SectionState>,
    googleSystemServiceConfig: OsGoogleSystemServiceConfig,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    context: Context
): List<InfoRow> {
    return when (card) {
        OsSectionCard.TOP_INFO -> {
            val system = sectionStates[SectionKind.SYSTEM]?.rows ?: emptyList()
            val secure = sectionStates[SectionKind.SECURE]?.rows ?: emptyList()
            val global = sectionStates[SectionKind.GLOBAL]?.rows ?: emptyList()
            val android = sectionStates[SectionKind.ANDROID]?.rows ?: emptyList()
            val java = sectionStates[SectionKind.JAVA]?.rows ?: emptyList()
            val linux = sectionStates[SectionKind.LINUX]?.rows ?: emptyList()
            buildTopInfoRows(system, secure, global, android, java, linux)
        }

        OsSectionCard.SHELL_RUNNER -> emptyList()

        OsSectionCard.GOOGLE_SYSTEM_SERVICE -> {
            buildGoogleSystemServiceRows(
                context = context,
                config = googleSystemServiceConfig,
                defaults = googleSystemServiceDefaults
            )
        }

        else -> {
            val section = sectionKindByCard(card) ?: return emptyList()
            removeTopInfoRows(section, sectionStates[section]?.rows ?: emptyList())
        }
    }
}

internal fun exportSlug(card: OsSectionCard): String = when (card) {
    OsSectionCard.TOP_INFO -> "top-info"
    OsSectionCard.SHELL_RUNNER -> "shell-runner"
    OsSectionCard.GOOGLE_SYSTEM_SERVICE -> "google-system-service"
    OsSectionCard.SYSTEM -> "system-table"
    OsSectionCard.SECURE -> "secure-table"
    OsSectionCard.GLOBAL -> "global-table"
    OsSectionCard.ANDROID -> "android-properties"
    OsSectionCard.JAVA -> "java-properties"
    OsSectionCard.LINUX -> "linux-environment"
}

@Composable
internal fun OsCardExportAction(
    card: OsSectionCard,
    exportingCard: OsSectionCard?,
    onExportClick: () -> Unit
) {
    val isExporting = exportingCard == card
    val enabled = exportingCard == null || isExporting
    val tint = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackgroundVariant
    Icon(
        imageVector = if (isExporting) MiuixIcons.Regular.Refresh else MiuixIcons.Regular.Download,
        contentDescription = if (isExporting) "准备导出中" else "导出${card.title}",
        tint = tint,
        modifier = Modifier.clickable(enabled = enabled && !isExporting) {
            onExportClick()
        }
    )
}
