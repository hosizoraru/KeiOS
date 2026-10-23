package os.kei.ui.page.main.student.catalog.component

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import os.kei.ui.page.main.student.catalog.testCatalogEntry
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmArtworkResolverTest {
    @Test
    fun `blank aliases do not match an unrelated student`() {
        val favorite = favorite("Unknown")
        val catalog = catalog(entry(1, "Aris"), entry(2, "Koyuki"))

        assertEquals(favorite, favorite.withResolvedCatalogStudentMetadata(catalog))
        assertEquals("", favorite.resolveStudentArtworkImageUrl(catalog))
    }

    @Test
    fun `precise normalized alias wins over earlier partial name`() {
        val precise = entry(2, "Aris Maid").copy(alias = "ARIS （MAID）")
        val catalog = catalog(entry(1, "Aris"), precise)
        val favorite = favorite(" \taris (maid)\n")

        assertEquals(precise.iconUrl, favorite.withResolvedCatalogStudentMetadata(catalog).studentImageUrl)
    }

    @Test
    fun `ambiguous precise names do not fall through to another student`() {
        val favorite = favorite("Aris")
        val catalog = catalog(
            entry(1, "Aris"),
            entry(2, "Another student").copy(alias = "Aris"),
            entry(3, "Aris Maid"),
        )

        assertEquals(favorite, favorite.withResolvedCatalogStudentMetadata(catalog))
    }

    @Test
    fun `partial name fallback requires exactly one matching student`() {
        val target = entry(2, "Aris Maid")
        val favorite = favorite("Aris")

        assertEquals(
            target.iconUrl,
            favorite.withResolvedCatalogStudentMetadata(catalog(entry(1, "Koyuki"), target)).studentImageUrl,
        )
        assertEquals(
            favorite,
            favorite.withResolvedCatalogStudentMetadata(catalog(target, entry(3, "Aris Dress"))),
        )
    }

    @Test
    fun `explicit missing content id never borrows an exact namesake`() {
        val catalog = catalog(entry(1, "Aris"))
        listOf(
            "https://www.gamekee.com/ba/99",
            "https://www.gamekee.com/ba/99.html",
            "https://www.gamekee.com/ba/?content_id=99",
        ).forEach { sourceUrl ->
            val favorite = favorite("Aris", sourceUrl = sourceUrl)
            assertEquals(favorite, favorite.withResolvedCatalogStudentMetadata(catalog), sourceUrl)
            assertEquals("", favorite.resolveStudentArtworkImageUrl(catalog), sourceUrl)
        }
    }

    @Test
    fun `entry metadata merge refuses a different explicit student identity`() {
        val favorite = favorite("Aris", sourceUrl = "https://www.gamekee.com/ba/99")

        assertEquals(favorite, favorite.withCatalogEntryStudentMetadata(entry(1, "Aris")))
    }

    @Test
    fun `explicit content id takes priority over a matching name`() {
        val target = entry(2, "Aris Maid")
        val favorite = favorite("Aris", sourceUrl = target.detailUrl)

        assertEquals(
            target.iconUrl,
            favorite.withResolvedCatalogStudentMetadata(catalog(entry(1, "Aris"), target)).studentImageUrl,
        )
    }

    @Test
    fun `duplicate catalog listings do not make one identity ambiguous`() {
        val target = entry(2, "Aris")
        val catalog = BaGuideCatalogBundle(
            entriesByTab = mapOf(
                BaGuideCatalogTab.Student to listOf(target),
                BaGuideCatalogTab.NpcSatellite to listOf(target),
            ),
            syncedAtMs = 0L,
        )

        assertEquals(target.iconUrl, favorite("Aris").withResolvedCatalogStudentMetadata(catalog).studentImageUrl)
    }

    @Test
    fun `blank metadata merges trimmed catalog values without losing playback identity`() {
        val target = entry(2, " Aris ").copy(
            iconUrl = " https://example.com/aris.png \n",
            detailUrl = " https://www.gamekee.com/ba/2 ",
        )
        val favorite = favorite(" \t").copy(
            studentImageUrl = "\n",
            imageUrl = "\t",
            note = "keep note",
            favoritedAtMs = 123L,
        )

        assertEquals(
            favorite.copy(
                studentTitle = "Aris",
                studentImageUrl = "https://example.com/aris.png",
                imageUrl = "https://example.com/aris.png",
                sourceUrl = "https://www.gamekee.com/ba/2",
            ),
            favorite.withCatalogEntryStudentMetadata(target),
        )
    }

    @Test
    fun `existing artwork remains preferred and metadata whitespace is trimmed`() {
        val favorite = favorite(" Aris ", sourceUrl = " https://www.gamekee.com/ba/2 ").copy(
            studentImageUrl = " https://example.com/original.png ",
            imageUrl = " https://example.com/gallery.png ",
        )
        val catalog = catalog(entry(2, "Aris"))
        val merged = favorite.withResolvedCatalogStudentMetadata(catalog)

        assertEquals("https://example.com/original.png", merged.studentImageUrl)
        assertEquals("https://example.com/gallery.png", merged.imageUrl)
        assertEquals("Aris", merged.studentTitle)
        assertEquals("https://www.gamekee.com/ba/2", merged.sourceUrl)
        assertEquals("https://example.com/original.png", merged.resolveStudentArtworkImageUrl(catalog))
        assertEquals("https://example.com/original.png", merged.resolvePlaybackArtworkImageUrl())
    }

    private fun favorite(studentTitle: String, sourceUrl: String = ""): GuideBgmFavoriteItem =
        testBgmFavorite(
            audioUrl = "https://example.com/bgm.mp3",
            sourceUrl = sourceUrl,
            studentTitle = studentTitle,
        )

    private fun entry(id: Long, name: String): BaGuideCatalogEntry =
        testCatalogEntry(contentId = id, name = name, iconUrl = "https://example.com/$id.png")

    private fun catalog(vararg entries: BaGuideCatalogEntry): BaGuideCatalogBundle =
        BaGuideCatalogBundle(
            entriesByTab = mapOf(BaGuideCatalogTab.Student to entries.toList()),
            syncedAtMs = 0L,
        )
}
