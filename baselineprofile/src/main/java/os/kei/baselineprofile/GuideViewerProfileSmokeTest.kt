package os.kei.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.runner.RunWith

/** An explicit warm/arrival check; the default generator class filter does not execute this test. */
@RunWith(AndroidJUnit4::class)
class GuideViewerProfileSmokeTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun preparePermissions() {
            prepareProfileCapturePermissions()
            ProfileCaptureInterruptions.begin()
        }

        @AfterClass
        @JvmStatic
        fun restoreInterruptions() = ProfileCaptureInterruptions.restore()
    }

    @Test
    fun compatibleViewersReachTheRealControls() {
        val scope = MacrobenchmarkScope(profileCaptureAppId(), false)
        val originalSize = scope.readWindowSizeOverride()
        try {
            scope.forceWindowSizeDp(widthDp = 1_000, heightDp = 800)
            scope.launchHomeFromColdStart()
            scope.navigateToMainPage(MAIN_BOTTOM_TAB_BA, BA_PAGE_ROOT, MAIN_PAGER_SETTLED_BA)
            scope.exerciseGuideViewersAndReturn()
            scope.forceWindowSizeDp(widthDp = 500, heightDp = 800)
            scope.launchHomeFromColdStart()
            scope.navigateToMainPage(MAIN_BOTTOM_TAB_BA, BA_PAGE_ROOT, MAIN_PAGER_SETTLED_BA)
            scope.exerciseBaCatalogAndReturn(wide = false)
        } catch (failure: Throwable) {
            val output = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .context.getExternalFilesDir(null)!!
            scope.device.dumpWindowHierarchy(java.io.File(output, "viewer-smoke-failure.xml"))
            scope.device.takeScreenshot(java.io.File(output, "viewer-smoke-failure.png"))
            throw failure
        } finally {
            scope.restoreWindowSize(originalSize)
        }
    }
}
