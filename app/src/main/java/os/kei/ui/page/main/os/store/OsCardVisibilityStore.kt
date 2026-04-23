package os.kei.ui.page.main.os

import com.tencent.mmkv.MMKV

internal object OsCardVisibilityStore {
    private const val KV_ID = "os_card_visibility_state"
    private const val LEGACY_KV_ID = "os_ui_state"
    private const val KEY_VISIBLE_CARDS = "visible_os_cards"
    private val DEFAULT_VISIBLE = setOf(
        OsSectionCard.SHELL_RUNNER,
        OsSectionCard.LINUX,
        OsSectionCard.GOOGLE_SYSTEM_SERVICE
    )
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private val legacyStore: MMKV by lazy { MMKV.mmkvWithID(LEGACY_KV_ID) }

    private fun resolveVisibleCards(raw: String): Set<OsSectionCard> {
        if (raw.isBlank()) return emptySet()
        val names = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val resolved = OsSectionCard.entries.filter { names.contains(it.name) }.toSet()
        return resolved
    }

    fun defaultVisibleCards(): Set<OsSectionCard> {
        return DEFAULT_VISIBLE
    }

    fun loadVisibleCards(): Set<OsSectionCard> {
        val newStore = store
        if (newStore.containsKey(KEY_VISIBLE_CARDS)) {
            return resolveVisibleCards(newStore.decodeString(KEY_VISIBLE_CARDS, "").orEmpty())
                .ifEmpty { DEFAULT_VISIBLE }
        }
        val legacy = legacyStore
        if (!legacy.containsKey(KEY_VISIBLE_CARDS)) return DEFAULT_VISIBLE
        val migrated = resolveVisibleCards(legacy.decodeString(KEY_VISIBLE_CARDS, "").orEmpty())
            .ifEmpty { DEFAULT_VISIBLE }
        saveVisibleCards(migrated)
        return migrated
    }

    fun saveVisibleCards(cards: Set<OsSectionCard>) {
        store.encode(
            KEY_VISIBLE_CARDS,
            cards.map { it.name }.sorted().joinToString(",")
        )
        legacyStore.removeValueForKey(KEY_VISIBLE_CARDS)
    }
}
