package os.kei.feature.github.notification

import android.app.Application
import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import os.kei.MainActivity
import os.kei.feature.github.notification.GitHubShareImportNotificationPhase as Phase
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.ui.page.main.github.share.GitHubShareImportActivity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * What the app decides about its share-import and page-install notifications: which two buttons each
 * phase offers and where they go, what the text says, and what the island shows. How a payload becomes
 * a focus template (colours, template names, action mirroring) is core-notification's
 * `MiIslandNotificationBuilderTest`.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    application = Application::class,
    sdk = [35]
)
class GitHubShareImportNotificationHelperTest {
    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `every phase offers its own two actions`() {
        data class Case(
            val name: String,
            val state: GitHubShareImportNotificationState,
            val primary: String,
            // null: the phase has no second action at all.
            val secondary: String?,
            val secondaryAction: ((Context) -> String)?,
            val ongoing: Boolean,
            val text: String? = null,
        )
        val receiver = GitHubShareImportActionReceiver.Companion
        val refresh = receiver::actionRefreshShareImport
        val cancelLinkage = receiver::actionCancelShareImport
        val confirmTrack = receiver::actionConfirmShareImport
        val sendInstall = receiver::actionSendInstallShareImport
        val confirmPageInstall = receiver::actionConfirmPageInstall
        val cancelPageInstall = receiver::actionCancelPageInstall
        val markRead = receiver::actionMarkReadShareImport
        val waitingRows =
            // The text stays the same whether or not the pending track has a scanned package name.
            listOf("", "demo.app").map { packageName ->
                Case(
                    name = "waiting for install, packageName=\"$packageName\"",
                    state = GitHubShareImportNotificationState(
                        phase = Phase.WaitingInstall,
                        owner = "owner",
                        repo = "repo",
                        assetName = "app-arm64.apk",
                        packageName = packageName,
                        count = 12,
                    ),
                    primary = "Open flow",
                    secondary = "Refresh",
                    secondaryAction = refresh,
                    ongoing = true,
                    text = "repo · 12 min",
                )
            }
        val terminalRows =
            listOf(
                Triple("added", GitHubShareImportNotificationState(phase = Phase.Added, owner = "owner", repo = "repo", appLabel = "Demo"), "View tracking"),
                Triple("already tracked", GitHubShareImportNotificationState(phase = Phase.AlreadyTracked, owner = "owner", repo = "repo", appLabel = "Demo"), "View tracking"),
                Triple("cancelled", GitHubShareImportNotificationState(phase = Phase.Cancelled), "View GitHub"),
                Triple("failed", GitHubShareImportNotificationState(phase = Phase.Failed, primaryLabel = "Network timeout"), "View GitHub"),
                Triple("page install completed", GitHubShareImportNotificationState(phase = Phase.PageInstallCompleted, owner = "owner", repo = "repo", appLabel = "Demo"), "View GitHub"),
                Triple("page install failed", GitHubShareImportNotificationState(phase = Phase.PageInstallFailed, owner = "owner", repo = "repo", appLabel = "Demo"), "View GitHub"),
                Triple("page install cancelled", GitHubShareImportNotificationState(phase = Phase.PageInstallCancelled, owner = "owner", repo = "repo", appLabel = "Demo"), "View GitHub"),
            ).map { (name, state, primary) ->
                Case(
                    name = name,
                    state = state,
                    primary = primary,
                    secondary = "Mark read",
                    secondaryAction = markRead,
                    ongoing = false,
                    text =
                        when (name) {
                            "added" -> "Demo was added to owner/repo tracking"
                            "failed" -> "Network timeout"
                            "page install completed" -> "Demo · owner/repo"
                            else -> null
                        },
                )
            }
        val rows =
            waitingRows + listOf(
                Case(
                    name = "resolving has nothing to offer beside the flow",
                    state = GitHubShareImportNotificationState(
                        phase = Phase.Resolving,
                        primaryLabel = "https://github.com/owner/repo/releases",
                    ),
                    primary = "Open flow",
                    secondary = null,
                    secondaryAction = null,
                    ongoing = true,
                ),
                Case(
                    name = "several APKs: choose in the flow, or cancel",
                    state = GitHubShareImportNotificationState(
                        phase = Phase.AssetReady,
                        owner = "owner",
                        repo = "repo",
                        releaseTag = "v1.2.3",
                        count = 2,
                    ),
                    primary = "Open flow",
                    secondary = "Cancel linkage",
                    secondaryAction = cancelLinkage,
                    ongoing = true,
                ),
                Case(
                    name = "one APK: send it to the installer from the notification",
                    state = GitHubShareImportNotificationState(
                        phase = Phase.AssetReady,
                        owner = "owner",
                        repo = "repo",
                        releaseTag = "v1.2.3",
                        count = 1,
                        sendInstallActionEnabled = true,
                    ),
                    primary = "Open flow",
                    secondary = "Send install",
                    secondaryAction = sendInstall,
                    ongoing = true,
                ),
                Case(
                    name = "install detected: confirm the track",
                    state = GitHubShareImportNotificationState(
                        phase = Phase.InstallDetected,
                        owner = "owner",
                        repo = "repo",
                        appLabel = "Demo",
                        packageName = "demo.app",
                        versionName = "2.0.0",
                    ),
                    primary = "Open flow",
                    secondary = "Confirm tracking",
                    secondaryAction = confirmTrack,
                    ongoing = true,
                    text = "Demo · repo · 2.0.0",
                ),
                Case(
                    name = "a staged managed install waits for an explicit continue",
                    state = GitHubShareImportNotificationState(
                        phase = Phase.InstallReady,
                        owner = "owner",
                        repo = "repo",
                        assetName = "demo.apk",
                        packageName = "demo.app",
                    ),
                    primary = "Open flow",
                    secondary = "Continue install",
                    secondaryAction = sendInstall,
                    ongoing = true,
                    text = "repo",
                ),
                Case(
                    name = "a page install in progress can be watched or cancelled",
                    state = GitHubShareImportNotificationState(
                        source = GitHubShareImportNotificationSource.PageInstall,
                        phase = Phase.InstallDownloading,
                        owner = "T8RIN",
                        repo = "ImageToolbox",
                        releaseTag = "4.2.0-alpha01",
                        progressPercentOverride = 40,
                        downloadedBytes = 40L,
                        totalBytes = 100L,
                    ),
                    primary = "View progress",
                    secondary = "Cancel install",
                    secondaryAction = cancelPageInstall,
                    ongoing = true,
                ),
                Case(
                    name = "a page install confirm with its manifest read",
                    state = pageInstallConfirm(confirmActionEnabled = true),
                    primary = "Review install",
                    secondary = "Confirm install",
                    secondaryAction = confirmPageInstall,
                    ongoing = true,
                    text = "Demo · owner/repo · waiting",
                ),
                Case(
                    name = "a page install confirm still waiting for its manifest",
                    state = pageInstallConfirm(confirmActionEnabled = false),
                    primary = "Review install",
                    secondary = null,
                    secondaryAction = null,
                    ongoing = true,
                ),
            ) + terminalRows

        rows.forEach { case ->
            val notification = buildModern(case.state)

            val expectedCategory =
                if (case.ongoing) Notification.CATEGORY_PROGRESS else Notification.CATEGORY_STATUS
            assertEquals(expectedCategory, notification.category, case.name)
            assertEquals(McpNotificationHelper.LIVE_CHANNEL_ID, notification.channelId, case.name)
            assertEquals(case.ongoing, notification.flags and Notification.FLAG_ONGOING_EVENT != 0, case.name)
            if (!case.ongoing) {
                assertNotNull(notification.deleteIntent, case.name)
                assertFalse(notification.extras.getBoolean("android.requestPromotedOngoing"), case.name)
            }
            assertEquals(case.primary, notification.actions[0].title.toString(), case.name)
            if (case.secondary == null) {
                assertEquals(1, notification.actions.size, case.name)
            } else {
                assertEquals(2, notification.actions.size, case.name)
                assertEquals(case.secondary, notification.actions[1].title.toString(), case.name)
                assertReceiverAction(notification.actions[1], checkNotNull(case.secondaryAction)(context), case.name)
            }
            if (case.text != null) {
                assertEquals(
                    case.text,
                    notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
                    case.name,
                )
            }
        }
    }

