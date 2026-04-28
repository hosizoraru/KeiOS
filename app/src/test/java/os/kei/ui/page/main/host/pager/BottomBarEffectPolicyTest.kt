package os.kei.ui.page.main.host.pager

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class BottomBarEffectPolicyTest {
    @Test
    fun `default policy keeps full effects during scroll`() {
        assertFalse(
            shouldReduceBottomBarEffectsDuringMotion(
                scrollEffectReductionEnabled = false,
                pagerScrollInProgress = true,
                activePageListScrollInProgress = true
            )
        )
    }

    @Test
    fun `enabled policy reduces effects while pager or list scrolls`() {
        assertTrue(
            shouldReduceBottomBarEffectsDuringMotion(
                scrollEffectReductionEnabled = true,
                pagerScrollInProgress = true,
                activePageListScrollInProgress = false
            )
        )
        assertTrue(
            shouldReduceBottomBarEffectsDuringMotion(
                scrollEffectReductionEnabled = true,
                pagerScrollInProgress = false,
                activePageListScrollInProgress = true
            )
        )
    }
}
