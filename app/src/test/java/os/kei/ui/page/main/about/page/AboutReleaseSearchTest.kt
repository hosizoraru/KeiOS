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
            "zh-CN" to listOf("v1.17.0", "Spine", "3D", "WebView", "中文输入", "超级岛"),
            "en" to listOf("v1.17.0", "Spine", "3D", "WebView", "Chinese composition", "Super Island"),
            "ja" to listOf("v1.17.0", "Spine", "3D", "WebView", "中国語", "スーパーアイランド"),
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
