@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import os.kei.ui.page.main.widget.glass.AppLiquidWindowBoundary
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.ProvideDisplayCutoutFix
import top.yukonga.miuix.kmp.utils.RemovePlatformDialogDefaultEffects
import top.yukonga.miuix.kmp.utils.WindowNavigationEventScope
import top.yukonga.miuix.kmp.utils.platformDialogProperties

enum class AppWindowDialogPresentation {
    Card,
    Fullscreen,
}

@Composable
fun AppWindowDialogHost(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    dismissible: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    maxWidth: Dp = DialogDefaults.MaxWidth,
    presentation: AppWindowDialogPresentation = AppWindowDialogPresentation.Card,
    content: @Composable () -> Unit,
) {
    if (presentation == AppWindowDialogPresentation.Card) {
        // Always the Liquid card. The miuix fallback that used to sit behind a preference is gone:
        // it was hosted in a Dialog window, where LocalSceneBackdrop is blanked and a blur draws
        // nothing, so it could never be more than a flat card.
        LiquidAlert(
            show = show,
            title = title,
            modifier = modifier,
            message = summary,
            dismissible = dismissible,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            maxWidth = maxWidth,
            content = content,
        )
        return
    }

    var rawDialogWasShown by remember { mutableStateOf(false) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    LaunchedEffect(show) {
        if (show) {
            rawDialogWasShown = true
        } else if (rawDialogWasShown) {
            rawDialogWasShown = false
            currentOnDismissFinished?.invoke()
        }
    }
    if (!show) return

    AppLiquidWindowBoundary {
        Dialog(
            onDismissRequest = {
                if (dismissible) {
                    currentOnDismissRequest?.invoke()
                }
            },
            properties = platformDialogProperties(),
        ) {
            // Theme locals cross into this window, but the host theme's cutout effect does not.
            ProvideDisplayCutoutFix()
            RemovePlatformDialogDefaultEffects()
            // A navigation-event owner provided in the host composition (miuix-nav's
            // entry dispatcher) is inherited across the platform-window boundary and
            // never receives this window's back events; re-resolve the dialog's own.
            WindowNavigationEventScope {
                BackHandler(enabled = dismissible) {
                    currentOnDismissRequest?.invoke()
                }
                content()
            }
        }
    }
}
