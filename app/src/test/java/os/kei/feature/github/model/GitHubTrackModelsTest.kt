package os.kei.feature.github.model

import os.kei.BuildConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubTrackModelsTest {
    @Test
    fun `default kei os tracked app points at current app package and repo`() {
        val item = defaultKeiOsTrackedApp()

        assertEquals("https://github.com/hosizoraru/KeiOS", item.repoUrl)
        assertEquals("hosizoraru", item.owner)
        assertEquals("KeiOS", item.repo)
        assertEquals(BuildConfig.APPLICATION_ID, item.packageName)
        assertEquals("KeiOS", item.appLabel)
    }

    @Test
    fun `kei os self track badge matches current app package and repo`() {
        val item = defaultKeiOsTrackedApp()

        assertTrue(item.isKeiOsSelfTrack())
    }
}