    @Test
    fun `the open action resumes the share flow, and a page install opens the GitHub page`() {
        val shareFlow = shadowOf(
            buildModern(
                GitHubShareImportNotificationState(
                    phase = Phase.AssetReady,
                    owner = "owner",
                    repo = "repo",
                    count = 1,
                ),
            ).actions[0].actionIntent,
        ).savedIntent
        val pageInstall = shadowOf(
            buildModern(
                GitHubShareImportNotificationState(
                    source = GitHubShareImportNotificationSource.PageInstall,
                    phase = Phase.InstallDownloading,
                    owner = "owner",
                    repo = "repo",
                ),
            ).actions[0].actionIntent,
        ).savedIntent

        assertEquals(GitHubShareImportActivity::class.java.name, shareFlow.component?.className)
        assertEquals(GitHubShareImportActivity.ACTION_RESUME_SHARE_IMPORT, shareFlow.action)
        assertTrue(shareFlow.getBooleanExtra(GitHubShareImportActivity.EXTRA_FORCE_SHEET, false))
        assertEquals(MainActivity::class.java.name, pageInstall.component?.className)
        assertEquals(
            MainActivity.TARGET_BOTTOM_PAGE_GITHUB,
            pageInstall.getStringExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE),
        )
    }

    @Test
    fun `the island names the app, never its package, under the share flow's own order id`() {
        val focusParam = buildMiIsland(
            GitHubShareImportNotificationState(
                phase = Phase.WaitingInstall,
                owner = "owner",
                repo = "repo",
                assetName = "app-arm64.apk",
                packageName = "demo.app",
                count = 12,
            ),
        ).focusParam()

        assertTrue(focusParam.contains("\"orderId\":\"github_share_import\""))
        assertFalse(focusParam.contains("demo.app"))
        assertFalse(focusParam.contains("progressTextInfo"))
    }

    @Test
    fun `a download shows its percentage only while the total is known`() {
        fun downloading(totalBytes: Long, percent: Int) =
            GitHubShareImportNotificationState(
                phase = Phase.InstallDownloading,
                owner = "owner",
                repo = "repo",
                releaseTag = "v1.2.3",
                assetName = "demo.apk",
                targetDisplayName = "Demo",
                progressPercentOverride = percent,
                downloadedBytes = 5_120L,
                totalBytes = totalBytes,
            )

        val known = buildMiIsland(downloading(totalBytes = 10_240L, percent = 48)).focusParam()
        val unknown = buildMiIsland(downloading(totalBytes = -1L, percent = 0)).focusParam()

        assertTrue(
            buildModern(downloading(totalBytes = 10_240L, percent = 48))
                .extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                .contains("Demo · v1.2.3"),
        )
        assertTrue(known.contains("\"title\":\"48%\""))
        assertTrue(known.contains("\"progress\":48"))
        assertTrue(known.contains("progressInfo"))
        // The version badge carries the tag, so the content must not repeat it.
        assertEquals(1, known.split("v1.2.3").size - 1)
        assertFalse(unknown.contains("progressInfo"))
        assertTrue(unknown.contains("\"title\":\"Download\""))
        assertTrue(unknown.contains("downloaded"))
    }

    @Test
    fun `the version badge prefers the manifest, falls back to the tag, and shortens only a long version`() {
        data class Case(
            val name: String,
            val state: GitHubShareImportNotificationState,
            val badge: String,
            val content: String,
            val modernText: String? = null,
            val absent: String? = null,
        )
        listOf(
            Case(
                name = "no manifest yet: the release tag",
                state = GitHubShareImportNotificationState(
                    phase = Phase.Installing,
                    owner = "owner",
                    repo = "repo",
                    releaseTag = "v1.2.3",
                    assetName = "demo.apk",
                    progressPercentOverride = 36,
                ),
                badge = "v1.2.3",
                content = "repo",
                modernText = "repo · v1.2.3",
            ),
            Case(
                name = "the manifest's version name wins over the tag",
                state = GitHubShareImportNotificationState(
                    phase = Phase.Installing,
                    owner = "owner",
                    repo = "repo",
                    releaseTag = "v1.2.3",
                    assetName = "demo.apk",
                    targetDisplayName = "Demo",
                    versionName = "2.0.0",
                    progressPercentOverride = 36,
                ),
                badge = "2.0.0",
                content = "Demo",
                modernText = "Demo · 2.0.0",
                absent = "v1.2.3",
            ),
            Case(
                name = "a short version sits in the badge and the content names the app",
                state = GitHubShareImportNotificationState(
                    phase = Phase.PageInstallCompleted,
                    owner = "owner",
                    repo = "repo",
                    appLabel = "Demo",
                    packageName = "dev.demo.app",
                    versionName = "2.0.0",
                ),
                badge = "2.0.0",
                content = "Demo",
            ),
            Case(
                name = "a long version keeps its semantic core in the badge and all of it in the content",
                state = GitHubShareImportNotificationState(
                    phase = Phase.PageInstallCompleted,
                    owner = "owner",
                    repo = "repo",
                    appLabel = "PiliPlus",
                    packageName = "com.example.piliplus",
                    versionName = "2.1.0-c1a5e8d5b1d9f04",
                ),
                badge = "2.1.0",
                content = "2.1.0-c1a5e8d5b1d9f04",
            ),
        ).forEach { case ->
            val focusParam = buildMiIsland(case.state).focusParam()
            val baseInfo = JSONObject(focusParam).getJSONObject("param_v2").getJSONObject("baseInfo")

            assertEquals(case.badge, baseInfo.getString("specialTitle"), case.name)
            assertEquals(case.content, baseInfo.getString("content"), case.name)
            if (case.name.startsWith("no manifest")) {
                // The badge carries the tag, so the content must not repeat it.
                assertEquals(1, focusParam.split(case.badge).size - 1, case.name)
            }
            case.absent?.let { assertFalse(focusParam.contains(it), case.name) }
            case.modernText?.let {
                assertEquals(
                    it,
                    buildModern(case.state).extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
                    case.name,
                )
            }
        }
    }

    @Test
    fun `page install island content fits the island without an ellipsis`() {
        val focusParam = buildMiIsland(
            GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = Phase.InstallDownloading,
                owner = "T8RIN",
                repo = "ImageToolbox",
                releaseTag = "4.2.0-alpha01",
                targetDisplayName = "Image Toolbox With A Very Long Display Name",
                progressPercentOverride = 99,
                downloadedBytes = 98_160_000L,
                totalBytes = 98_560_000L,
            ),
        ).focusParam()
        val content =
            JSONObject(focusParam).getJSONObject("param_v2").getJSONObject("baseInfo").getString("content")

        assertTrue(content.length <= 28, content)
        assertFalse(content.contains("..."), content)
        assertTrue(focusParam.contains("\"orderId\":\"github_page_install\""))
    }

    @Test
    fun `a finished flow floats once and is not promoted as ongoing`() {
        listOf(
            GitHubShareImportNotificationState(phase = Phase.Added, owner = "owner", repo = "repo", appLabel = "Demo"),
            GitHubShareImportNotificationState(phase = Phase.AlreadyTracked, owner = "owner", repo = "repo", appLabel = "Demo"),
            GitHubShareImportNotificationState(phase = Phase.Failed, primaryLabel = "Network timeout"),
            GitHubShareImportNotificationState(phase = Phase.Cancelled),
            GitHubShareImportNotificationState(phase = Phase.PageInstallCompleted, owner = "owner", repo = "repo", appLabel = "Demo"),
            GitHubShareImportNotificationState(phase = Phase.PageInstallFailed, owner = "owner", repo = "repo", appLabel = "Demo"),
            GitHubShareImportNotificationState(phase = Phase.PageInstallCancelled, owner = "owner", repo = "repo", appLabel = "Demo"),
        ).forEach { state ->
            val notification = buildMiIsland(state)
            val focusParam = notification.focusParam()
            val name = state.phase.name

            assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT == 0, name)
            assertFalse(notification.extras.getBoolean("android.requestPromotedOngoing"), name)
            assertTrue(JSONObject(focusParam).getJSONObject("param_v2").getBoolean("enableFloat"), name)
            assertEquals("Mark read", notification.actions[1].title.toString(), name)
            assertFalse(focusParam.contains("progressTextInfo"), name)
        }
    }

    private fun pageInstallConfirm(confirmActionEnabled: Boolean) =
        GitHubShareImportNotificationState(
            source = GitHubShareImportNotificationSource.PageInstall,
            phase = Phase.PageInstallConfirm,
            owner = "owner",
            repo = "repo",
            appLabel = "Demo",
            packageName = "dev.demo.app",
            versionName = "2.0.0",
            pageInstallConfirmActionEnabled = confirmActionEnabled,
        )

    private fun buildModern(state: GitHubShareImportNotificationState): Notification =
        GitHubShareImportNotificationHelper.buildFrameworkLiveUpdateNotification(context, state)

    private fun buildMiIsland(state: GitHubShareImportNotificationState): Notification =
        GitHubShareImportNotificationHelper.buildFrameworkMiIslandNotification(context, state)

    private fun Notification.focusParam(): String = extras.getString("miui.focus.param").orEmpty()

    private fun assertReceiverAction(
        action: Notification.Action,
        expectedAction: String,
        case: String,
    ) {
        val intent = shadowOf(action.actionIntent).savedIntent
        assertEquals(GitHubShareImportActionReceiver::class.java.name, intent.component?.className, case)
        assertEquals(expectedAction, intent.action, case)
        assertTrue(intent.flags and Intent.FLAG_RECEIVER_FOREGROUND != 0, case)
    }
}
