package os.kei.ui.page.main.student.rendering

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import os.kei.core.prefs.KeiMmkv

/** Presentation choices, not promises about the system's Vulkan/ANGLE driver selection. */
internal enum class GuideViewerRendering { SystemWebView, CompatibleFrames }

internal data class GuideViewerRenderingPreferences(
    val lobby: GuideViewerRendering,
    val model: GuideViewerRendering,
)

internal fun decodeGuideViewerRenderingPreferences(lobby: String?, model: String?): GuideViewerRenderingPreferences {
    fun decode(value: String?) = GuideViewerRendering.entries.firstOrNull { it.name == value }
        ?: GuideViewerRendering.SystemWebView
    return GuideViewerRenderingPreferences(decode(lobby), decode(model))
}

/** Changes apply on the next entry, so a settings change cannot interrupt an active animation. */
internal object GuideViewerRenderingStore {
    private val store get() = KeiMmkv.byId("ba_viewer_rendering")
    private val mutable by lazy {
        MutableStateFlow(decodeGuideViewerRenderingPreferences(store.decodeString("lobby"), store.decodeString("model")))
    }
    val preferences get() = mutable.asStateFlow()
    fun setLobby(value: GuideViewerRendering) {
        store.encode("lobby", value.name); mutable.value = mutable.value.copy(lobby = value)
    }
    fun setModel(value: GuideViewerRendering) {
        store.encode("model", value.name); mutable.value = mutable.value.copy(model = value)
    }
}
