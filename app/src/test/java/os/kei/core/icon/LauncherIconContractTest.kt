package os.kei.core.icon

import android.content.pm.PackageManager
import org.junit.Test
import org.w3c.dom.Element
import os.kei.core.prefs.LauncherIconDesign
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import os.kei.ui.testing.repoFile

class LauncherIconContractTest {
    @Test
    fun `component package follows installed application id while class stays manifest namespace`() {
        val appleDebugComponent = LauncherIconDesign.Apple.componentSpec("os.kei.debug")
        val androidBenchmarkComponent = LauncherIconDesign.Android.componentSpec("os.kei")

        assertEquals("os.kei.debug", appleDebugComponent.packageName)
        assertEquals("os.kei.LauncherAppleDesigns", appleDebugComponent.className)
        assertEquals("os.kei", androidBenchmarkComponent.packageName)
        assertEquals("os.kei.LauncherAndroidDesigns", androidBenchmarkComponent.className)
    }

    @Test
    fun `component state specs place selected entry before inactive entries`() {
        val specs = LauncherIconDesign.Apple.componentStateSpecs("os.kei")

        assertEquals(2, specs.size)
        assertEquals("os.kei", specs[0].component.packageName)
        assertEquals("os.kei.LauncherAppleDesigns", specs[0].component.className)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, specs[0].enabledState)
        assertEquals("os.kei", specs[1].component.packageName)
        assertEquals("os.kei.LauncherAndroidDesigns", specs[1].component.className)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, specs[1].enabledState)
    }

    @Test
    fun `launcher manifest defaults to android designs and keeps apple designs disabled`() {
        val manifest = androidManifest()
        val application =
            manifest.documentElement
                .childElements("application")
                .single()

        assertEquals("@mipmap/ic_launcher_android", application.androidAttr("icon"))
        assertEquals("@mipmap/ic_launcher_round_android", application.androidAttr("roundIcon"))

        val mainActivity =
            application
                .childElements("activity")
                .single { it.androidAttr("name") == ".MainActivity" }
        assertFalse(mainActivity.hasLauncherFilter())

        val appleAlias = application.launcherAlias(".LauncherAppleDesigns")
        assertEquals("false", appleAlias.androidAttr("enabled"))
        assertEquals("@mipmap/ic_launcher_apple", appleAlias.androidAttr("icon"))
        assertEquals("@mipmap/ic_launcher_round_apple", appleAlias.androidAttr("roundIcon"))
        assertEquals(".MainActivity", appleAlias.androidAttr("targetActivity"))
        assertTrue(appleAlias.hasLauncherFilter())

        val androidAlias = application.launcherAlias(".LauncherAndroidDesigns")
        assertEquals("true", androidAlias.androidAttr("enabled"))
        assertEquals("@mipmap/ic_launcher_android", androidAlias.androidAttr("icon"))
        assertEquals("@mipmap/ic_launcher_round_android", androidAlias.androidAttr("roundIcon"))
        assertEquals(".MainActivity", androidAlias.androidAttr("targetActivity"))
        assertTrue(androidAlias.hasLauncherFilter())
    }

    private fun Element.launcherAlias(name: String): Element =
        childElements("activity-alias")
            .single { it.androidAttr("name") == name }

    private fun Element.hasLauncherFilter(): Boolean =
        childElements("intent-filter").any { filter ->
            val hasMainAction =
                filter.childElements("action").any {
                    it.androidAttr("name") == "android.intent.action.MAIN"
                }
            val hasLauncherCategory =
                filter.childElements("category").any {
                    it.androidAttr("name") == "android.intent.category.LAUNCHER"
                }
            hasMainAction && hasLauncherCategory
        }

    private fun Element.childElements(tagName: String): List<Element> {
        val nodes = getElementsByTagName(tagName)
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) as? Element }
            .filter { it.parentNode === this }
    }

    private fun Element.androidAttr(name: String): String = getAttributeNS(ANDROID_NAMESPACE, name)

    private fun androidManifest() =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(repoFile("app/src/main/AndroidManifest.xml"))

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
