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
            "zh-CN" to listOf("v1.18.0", "Liquid Sheet", "长发行日志", "BGM", "通知", "页面切换"),
            "en" to listOf("v1.18.0", "Liquid Sheet", "long release notes", "BGM", "notification", "page transitions"),
            "ja" to listOf("v1.18.0", "Liquid Sheet", "長いリリースノート", "BGM", "通知", "ページ切替"),
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
