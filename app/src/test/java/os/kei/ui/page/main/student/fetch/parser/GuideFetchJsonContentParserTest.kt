package os.kei.ui.page.main.student.fetch.parser

import kotlin.test.assertEquals
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class GuideFetchJsonContentParserTest {
    @Test
    fun `terrain rows keep first value when GameKee payload contains replacement candidates`() {
        val detail = parseGuideDetailFromObjectContentJson(
            raw = objectContentJson(
                row(
                    "屋外",
                    textCell("B"),
                    imageCell("//cdn.example/outdoor-b.png"),
                    textCell(""),
                    textCell("C"),
                    imageCell("//cdn.example/outdoor-c.png"),
                    textCell("D"),
                    imageCell("//cdn.example/outdoor-d.png")
                ),
                row(
                    "屋内",
                    textCell("D"),
                    imageCell("//cdn.example/indoor-d.png"),
                    textCell(""),
                    textCell("A"),
                    imageCell("//cdn.example/indoor-a.png"),
                    textCell("B"),
                    imageCell("//cdn.example/indoor-b.png")
                )
            ),
            sourceUrl = "https://www.gamekee.com/ba/tj/591006.html"
        )

        val profileValues = detail.profileRows.associate { it.key to it.value }
        assertEquals("B", profileValues["屋外"])
        assertEquals("D", profileValues["屋内"])
    }

    @Test
    fun `momotalk unlock level clears status text instead of using memory lobby level`() {
        val status = "奉行不回头看过去的原则。"
        val detail = parseGuideDetailFromObjectContentJson(
            raw = objectContentJson(
                row("回忆大厅解锁等级", textCell("5")),
                row("MomoTalk解锁等级", textCell(status)),
                row("MomoTalk状态消息", textCell(status))
            ),
            sourceUrl = "https://www.gamekee.com/ba/tj/591006.html"
        )

        val profileValues = detail.profileRows.associate { it.key to it.value }
        assertEquals("", profileValues["MomoTalk解锁等级"])
        assertEquals(status, profileValues["MomoTalk状态消息"])
    }

    @Test
    fun `momotalk unlock level keeps numeric level lists`() {
        val detail = parseGuideDetailFromObjectContentJson(
            raw = objectContentJson(
                row("MomoTalk解锁等级", textCell("Lv.2、Lv.3、5级")),
                row("MomoTalk状态消息", textCell("今天也会认真执行任务。"))
            ),
            sourceUrl = "https://www.gamekee.com/ba/tj/sample.html"
        )

        val profileValues = detail.profileRows.associate { it.key to it.value }
        assertEquals("2 / 3 / 5", profileValues["MomoTalk解锁等级"])
    }

    private fun objectContentJson(vararg rows: JSONArray): String {
        return JSONObject()
            .put(
                "baseData",
                JSONArray().apply {
                    rows.forEach(::put)
                }
            )
            .toString()
    }

    private fun row(key: String, vararg cells: JSONObject): JSONArray {
        return JSONArray()
            .put(textCell(key, isGlobal = true))
            .apply {
                cells.forEach(::put)
            }
    }

    private fun textCell(value: String, isGlobal: Boolean = false): JSONObject {
        return JSONObject()
            .put("type", "text")
            .put("value", value)
            .apply {
                if (isGlobal) put("isGlobal", true)
            }
    }

    private fun imageCell(value: String): JSONObject {
        return JSONObject()
            .put("type", "image")
            .put("value", value)
    }
}
