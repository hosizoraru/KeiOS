package os.kei.appwidget.github

import android.app.Application
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GitHubWidgetStateStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun store() = GitHubWidgetStateStore(temporary.root)

    @Test
    fun `fresh reader recovers the published snapshot without in memory state`() {
        val snapshot = GitHubWidgetSnapshot(
            available = true, trackedCount = 4, updateCount = 2,
            lastCheckedAtMillis = 123456L, pendingUpdates = listOf(GitHubWidgetPendingUpdate("One", "v2.0"), GitHubWidgetPendingUpdate("Two", "v3.0-rc1", true)),
            projects = listOf(
                GitHubWidgetProject("One", GitHubWidgetProjectStatus.Update, "v2.0", localVersion = "1.0"),
                GitHubWidgetProject("Two", GitHubWidgetProjectStatus.Update, "v3.0-rc1", true),
                GitHubWidgetProject("Three", GitHubWidgetProjectStatus.NoUpdate),
                GitHubWidgetProject("Four", GitHubWidgetProjectStatus.Unchecked),
            ),
        )
        store().writeSnapshot(snapshot)
        assertEquals(snapshot, store().readSnapshot())
        store().writeSnapshot(GitHubWidgetSnapshot(available = true))
        assertEquals(emptyList(), store().readSnapshot().pendingNames)
    }

    @Test
    fun `previous names only snapshots remain readable without inventing versions`() {
        File(temporary.root, "github_snapshot.json").writeText(
            """{"schema":1,"available":true,"tracked":4,"updates":2,"names":["One","Two"]}""",
        )
        assertEquals(
            listOf(GitHubWidgetPendingUpdate("One"), GitHubWidgetPendingUpdate("Two")),
            store().readSnapshot().pendingUpdates,
        )
    }

    @Test
    fun `persisted statuses use stable ids and accept previous enum names`() {
        File(temporary.root, "github_snapshot.json").writeText(
            """{"schema":1,"available":true,"projects":[{"name":"Old","status":"NoUpdate"},{"name":"New","status":"update","localVersion":"1.0"},{"name":"Unknown","status":"future"}]}""",
        )
        val snapshot = store().readSnapshot()
        assertEquals(
            listOf(GitHubWidgetProjectStatus.NoUpdate, GitHubWidgetProjectStatus.Update, GitHubWidgetProjectStatus.Unchecked),
            snapshot.projects.map { it.status },
        )
        assertEquals("1.0", snapshot.projects[1].localVersion)
        store().writeSnapshot(snapshot)
        assertTrue(File(temporary.root, "github_snapshot.json").readText().contains("\"status\":\"current\""))
        assertEquals(snapshot, store().readSnapshot())
    }

    @Test
    fun `name privacy defaults off and is isolated by widget id`() {
        assertFalse(store().configuration(10).showProjectNames)
        store().configure(10, GitHubWidgetConfiguration(true))
        store().configure(11, GitHubWidgetConfiguration(false))
        assertTrue(store().configuration(10).showProjectNames)
        assertFalse(store().configuration(11).showProjectNames)
        store().delete(intArrayOf(10))
        assertFalse(store().configuration(10).showProjectNames)
        assertFalse(store().configuration(11).showProjectNames)
    }

    @Test
    fun `restoration maps ids simultaneously and repeated delivery preserves new configuration`() {
        store().configure(10, GitHubWidgetConfiguration(true))
        store().configure(11, GitHubWidgetConfiguration(false))
        store().restore(intArrayOf(10, 11), intArrayOf(11, 12))
        assertFalse(store().configuration(10).showProjectNames)
        assertTrue(store().configuration(11).showProjectNames)
        assertFalse(store().configuration(12).showProjectNames)
        store().configure(12, GitHubWidgetConfiguration(true))
        store().restore(intArrayOf(10, 11), intArrayOf(11, 12))
        assertTrue(store().configuration(11).showProjectNames)
        assertTrue(store().configuration(12).showProjectNames)
        store().restore(intArrayOf(10), intArrayOf(11))
        assertTrue(store().configuration(11).showProjectNames)
    }

    @Test
    fun `pin preferences survive a process restart and apply only to the new instance`() {
        store().configure(10, GitHubWidgetConfiguration(false))
        val token = store().beginPinRequest(GitHubWidgetConfiguration(true), intArrayOf(10), 1000L)
        assertFalse(store().completePinRequest(token, 10, 2000L))
        assertFalse(store().configuration(10).showProjectNames)
        assertEquals(null, store().pinResult(token))
        assertTrue(store().completePinRequest(token, 11, 2000L))
        assertTrue(store().configuration(11).showProjectNames)
        assertEquals(11, store().pinResult(token))
        store().configure(11, GitHubWidgetConfiguration(false))
        assertFalse(store().completePinRequest(token, 12, 3000L))
        assertFalse(store().configuration(11).showProjectNames)
        assertFalse(store().configuration(12).showProjectNames)
    }

    @Test
    fun `expired cancelled and unknown pin callbacks cannot change privacy`() {
        val expired = store().beginPinRequest(GitHubWidgetConfiguration(true), intArrayOf(), 1000L)
        assertFalse(store().completePinRequest(expired, 11, 1_000_000L))
        val cancelled = store().beginPinRequest(GitHubWidgetConfiguration(true), intArrayOf(), 1_000_000L)
        store().abandonPinRequest(cancelled)
        assertFalse(store().completePinRequest(cancelled, 11, 1_001_000L))
        assertFalse(store().completePinRequest("unknown", 11, 1_001_000L))
        assertFalse(store().configuration(11).showProjectNames)
    }

    @Test
    fun `unconfigured restored instances never inherit a stale destination privacy setting`() {
        store().configure(20, GitHubWidgetConfiguration(true))
        store().restore(intArrayOf(10), intArrayOf(20))
        assertFalse(store().configuration(20).showProjectNames)
    }

    @Test
    fun `malformed data and unsupported schema fall back without revealing names`() {
        File(temporary.root, "github_snapshot.json").writeText("{invalid")
        File(temporary.root, "github_instances.json").writeText("{invalid")
        assertEquals(GitHubWidgetSnapshot(), store().readSnapshot())
        assertFalse(store().configuration(10).showProjectNames)
        File(temporary.root, "github_snapshot.json").writeText("""{"schema":99,"available":true,"updates":9}""")
        assertFalse(store().readSnapshot().available)
    }

    @Test
    fun `atomic backup is recovered under the same lock used by writers`() {
        store().configure(10, GitHubWidgetConfiguration(true))
        File(temporary.root, "github_instances.json").copyTo(File(temporary.root, "github_instances.json.bak"))
        File(temporary.root, "github_instances.json").writeText("{partial")
        assertTrue(store().configuration(10).showProjectNames)
    }

    @Test
    fun `concurrent instances preserve every independent configuration`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit { (1..20).forEach { store().configure(it, GitHubWidgetConfiguration(true)) } }
            val second = executor.submit { (21..40).forEach { store().configure(it, GitHubWidgetConfiguration(true)) } }
            first.get(15, TimeUnit.SECONDS)
            second.get(15, TimeUnit.SECONDS)
            (1..40).forEach { assertTrue(store().configuration(it).showProjectNames, "Lost widget $it") }
        } finally {
            executor.shutdownNow()
        }
    }
}
