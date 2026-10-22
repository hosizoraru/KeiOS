package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import kotlin.test.assertEquals

class BaGuideBgmNativeQueuePositionTest {
    @Test
    fun `removing an earlier track preserves the current media id and position`() {
        val nextIds = listOf("B", "C")
        val position = resolve(nextIds = nextIds, currentId = "B", currentPosition = 42_000L)

        assertEquals("B", nextIds[position.index])
        assertEquals(42_000L, position.positionMs)
    }

    @Test
    fun `shrinking the queue to its current track produces a valid index`() {
        val nextIds = listOf("C")
        val position = resolve(nextIds = nextIds, currentId = "C", currentPosition = 18_000L)

        assertEquals("C", nextIds[position.index])
        assertEquals(18_000L, position.positionMs)
    }

    @Test
    fun `reordering during a passive queue refresh preserves current playback over stale selection`() {
        val nextIds = listOf("C", "B", "A")
        val position =
            resolve(nextIds = nextIds, currentId = "A", currentPosition = 12_000L, selectedId = "B")

        assertEquals("A", nextIds[position.index])
        assertEquals(12_000L, position.positionMs)
    }

    @Test
    fun `explicit play or pause selects the requested track and supplied resume position`() {
        val nextIds = listOf("B", "A")
        listOf(true, false).forEach { playWhenReady ->
            val position =
                resolve(
                    nextIds = nextIds,
                    currentId = "A",
                    currentPosition = 12_000L,
                    selectedId = "B",
                    playWhenReady = playWhenReady,
                )

            assertEquals("B", nextIds[position.index])
            assertEquals(7_000L, position.positionMs)
        }
    }

    @Test
    fun `removing the current track uses the selected replacement and its resume position`() {
        val nextIds = listOf("B", "C")
        val position = resolve(nextIds = nextIds, currentId = "A", selectedId = "C")

        assertEquals("C", nextIds[position.index])
        assertEquals(7_000L, position.positionMs)
    }

    @Test
    fun `restart uses selected track at zero instead of preserving current playback`() {
        val nextIds = listOf("B", "A")
        val position =
            resolve(nextIds = nextIds, currentId = "A", selectedId = "B", restart = true)

        assertEquals("B", nextIds[position.index])
        assertEquals(0L, position.positionMs)
    }

    @Test
    fun `missing selection falls back to the first track and clamps negative positions`() {
        val nextIds = listOf("B", "C")
        val position =
            resolveBaGuideBgmNativeQueuePosition(
                nextMediaIds = nextIds,
                selectedMediaId = "missing",
                currentMediaId = "A",
                currentPositionMs = 12_000L,
                startPositionMs = -1L,
                playWhenReady = null,
                restart = false,
            )

        assertEquals("B", nextIds[position.index])
        assertEquals(0L, position.positionMs)
    }

    private fun resolve(
        nextIds: List<String>,
        currentId: String,
        currentPosition: Long = 12_000L,
        selectedId: String = currentId,
        playWhenReady: Boolean? = null,
        restart: Boolean = false,
    ): BaGuideBgmNativeQueuePosition =
        resolveBaGuideBgmNativeQueuePosition(
            nextMediaIds = nextIds,
            selectedMediaId = selectedId,
            currentMediaId = currentId,
            currentPositionMs = currentPosition,
            startPositionMs = 7_000L,
            playWhenReady = playWhenReady,
            restart = restart,
        )
}
