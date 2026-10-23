package os.kei.ui.page.main.about.section

import org.junit.Test
import java.io.File
import kotlin.test.assertContains
import os.kei.ui.testing.repoRoot
/**
 * The release card documents a line, so it is pinned to one.
 *
 * Every other version on the About page is read from the build, and this card deliberately is not: its
 * body lists what a particular release brought, so a header that tracked the running build would print
 * one version above notes describing another. Pinning it here is what makes updating the card a
 * deliberate act at release time instead of something that silently falls behind — which it had, sitting
 * at 1.14.0 through 117 commits.
 */
class AboutReleaseCardContentTest {
    @Test
    fun releaseCardPublishesV1190InEverySupportedLocale() {
        aboutReleaseResourceFiles().forEach { resourceFile ->
            val resources = resourceFile.readText()

            assertContains(
                resources,
                """<string name="about_card_release_title">Release v1.19.0</string>""",
                message = "${resourceFile.path} must publish the v1.19.0 card title",
            )
            assertContains(
                resources,
                """<string name="about_release_value_version">v1.19.0 · Android 15+ · arm64-v8a</string>""",
                message = "${resourceFile.path} must publish the v1.19.0 version line",
            )
        }
    }

}

private fun aboutReleaseResourceFiles(): List<File> {
    val projectRoot = repoRoot()
    return listOf(
        "app/src/main/res/values/strings_about.xml",
        "app/src/main/res/values-zh-rCN/strings_about.xml",
        "app/src/main/res/values-en/strings_about.xml",
        "app/src/main/res/values-ja/strings_about.xml",
    ).map(projectRoot::resolve)
}
