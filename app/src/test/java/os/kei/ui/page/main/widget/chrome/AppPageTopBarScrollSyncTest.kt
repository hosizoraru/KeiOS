package os.kei.ui.page.main.widget.chrome

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppPageTopBarScrollSyncTest {
    @Test
    fun `page is settled at top only when first item is fully visible and list is idle`() {
        assertTrue(
            isPageSettledAtTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                listScrollInProgress = false
            )
        )

        assertFalse(
            isPageSettledAtTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 1,
                listScrollInProgress = false
            )
        )

        assertFalse(
            isPageSettledAtTop(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                listScrollInProgress = false
            )
        )

        assertFalse(
            isPageSettledAtTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                listScrollInProgress = true
            )
        )
    }
}
