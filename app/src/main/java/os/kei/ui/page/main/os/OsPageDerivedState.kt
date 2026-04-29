package os.kei.ui.page.main.os

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import os.kei.R
import os.kei.ui.page.main.os.shell.OsShellCommandCard

internal data class OsPageDerivedState(
    val query: String,
    val displayedTopInfoRows: List<InfoRow>,
    val groupedTopInfoRows: List<Pair<String, List<InfoRow>>>,
    val shellRunnerRows: List<InfoRow>,
    val displayedSystemRows: List<InfoRow>,
    val displayedSecureRows: List<InfoRow>,
    val displayedGlobalRows: List<InfoRow>,
    val displayedAndroidRows: List<InfoRow>,
    val displayedJavaRows: List<InfoRow>,
    val displayedLinuxRows: List<InfoRow>,
    val prunedSystemRows: List<InfoRow>,
    val prunedSecureRows: List<InfoRow>,
    val prunedGlobalRows: List<InfoRow>,
    val prunedAndroidRows: List<InfoRow>,
    val prunedJavaRows: List<InfoRow>,
    val prunedLinuxRows: List<InfoRow>,
    val overviewUiState: OsOverviewUiState,
)

@Composable
internal fun rememberOsPageDerivedState(
    context: Context,
    queryApplied: String,
    shizukuStatus: String,
    shellSavedCountLabel: String,
    shellCommandCards: List<OsShellCommandCard>,
    sectionStates: Map<SectionKind, SectionState>,
    topInfoExpanded: Boolean,
    systemTableExpanded: Boolean,
    secureTableExpanded: Boolean,
    globalTableExpanded: Boolean,
    androidPropsExpanded: Boolean,
    javaPropsExpanded: Boolean,
    linuxEnvExpanded: Boolean,
    isDark: Boolean,
    inactiveColor: Color,
    cachedColor: Color,
    refreshingColor: Color,
    syncedColor: Color,
    surfaceColor: Color,
    refreshing: Boolean,
    refreshProgress: Float,
    cachePersisted: Boolean,
    visibleCards: Set<OsSectionCard>,
    activityShortcutCards: List<os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard>,
): OsPageDerivedState {
    val systemRows = sectionStates[SectionKind.SYSTEM]?.rows ?: emptyList()
    val secureRows = sectionStates[SectionKind.SECURE]?.rows ?: emptyList()
    val globalRows = sectionStates[SectionKind.GLOBAL]?.rows ?: emptyList()
    val androidRows = sectionStates[SectionKind.ANDROID]?.rows ?: emptyList()
    val javaRows = sectionStates[SectionKind.JAVA]?.rows ?: emptyList()
    val linuxRows = sectionStates[SectionKind.LINUX]?.rows ?: emptyList()

    val topInfoRows = remember(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows) {
        buildTopInfoRows(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows)
    }
    val prunedSystemRows = remember(systemRows) { removeTopInfoRows(SectionKind.SYSTEM, systemRows) }
    val prunedSecureRows = remember(secureRows) { removeTopInfoRows(SectionKind.SECURE, secureRows) }
    val prunedGlobalRows = remember(globalRows) { removeTopInfoRows(SectionKind.GLOBAL, globalRows) }
    val prunedAndroidRows = remember(androidRows) { removeTopInfoRows(SectionKind.ANDROID, androidRows) }
    val prunedJavaRows = remember(javaRows) { removeTopInfoRows(SectionKind.JAVA, javaRows) }
    val prunedLinuxRows = remember(linuxRows) { removeTopInfoRows(SectionKind.LINUX, linuxRows) }

    val query = queryApplied.trim()
    val displayedTopInfoRows = remember(query, topInfoRows, topInfoExpanded) {
        if (query.isBlank() && !topInfoExpanded) {
            topInfoRows
        } else {
            sortRowsByType(filterRows(topInfoRows, query))
        }
    }
    val displayedSystemRows = remember(query, prunedSystemRows, systemTableExpanded) {
        if (query.isBlank() && !systemTableExpanded) {
            prunedSystemRows
        } else {
            sortRowsByType(filterRows(prunedSystemRows, query))
        }
    }
    val displayedSecureRows = remember(query, prunedSecureRows, secureTableExpanded) {
        if (query.isBlank() && !secureTableExpanded) {
            prunedSecureRows
        } else {
            sortRowsByType(filterRows(prunedSecureRows, query))
        }
    }
    val displayedGlobalRows = remember(query, prunedGlobalRows, globalTableExpanded) {
        if (query.isBlank() && !globalTableExpanded) {
            prunedGlobalRows
        } else {
            sortRowsByType(filterRows(prunedGlobalRows, query))
        }
    }
    val displayedAndroidRows = remember(query, prunedAndroidRows, androidPropsExpanded) {
        if (query.isBlank() && !androidPropsExpanded) {
            prunedAndroidRows
        } else {
            sortRowsByType(filterRows(prunedAndroidRows, query))
        }
    }
    val displayedJavaRows = remember(query, prunedJavaRows, javaPropsExpanded) {
        if (query.isBlank() && !javaPropsExpanded) {
            prunedJavaRows
        } else {
            sortRowsByType(filterRows(prunedJavaRows, query))
        }
    }
    val displayedLinuxRows = remember(query, prunedLinuxRows, linuxEnvExpanded) {
        if (query.isBlank() && !linuxEnvExpanded) {
            prunedLinuxRows
        } else {
            sortRowsByType(filterRows(prunedLinuxRows, query))
        }
    }
    val groupedTopInfoRows = remember(context, displayedTopInfoRows, topInfoExpanded, query) {
        if (query.isBlank() && !topInfoExpanded) emptyList() else groupTopInfoRows(context, displayedTopInfoRows)
    }
    val shellRunnerRows = remember(
        shizukuStatus,
        context,
        shellSavedCountLabel,
        shellCommandCards
    ) {
        listOf(
            InfoRow(
                key = context.getString(R.string.os_shell_card_status_label),
                value = shizukuStatus
            ),
            InfoRow(
                key = shellSavedCountLabel,
                value = context.getString(R.string.common_item_count, shellCommandCards.size)
            )
        )
    }
    val visibleRowsCount = remember(
        displayedTopInfoRows.size,
        displayedSystemRows.size,
        displayedSecureRows.size,
        displayedGlobalRows.size,
        displayedAndroidRows.size,
        displayedJavaRows.size,
        displayedLinuxRows.size
    ) {
        displayedTopInfoRows.size +
            displayedSystemRows.size +
            displayedSecureRows.size +
            displayedGlobalRows.size +
            displayedAndroidRows.size +
            displayedJavaRows.size +
            displayedLinuxRows.size
    }
    val overviewUiState = remember(
        isDark,
        inactiveColor,
        cachedColor,
        refreshingColor,
        syncedColor,
        refreshing,
        refreshProgress,
        cachePersisted,
        visibleCards,
        sectionStates,
        topInfoRows.size,
        visibleRowsCount,
        activityShortcutCards,
        shellCommandCards,
        surfaceColor
    ) {
        buildOsOverviewUiState(
            context = context,
            isDark = isDark,
            inactiveColor = inactiveColor,
            cachedColor = cachedColor,
            refreshingColor = refreshingColor,
            syncedColor = syncedColor,
            surfaceColor = surfaceColor,
            refreshing = refreshing,
            refreshProgress = refreshProgress,
            cachePersisted = cachePersisted,
            visibleCards = visibleCards,
            sectionStates = sectionStates,
            topInfoCount = topInfoRows.size,
            visibleRowsCount = visibleRowsCount,
            activityCards = activityShortcutCards,
            shellCommandCards = shellCommandCards
        )
    }

    return OsPageDerivedState(
        query = query,
        displayedTopInfoRows = displayedTopInfoRows,
        groupedTopInfoRows = groupedTopInfoRows,
        shellRunnerRows = shellRunnerRows,
        displayedSystemRows = displayedSystemRows,
        displayedSecureRows = displayedSecureRows,
        displayedGlobalRows = displayedGlobalRows,
        displayedAndroidRows = displayedAndroidRows,
        displayedJavaRows = displayedJavaRows,
        displayedLinuxRows = displayedLinuxRows,
        prunedSystemRows = prunedSystemRows,
        prunedSecureRows = prunedSecureRows,
        prunedGlobalRows = prunedGlobalRows,
        prunedAndroidRows = prunedAndroidRows,
        prunedJavaRows = prunedJavaRows,
        prunedLinuxRows = prunedLinuxRows,
        overviewUiState = overviewUiState
    )
}
