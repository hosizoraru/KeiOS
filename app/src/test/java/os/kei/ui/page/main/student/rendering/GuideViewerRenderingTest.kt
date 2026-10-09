package os.kei.ui.page.main.student.rendering

import org.junit.Test
import kotlin.test.assertEquals

class GuideViewerRenderingTest {
    @Test
    fun newOrUnknownPreferencesUseSystemWebViewForBothViewers() {
        val expected = GuideViewerRenderingPreferences(GuideViewerRendering.SystemWebView, GuideViewerRendering.SystemWebView)
        assertEquals(expected, decodeGuideViewerRenderingPreferences(null, null))
        assertEquals(expected, decodeGuideViewerRenderingPreferences("obsolete", ""))
    }

    @Test
    fun savedCompatibleChoiceDoesNotChangeTheOtherViewersDefault() {
        assertEquals(GuideViewerRenderingPreferences(GuideViewerRendering.CompatibleFrames, GuideViewerRendering.SystemWebView),
            decodeGuideViewerRenderingPreferences("CompatibleFrames", null))
        assertEquals(GuideViewerRenderingPreferences(GuideViewerRendering.SystemWebView, GuideViewerRendering.CompatibleFrames),
            decodeGuideViewerRenderingPreferences(null, "CompatibleFrames"))
    }

    @Test
    fun explicitSystemAndCompatibleChoicesSurvivePreferenceDecoding() {
        assertEquals(GuideViewerRenderingPreferences(GuideViewerRendering.SystemWebView, GuideViewerRendering.CompatibleFrames),
            decodeGuideViewerRenderingPreferences("SystemWebView", "CompatibleFrames"))
    }
}
