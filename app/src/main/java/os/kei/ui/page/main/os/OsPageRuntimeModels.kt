package os.kei.ui.page.main.os

import android.graphics.Bitmap
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.ShortcutActivityClassOption
import os.kei.ui.page.main.os.shortcut.ShortcutInstalledAppOption
import os.kei.ui.page.main.os.shortcut.ShortcutSuggestionField
import os.kei.ui.page.main.os.state.OsCardImportTarget
import os.kei.ui.page.main.os.transfer.OsCardImportPreview

internal data class OsPageRuntimeState(
    val sectionStates: Map<SectionKind, SectionState> = defaultOsSectionStates(),
    val cacheLoaded: Boolean = false,
    val cachePersisted: Boolean = false,
    val initialVisibleRefreshComplete: Boolean = false,
    val uiStatePersistenceReady: Boolean = false,
    val refreshing: Boolean = false,
    val refreshProgress: Float = 0f,
    val runningShellCommandCardIds: Set<String> = emptySet(),
    val exportingCard: OsSectionCard? = null,
    val showCardManager: Boolean = false,
    val showActivityVisibilityManager: Boolean = false,
    val activityVisibilityQuery: String = "",
    val showShellCardVisibilityManager: Boolean = false,
    val shellCardVisibilityQuery: String = "",
    val showActivityShortcutEditor: Boolean = false,
    val activityCardEditMode: OsActivityCardEditMode = OsActivityCardEditMode.Edit,
    val editingActivityShortcutCardId: String? = null,
    val editingActivityShortcutBuiltIn: Boolean = false,
    val activityShortcutDraft: OsGoogleSystemServiceConfig? = null,
    val showShellCommandCardEditor: Boolean = false,
    val editingShellCommandCardId: String? = null,
    val shellCommandCardDraft: OsShellCommandCard? = null,
    val showShellCardDeleteConfirm: Boolean = false,
    val showActivityCardDeleteConfirm: Boolean = false,
    val pendingExportContent: String? = null,
    val pendingImportTarget: OsCardImportTarget? = null,
    val pendingCardImportPreview: OsCardImportPreview? = null,
    val cardTransferInProgress: Boolean = false,
)

internal data class OsActivitySuggestionUiState(
    val packageSuggestions: List<ShortcutInstalledAppOption> = emptyList(),
    val packageSuggestionsLoading: Boolean = false,
    val classSuggestions: List<ShortcutActivityClassOption> = emptyList(),
    val classSuggestionsLoading: Boolean = false,
)

internal data class OsActivitySuggestionChromeState(
    val showSheet: Boolean = false,
    val target: ShortcutSuggestionField = ShortcutSuggestionField.IntentAction,
    val packageQuery: String = "",
    val classQuery: String = "",
)

internal data class OsActivityShortcutIconUiState(
    val bitmaps: Map<String, Bitmap> = emptyMap(),
    val missingKeys: Set<String> = emptySet(),
    val packageBitmaps: Map<String, Bitmap> = emptyMap(),
    val missingPackages: Set<String> = emptySet(),
)

internal data class OsCardExpansionUiState(
    val activityCards: Map<String, Boolean> = emptyMap(),
    val shellCommandCards: Map<String, Boolean> = emptyMap(),
)

internal fun defaultOsSectionStates(): Map<SectionKind, SectionState> =
    mapOf(
        SectionKind.SYSTEM to SectionState(),
        SectionKind.SECURE to SectionState(),
        SectionKind.GLOBAL to SectionState(),
        SectionKind.ANDROID to SectionState(),
        SectionKind.JAVA to SectionState(),
        SectionKind.LINUX to SectionState(),
    )
