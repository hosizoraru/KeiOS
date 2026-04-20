package com.example.keios.ui.page.main.os.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntRect
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.shortcut.ShortcutIntentExtra
import com.example.keios.ui.page.main.os.shortcut.ShortcutIntentExtraType
import com.example.keios.ui.page.main.os.shortcut.ensureEditorShortcutIntentExtras

internal data class OsGoogleSystemServiceIntentExtraController(
    val editableExtras: List<ShortcutIntentExtra>,
    val intentExtraTypePopupExpanded: Map<Int, Boolean>,
    val intentExtraTypePopupAnchors: Map<Int, IntRect?>,
    val onAddIntentExtra: () -> Unit,
    val onExtraKeyChange: (Int, String) -> Unit,
    val onExtraTypeChange: (Int, ShortcutIntentExtraType) -> Unit,
    val onExtraTypeExpandedChange: (Int, Boolean) -> Unit,
    val onExtraTypeAnchorBoundsChange: (Int, IntRect?) -> Unit,
    val onExtraValueChange: (Int, String) -> Unit,
    val onRemoveIntentExtra: (Int) -> Unit
)

@Composable
internal fun rememberOsGoogleSystemServiceIntentExtraController(
    draft: OsGoogleSystemServiceConfig,
    onDraftChange: (OsGoogleSystemServiceConfig) -> Unit
): OsGoogleSystemServiceIntentExtraController {
    val intentExtraTypePopupExpanded = remember { mutableStateMapOf<Int, Boolean>() }
    val intentExtraTypePopupAnchors = remember { mutableStateMapOf<Int, IntRect?>() }
    val editableExtras = ensureEditorShortcutIntentExtras(draft.intentExtras)

    fun cleanupIntentExtraPopupState(size: Int) {
        intentExtraTypePopupExpanded.keys
            .filter { it >= size }
            .forEach { key ->
                intentExtraTypePopupExpanded.remove(key)
                intentExtraTypePopupAnchors.remove(key)
            }
    }

    fun commitIntentExtras(next: List<ShortcutIntentExtra>) {
        val finalList = if (next.isEmpty()) {
            listOf(ShortcutIntentExtra())
        } else {
            next
        }
        cleanupIntentExtraPopupState(finalList.size)
        onDraftChange(draft.copy(intentExtras = finalList))
    }

    fun updateIntentExtra(index: Int, transform: (ShortcutIntentExtra) -> ShortcutIntentExtra) {
        val current = ensureEditorShortcutIntentExtras(draft.intentExtras).toMutableList()
        if (index !in current.indices) return
        current[index] = transform(current[index])
        commitIntentExtras(current)
    }

    val onAddIntentExtra = {
        val current = ensureEditorShortcutIntentExtras(draft.intentExtras)
        commitIntentExtras(current + ShortcutIntentExtra())
    }
    val onExtraKeyChange: (Int, String) -> Unit = { index, input ->
        updateIntentExtra(index) { current -> current.copy(key = input) }
    }
    val onExtraTypeChange: (Int, ShortcutIntentExtraType) -> Unit = { index, type ->
        updateIntentExtra(index) { current -> current.copy(type = type) }
    }
    val onExtraTypeExpandedChange: (Int, Boolean) -> Unit = { index, expanded ->
        intentExtraTypePopupExpanded[index] = expanded
    }
    val onExtraTypeAnchorBoundsChange: (Int, IntRect?) -> Unit = { index, bounds ->
        intentExtraTypePopupAnchors[index] = bounds
    }
    val onExtraValueChange: (Int, String) -> Unit = { index, input ->
        updateIntentExtra(index) { current -> current.copy(value = input) }
    }
    val onRemoveIntentExtra: (Int) -> Unit = { index ->
        val current = ensureEditorShortcutIntentExtras(draft.intentExtras).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            commitIntentExtras(current)
        }
    }

    return OsGoogleSystemServiceIntentExtraController(
        editableExtras = editableExtras,
        intentExtraTypePopupExpanded = intentExtraTypePopupExpanded,
        intentExtraTypePopupAnchors = intentExtraTypePopupAnchors,
        onAddIntentExtra = onAddIntentExtra,
        onExtraKeyChange = onExtraKeyChange,
        onExtraTypeChange = onExtraTypeChange,
        onExtraTypeExpandedChange = onExtraTypeExpandedChange,
        onExtraTypeAnchorBoundsChange = onExtraTypeAnchorBoundsChange,
        onExtraValueChange = onExtraValueChange,
        onRemoveIntentExtra = onRemoveIntentExtra
    )
}
