package os.kei.core.download

import org.junit.Test
import kotlin.test.assertEquals

class AppPrivateDownloadManagerTest {
    @Test
    fun sanitizeDownloadFileNameDropsPathSegments() {
        assertEquals(
            "KeiOS-arm64-release.apk",
            AppPrivateDownloadManager.sanitizeDownloadFileName("/sdcard/Download/KeiOS-arm64-release.apk")
        )
        assertEquals(
            "artifact.zip",
            AppPrivateDownloadManager.sanitizeDownloadFileName("""C:\temp\artifact.zip""")
        )
    }

    @Test
    fun sanitizeDownloadFileNameReplacesUnsafeCharacters() {
        assertEquals(
            "demo_release_arm64.apk",
            AppPrivateDownloadManager.sanitizeDownloadFileName("""demo:release*arm64?.apk""")
        )
    }

    @Test
    fun sanitizeDownloadFileNameFallsBackWhenBlank() {
        assertEquals(
            "download.bin",
            AppPrivateDownloadManager.sanitizeDownloadFileName("...")
        )
    }
}
