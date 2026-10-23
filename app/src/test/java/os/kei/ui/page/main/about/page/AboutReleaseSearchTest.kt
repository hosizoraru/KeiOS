package os.kei.ui.page.main.about.page

import android.app.Application
import android.content.res.Configuration
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AboutReleaseSearchTest {
    @Test
    fun releaseHighlightsAreSearchableInSupportedLanguages() {
        val application = RuntimeEnvironment.getApplication()
        val queriesByLanguage = mapOf(
            "zh-CN" to listOf("v1.19.0", "BGM", "封面", "桌面图标", "编辑", "文件导入", "提醒"),
            "en" to listOf("v1.19.0", "BGM", "artwork", "launcher icons", "unfinished edits", "imports", "reminders"),
            "ja" to listOf("v1.19.0", "BGM", "カバー", "ホームアイコン", "編集内容", "ファイル", "通知"),
        )
        queriesByLanguage.forEach { (language, queries) ->
            val configuration = Configuration(application.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }
            val context = application.createConfigurationContext(configuration)
            val release = buildAboutSearchTargets(
                context = context,
                appLabel = "KeiOS",
                privilegeStatus = "",
                permissionEntries = emptyList(),
                componentEntries = emptyList(),
            ).single { it.card == AboutSearchCard.Release }

            queries.forEach { query ->
                assertTrue("Release card must match '$query' in $language", release.matches(query))
            }
        }
    }
}
