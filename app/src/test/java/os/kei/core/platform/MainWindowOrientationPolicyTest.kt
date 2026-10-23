package os.kei.core.platform

import android.content.pm.ActivityInfo
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainWindowOrientationPolicyTest {
    @Test
    fun phoneLaunchRetainsManifestPortraitPreference() {
        assertNull(
            MainWindowOrientationPolicy.requestFor(
                smallestScreenWidthDp = 393,
                currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            ),
        )
    }

    @Test
    fun tabletLaunchReleasesManifestPortraitPreference() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
            MainWindowOrientationPolicy.requestFor(
                smallestScreenWidthDp = 800,
                currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            ),
        )
    }

    @Test
    fun foldUnfoldAndWindowResizeRecomputeWithoutRecreation() {
        var current = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        val requests = mutableListOf<Int>()
        // Repeated configurations model rotations and unrelated changes within the same size class.
        for (smallestWidth in listOf(393, 673, 673, 500, 500, 800, 393)) {
            MainWindowOrientationPolicy.requestFor(smallestWidth, current)?.let {
                requests += it
                current = it
            }
        }
        assertEquals(
            listOf(
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            ),
            requests,
        )
    }

    @Test
    fun smallestWidthBoundaryDoesNotReleaseNarrowPhoneWindows() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            MainWindowOrientationPolicy.requestFor(599, ActivityInfo.SCREEN_ORIENTATION_FULL_USER),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
            MainWindowOrientationPolicy.requestFor(600, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT),
        )
    }

    @Test
    fun unchangedLargeWindowDoesNotRepeatOrientationRequest() {
        assertNull(
            MainWindowOrientationPolicy.requestFor(800, ActivityInfo.SCREEN_ORIENTATION_FULL_USER),
        )
    }

    @Test
    fun undefinedSizeRetainsCurrentPreference() {
        assertNull(
            MainWindowOrientationPolicy.requestFor(0, ActivityInfo.SCREEN_ORIENTATION_FULL_USER),
        )
    }
}
