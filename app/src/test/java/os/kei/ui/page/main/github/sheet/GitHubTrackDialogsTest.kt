@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import android.app.Application
import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.page.GitHubTrackImportPreview
import os.kei.ui.page.main.widget.dialog.LiquidGlassDialog
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class GitHubTrackDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deleteExitKeepsTrackSummaryAndActionsUntilDismissalFinishes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item = trackedApp()
        val pendingItem = mutableStateOf<GitHubTrackedApp?>(item)
        var cancelCount = 0
        var confirmCount = 0
        val title = context.getString(R.string.github_delete_dialog_title)
        val summary =
            context.getString(
                R.string.github_delete_dialog_summary,
                item.appLabel,
                item.owner,
                item.repo,
            )
        val cancelLabel = context.getString(R.string.common_cancel)
        val deleteLabel = context.getString(R.string.common_delete)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides true) {
                    // The production composable, not a rebuilt harness: it is an action sheet now, and
                    // the exit-snapshot behaviour asserted below is a property of that composable rather
                    // than of a dialog wrapper the app no longer uses here.
                    GitHubDeleteTrackDialog(
                        pendingDeleteItem = pendingItem.value,
                        deleteInProgress = false,
                        onDismissRequest = {},
                        onCancel = { cancelCount++ },
                        onConfirmDelete = { confirmCount++ },
                    )
                }
            }
        }

        composeRule.onNode(hasText(title) and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeRule.onNode(hasText(cancelLabel) and hasClickAction()).performClick()
        composeRule.onNode(hasText(deleteLabel) and hasClickAction()).performClick()

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { pendingItem.value = null }
        composeRule.mainClock.advanceTimeBy(EXIT_OBSERVATION_MILLIS)

        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onNode(hasText(deleteLabel)).assertIsNotEnabled()
        composeRule.onNode(hasText(cancelLabel)).assertIsNotEnabled()
        // The snapshot now lives inside GitHubDeleteTrackDialog, so it is observed through what it
        // keeps on screen rather than by reaching for the object.

        finishExitAnimation()

        composeRule.onAllNodes(hasText(title)).assertCountEquals(0)
        // Retention is observed through the screen above; the snapshot is internal to the sheet now.
        assertEquals(1, cancelCount)
        assertEquals(1, confirmCount)
    }

    @Test
    fun importExitKeepsPreviewAndRoutesReadyActionsUntilDismissalFinishes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = readyImportPreview()
        val pendingPreview = mutableStateOf<GitHubTrackImportPreview?>(preview)
        var observedSnapshot: GitHubTrackDialogExitSnapshot<GitHubTrackImportPreview>? = null
        var cancelCount = 0
        var confirmCount = 0
        val title = context.getString(R.string.github_import_dialog_title)
        val summary = context.getString(R.string.github_import_dialog_summary_ready)
        val cancelLabel = context.getString(R.string.common_cancel)
        val confirmLabel = context.getString(R.string.github_import_dialog_action_confirm)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides true) {
                    val exitSnapshot = rememberGitHubTrackDialogExitSnapshot(pendingPreview.value)
                    val renderedPreview = exitSnapshot.resolve(pendingPreview.value)
                    SideEffect { observedSnapshot = exitSnapshot }
                    LiquidGlassDialog(
                        show = pendingPreview.value != null,
                        title = title,
                        summary = renderedPreview?.let { summary },
                        onDismissFinished = exitSnapshot::clear,
                    ) {
                        renderedPreview?.let {
                            GitHubTrackImportDialogActions(
                                canImport = it.canImport,
                                importInProgress = false,
                                actionsEnabled = pendingPreview.value != null,
                                onDismissRequest = {},
                                onCancel = { cancelCount++ },
                                onConfirmImport = { confirmCount++ },
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNode(hasText(title) and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onNode(hasText(cancelLabel) and hasClickAction()).performClick()
        composeRule.onNode(hasText(confirmLabel) and hasClickAction()).performClick()

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { pendingPreview.value = null }
        composeRule.mainClock.advanceTimeBy(EXIT_OBSERVATION_MILLIS)

        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onNode(hasText(confirmLabel)).assertIsNotEnabled()
        composeRule.onNode(hasText(cancelLabel)).assertIsNotEnabled()
        assertEquals(preview, assertNotNull(observedSnapshot).retainedValue)

        finishExitAnimation()

        composeRule.onAllNodes(hasText(title)).assertCountEquals(0)
        assertNull(assertNotNull(observedSnapshot).retainedValue)
        assertEquals(1, cancelCount)
        assertEquals(1, confirmCount)
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h760dp-xxhdpi")
    fun richImportPreviewKeepsActionsVisibleAndAllStatisticsReachable() {
        assertRichImportPreview()
    }

    @Test
    @Config(qualifiers = "ja-rJP-w320dp-h640dp-xxhdpi")
    fun richImportPreviewFitsJapaneseOnAShortNarrowScreen() {
        assertRichImportPreview()
    }

    private fun assertRichImportPreview() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview =
            readyImportPreview().copy(
                fileItemCount = 41,
                validCount = 41,
                newCount = 40,
                updatedCount = 1,
                mergedCount = 41,
                githubRepositoryCount = 39,
                directApkCount = 2,
                preferPreReleaseCount = 7,
                actionsUpdateCount = 12,
            )
        var confirms = 0
        val confirm = context.getString(R.string.github_import_dialog_action_confirm)
        val cancel = context.getString(R.string.common_cancel)
        val lastStatistic = context.getString(R.string.github_import_dialog_label_download_items)

        composeRule.setContent {
            val density = LocalDensity.current
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, 1.5f),
                    LocalTransitionAnimationsEnabled provides false,
                    LocalTextCopyExpandedOverride provides false,
                ) {
                    GitHubTrackImportDialog(
                        preview = preview,
                        importInProgress = false,
                        onDismissRequest = {},
                        onCancel = {},
                        onConfirmImport = { confirms++ },
                    )
                }
            }
        }

        composeRule.onNode(hasText(confirm) and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasText(cancel) and hasClickAction()).assertIsDisplayed()
        val height = with(composeRule.density) {
            composeRule.onNodeWithTag(GITHUB_IMPORT_STATISTICS_TEST_TAG)
                .fetchSemanticsNode().boundsInRoot.height.toDp()
        }
        assertTrue(height > 0.dp && height <= 360.dp, "Statistics viewport height: $height")
        composeRule.onNode(hasText(lastStatistic, substring = true)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(confirm) and hasClickAction()).assertIsDisplayed().performClick()
        assertEquals(1, confirms)
    }

    @Test
    fun importContentRoutesInvalidCloseAndDisablesBusyActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importInProgress = mutableStateOf(false)
        var cancelCount = 0
        var dismissCount = 0
        var confirmCount = 0
        val cancelLabel = context.getString(R.string.common_cancel)
        val closeLabel = context.getString(R.string.common_close)
        val importingLabel = context.getString(R.string.github_check_sheet_action_importing)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    LiquidGlassDialog(show = true, title = "Import actions") {
                        GitHubTrackImportDialogActions(
                            canImport = false,
                            importInProgress = importInProgress.value,
                            onDismissRequest = { dismissCount++ },
                            onCancel = { cancelCount++ },
                            onConfirmImport = { confirmCount++ },
                        )
                    }
                }
            }
        }

        composeRule.onNode(hasText(cancelLabel) and hasClickAction()).performClick()
        composeRule.onNode(hasText(closeLabel) and hasClickAction()).performClick()
        composeRule.runOnIdle { importInProgress.value = true }
        composeRule.onNode(hasText(importingLabel)).assertIsNotEnabled()
        composeRule.onNode(hasText(cancelLabel)).assertIsNotEnabled()

        assertEquals(1, cancelCount)
        assertEquals(1, dismissCount)
        assertEquals(0, confirmCount)
    }

    @Test
    fun deleteSheetIgnoresBackWhileTheDeleteIsInProgress() {
        assertDeleteSheetRefusesDismissalWhileDeleting { backDispatcher ->
            composeRule.runOnIdle { backDispatcher.onBackPressed() }
        }
    }

    @Test
    fun deleteSheetIgnoresAScrimTapWhileTheDeleteIsInProgress() {
        assertDeleteSheetRefusesDismissalWhileDeleting {
            composeRule.onRoot().performTouchInput { click(Offset(centerX, SCRIM_TAP_Y_PX)) }
        }
    }

    /**
     * A delete in flight cannot be walked away from: [dismissAttempt] must not reach
     * `onDismissRequest` while `deleteInProgress` is true, and must once it is false. The second half
     * is what makes the first meaningful: it proves the gesture reaches the sheet and the zero is the
     * sheet refusing.
     */
    private fun assertDeleteSheetRefusesDismissalWhileDeleting(
        dismissAttempt: (OnBackPressedDispatcher) -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deleteInProgress = mutableStateOf(true)
        var dismissCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher
        val title = context.getString(R.string.github_delete_dialog_title)
        val deletingLabel = context.getString(R.string.github_delete_dialog_deleting)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    val dispatcherOwner = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
                    SideEffect { backDispatcher = dispatcherOwner.onBackPressedDispatcher }
                    GitHubDeleteTrackDialog(
                        pendingDeleteItem = trackedApp(),
                        deleteInProgress = deleteInProgress.value,
                        onDismissRequest = { dismissCount++ },
                        onCancel = {},
                        onConfirmDelete = {},
                    )
                }
            }
        }

        composeRule.onNode(hasText(deletingLabel)).assertIsNotEnabled()
        dismissAttempt(backDispatcher)
        composeRule.waitForIdle()

        assertEquals(0, dismissCount, "a delete in flight must not be dismissible")
        composeRule.onNode(hasText(title) and isHeading()).assertIsDisplayed()

        composeRule.runOnIdle { deleteInProgress.value = false }
        composeRule.waitForIdle()
        dismissAttempt(backDispatcher)
        composeRule.waitForIdle()

        assertEquals(1, dismissCount, "once the delete has finished the same gesture dismisses")
    }

    private fun finishExitAnimation() {
        composeRule.mainClock.advanceTimeBy(EXIT_COMPLETION_MILLIS)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }
}

private fun trackedApp(): GitHubTrackedApp =
    GitHubTrackedApp(
        repoUrl = "https://github.com/kyant0/backdrop",
        owner = "kyant0",
        repo = "backdrop",
        packageName = "io.github.kyant0.backdrop",
        appLabel = "Backdrop",
    )

private fun readyImportPreview(): GitHubTrackImportPreview =
    GitHubTrackImportPreview(
        payload = GitHubTrackedItemsImportPayload(),
        fileItemCount = 4,
        validCount = 3,
        duplicateCount = 1,
        invalidCount = 0,
        newCount = 2,
        updatedCount = 1,
        unchangedCount = 0,
        mergedCount = 3,
    )

private const val EXIT_OBSERVATION_MILLIS = 16L
private const val EXIT_COMPLETION_MILLIS = 300L
private const val SCRIM_TAP_Y_PX = 48f
