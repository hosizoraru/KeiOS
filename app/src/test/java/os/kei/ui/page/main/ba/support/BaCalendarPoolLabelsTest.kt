package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaCalendarPoolLabelsTest {
    @Test
    fun `legacy Chinese calendar fallback decodes to resource fallback marker`() {
        val entries = decodeBaCalendarEntries(
            raw = """
                [
                  {
                    "id": 1,
                    "title": "Event",
                    "kindId": 31,
                    "kindName": "\u5176\u4ed6",
                    "beginAtMs": 1000,
                    "endAtMs": 3000,
                    "linkUrl": "https://example.com",
                    "imageUrl": ""
                  }
                ]
            """.trimIndent(),
            nowMs = 1500L
        )

        assertEquals(1, entries.size)
        assertTrue(entries.single().kindName.isBlank())
    }

    @Test
    fun `legacy Chinese pool tag decodes to resource fallback marker`() {
        val entries = decodeBaPoolEntries(
            raw = """
                [
                  {
                    "id": 1,
                    "name": "Recruitment",
                    "tagId": 6,
                    "tagName": "\u9650\u5b9a",
                    "startAtMs": 1000,
                    "endAtMs": 3000,
                    "linkUrl": "https://example.com",
                    "imageUrl": ""
                  }
                ]
            """.trimIndent(),
            nowMs = 1500L
        )

        assertEquals(1, entries.size)
        assertTrue(entries.single().tagName.isBlank())
    }

    @Test
    fun `source-specific labels stay available for unknown ids`() {
        assertEquals("Special", normalizeBaCalendarKindFallback("Special"))
        assertEquals("Custom Banner", normalizeBaPoolTagFallback("Custom Banner"))
    }
}
