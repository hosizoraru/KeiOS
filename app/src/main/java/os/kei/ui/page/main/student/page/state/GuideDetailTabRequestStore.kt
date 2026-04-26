package os.kei.ui.page.main.student.page.state

import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.normalizeStudentGuideSourceUrl
import java.util.concurrent.ConcurrentHashMap

internal object GuideDetailTabRequestStore {
    private val targetTabs = ConcurrentHashMap<String, GuideBottomTab>()

    fun request(sourceUrl: String, tab: GuideBottomTab) {
        val source = normalizeStudentGuideSourceUrl(sourceUrl)
        if (source.isBlank()) return
        targetTabs[source] = tab
    }

    fun consume(sourceUrl: String): GuideBottomTab? {
        val source = normalizeStudentGuideSourceUrl(sourceUrl)
        if (source.isBlank()) return null
        return targetTabs.remove(source)
    }
}
