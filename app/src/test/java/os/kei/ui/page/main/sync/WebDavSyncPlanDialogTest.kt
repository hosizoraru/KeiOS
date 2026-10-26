package os.kei.ui.page.main.sync

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.theme.*
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-w360dp-h800dp-xxhdpi")
class WebDavSyncPlanDialogTest {
    @get:Rule val composeRule = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun restoreExplainsRemovalAndKeepsConfirmationVisibleAtLargeFont() {
        var confirmed = 0
        show(WebDavBatchKind.Download, WebDavSyncPlanRemoteState.Found(0, 100, "etag", "hash"),
            WebDavSyncPlanEffect.DownloadReplace) { confirmed++ }
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_plan_download_summary)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_plan_effect_download_replace)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_plan_confirm_download)).assertIsEnabled().performClick()
        assertEquals(1, confirmed)
        val image = composeRule.onNodeWithTag(ROOT).captureToImage().asAndroidBitmap()
        File("build/reports/webdav-restore-plan.png").apply { parentFile?.mkdirs() }.outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun unrecognizedBackupCanBeReviewedForAnExplicitRepairUpload() {
        var confirmed = 0
        show(WebDavBatchKind.Upload, WebDavSyncPlanRemoteState.Found(-1, 30, null, "invalid"),
            WebDavSyncPlanEffect.UploadOverwrite) { confirmed++ }
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_plan_remote_unknown_count), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_plan_confirm_upload)).assertIsEnabled().performClick()
        assertEquals(1, confirmed)
    }

    @Test fun aRateLimitedPreviewCannotConfirmAnOverwrite() {
        show(WebDavBatchKind.Upload, WebDavSyncPlanRemoteState.Error(WebDavItemStatus.RateLimited, null),
            WebDavSyncPlanEffect.Error) {}
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_plan_confirm_upload)).assertIsNotEnabled()
    }

    @Test fun invalidNutstoreDirectoryRemainsVisibleWhenTheServerUrlIsLocked() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                    WebDavRemoteWorkspaceCard(
                        state = WebDavSyncUiState(provider = WebDavProvider.Jianguoyun,
                            remoteDir = "../invalid", urlError = WebDavUrlError.Invalid),
                        cardColor = MiuixTheme.colorScheme.surfaceContainer,
                        onUpdateServerUrl = {}, onUpdateRemoteDir = {}, onOpenJianguoyunHelp = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.webdav_sync_url_error_invalid)).assertIsDisplayed()
    }

    private fun show(
        kind: WebDavBatchKind, remote: WebDavSyncPlanRemoteState, effect: WebDavSyncPlanEffect, onConfirm: () -> Unit,
    ) {
        val plan = WebDavSyncPlan(kind, WebDavSyncPlanScope.Batch, 1_000, listOf(
            WebDavSyncPlanItem(WebDavSyncItem.BaAccounts, 2, "local", remote, effect),
        ))
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.5f),
                LocalTransitionAnimationsEnabled provides false,
            ) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(Modifier.width(360.dp).background(MiuixTheme.colorScheme.background).padding(16.dp).testTag(ROOT)) {
                        WebDavSyncPlanDialog(plan, onDismiss = {}, onConfirm = onConfirm)
                    }
                }
            }
        }
    }
    companion object { private const val ROOT = "webdav-plan-preview" }
}
