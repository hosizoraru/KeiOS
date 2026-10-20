package os.kei.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until

/**
 * What the profile generator and the frame benchmarks both need to find their way around the app.
 *
 * Only the shared vocabulary lives here. Gestures stay with their callers on purpose: the generator swipes
 * to reach code, the benchmarks swipe and then hold still so a measured window covers the motion, and the
 * two sets of timings are not interchangeable.
 */

/** The app under test, passed by the Gradle task; a hand-run `am instrument` must add `-e targetAppId`. */
internal fun targetAppId(): String =
    InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("targetAppId not passed as instrumentation runner arg")

/** Validate before BaselineProfileRule can reset compilation or stop its target. */
internal fun profileCaptureAppId(): String =
    targetAppId().also { appId ->
        check(appId == "os.kei.profilecapture") {
            "Profile collection requires the disposable os.kei.profilecapture package; got $appId"
        }
    }

/** The fixed viewer harness and instrumentation runner never ship in the release application. */
internal fun isReleaseProfileRule(rule: String): Boolean =
    "Los/kei/profilecapture/" !in rule && "Los/kei/baselineprofile/" !in rule

internal fun testTagSelector(tag: String): BySelector = By.res(tag)

internal fun MacrobenchmarkScope.waitForTestTag(
    tag: String,
    timeoutMs: Long = 5_000,
) {
    check(device.wait(Until.hasObject(testTagSelector(tag)), timeoutMs)) {
        "Timed out waiting for testTag=$tag in ${targetAppId()}"
    }
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.waitForOptionalTestTag(
    tag: String,
    timeoutMs: Long,
): Boolean {
    val found = device.wait(Until.hasObject(testTagSelector(tag)), timeoutMs)
    if (found) device.waitForIdle()
    return found
}

internal fun MacrobenchmarkScope.grantRuntimePermissions(packageName: String = targetAppId()) {
    listOf(
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.POST_PROMOTED_NOTIFICATIONS",
        "android.permission.ACCESS_LOCAL_NETWORK",
        "android.permission.USE_LOOPBACK_INTERFACE",
    ).forEach { permission ->
        device.executeShellCommand("pm grant $packageName $permission >/dev/null 2>&1 || true")
    }
}
