package os.kei.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/** Real, cached players share the existing adaptive journey's two-replay budget. */
internal fun MacrobenchmarkScope.exerciseGuideViewersAndReturn() {
    val appId = profileCaptureAppId()
    // OEM AVD direct WebView composition can block HWUI. Collect the fully visible compatible
    // path on this target; it still executes the real WebGL/Wiki runtime and native glass chrome.
    for (presentation in listOf("compatible")) {
        launchCaptureViewer(appId, "model", presentation)
        waitForTestTag(GUIDE_MODEL3D_READY, timeoutMs = 75_000)
        clickViewerAction("guide_model_3d_tools")
        clickViewerAction("guide_model_3d_background")
        check(device.wait(Until.hasObject(By.text(viewerLabel("guide_model_3d_background_hex"))), 5_000)) {
            "Pad background palette did not stay in the model tools"
        }
        // Text-field child semantics own accessibility, so assert its visible value and real IME.
        val colorInput = device.findObject(By.text(Pattern.compile("#[A-Fa-f0-9]{6,8}")))
        checkNotNull(colorInput) { "Missing editable background color" }.click()
        val imePackage = device.executeShellCommand("settings get secure default_input_method")
            .trim().substringBefore('/')
        check(device.wait(Until.hasObject(By.pkg(imePackage)), 10_000)) { "Color input did not open the IME" }
        device.pressBack()
        // Keep the real palette and Liquid panel open while the camera is manipulated.
        val w = device.displayWidth
        val h = device.displayHeight
        check(device.swipe(w * 3 / 10, h / 2, w * 5 / 10, h * 5 / 10, 24))
        val ready = device.findObject(testTagSelector(GUIDE_MODEL3D_READY))
        val bounds = ready.visibleBounds
        ready.pinchOpen(0.25f)
        clickViewerAction("guide_gallery_dynamic_lobby_hide_controls")
        check(device.wait(Until.gone(By.res("guide_web_lobby_header")), 5_000))
        check(device.click(bounds.centerX(), bounds.centerY()))
        check(device.wait(Until.hasObject(By.desc(viewerLabel("guide_model_3d_tools"))), 5_000))
        clickViewerAction("guide_gallery_dynamic_lobby_reset_view")
        // The inline palette stays inside the tools; one Back closes it and another closes tools.
        device.pressBack()
        device.pressBack()
        clickViewerAction("common_close")
        waitForTestTag(BA_PAGE_ROOT, timeoutMs = 15_000)

        launchCaptureViewer(appId, "lobby", presentation)
        waitForTestTag(GUIDE_LOBBY_READY, timeoutMs = 75_000)
        clickViewerAction("guide_gallery_dynamic_lobby_actions")
        waitForTestTag(SNAPSHOT_MENU_PANEL)
        device.pressBack()
        clickViewerAction("guide_gallery_dynamic_lobby_hide_controls")
        check(device.wait(Until.gone(By.res("guide_web_lobby_header")), 5_000))
        check(device.click(device.displayWidth / 2, device.displayHeight / 2))
        check(device.wait(Until.hasObject(By.desc(viewerLabel("common_close"))), 5_000))
        clickViewerAction("ba_catalog_bgm_action_mute")
        clickViewerAction("ba_catalog_bgm_action_restore_volume")
        clickViewerAction("common_close")
        waitForTestTag(BA_PAGE_ROOT, timeoutMs = 15_000)
    }
}

private fun MacrobenchmarkScope.launchCaptureViewer(appId: String, viewer: String, presentation: String) {
    val result = device.executeShellCommand("am start -W -n $appId/os.kei.profilecapture.GuideViewerCaptureActivity " +
        "--es viewer $viewer --es presentation $presentation")
    check("Error:" !in result && "Exception" !in result) { "Cannot enter $viewer/$presentation: $result" }
}

private fun MacrobenchmarkScope.clickViewerAction(resource: String) {
    val label = viewerLabel(resource)
    val selector = By.desc(label)
    val textSelector = By.text(label)
    check(device.wait(Until.hasObject(selector), 5_000) || device.hasObject(textSelector)) {
        "Missing viewer action: $resource ($label)"
    }
    val node = device.findObject(selector) ?: device.findObject(textSelector)
    node.click()
    device.waitForIdle()
}

private fun viewerLabel(resource: String): String {
    val context = InstrumentationRegistry.getInstrumentation().context.createPackageContext(profileCaptureAppId(), 0)
    val id = context.resources.getIdentifier(resource, "string", context.packageName)
    check(id != 0) { "Unknown viewer string: $resource" }
    return context.getString(id)
}
