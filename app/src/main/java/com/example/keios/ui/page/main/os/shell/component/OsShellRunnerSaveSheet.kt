package com.example.keios.ui.page.main.os.shell.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.keios.R
import com.example.keios.ui.page.main.os.appLucideCloseIcon
import com.example.keios.ui.page.main.os.appLucideConfirmIcon
import com.example.keios.ui.page.main.os.shell.ShellOutputDisplayEntry
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassSearchField
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.sheet.SheetContentColumn
import com.example.keios.ui.page.main.widget.sheet.SheetFieldBlock
import com.example.keios.ui.page.main.widget.sheet.SheetSectionCard
import com.example.keios.ui.page.main.widget.sheet.SheetSectionTitle
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OsShellRunnerSaveSheet(
    show: Boolean,
    title: String,
    commandInput: String,
    latestOutputEntry: ShellOutputDisplayEntry?,
    saveSheetCommandLabel: String,
    saveSheetFieldTitle: String,
    saveSheetFieldSubtitle: String,
    saveSheetTitleHint: String,
    saveSheetSubtitleHint: String,
    saveSheetTimePlaceholder: String,
    saveTitleInput: String,
    onSaveTitleInputChange: (String) -> Unit,
    saveSubtitleInput: String,
    onSaveSubtitleInputChange: (String) -> Unit,
    shellCommandAccentColor: Color,
    shellSuccessAccentColor: Color,
    shellStoppedAccentColor: Color,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = null,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = null,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.common_save),
                onClick = onConfirm
            )
        }
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            val commandPreview = commandInput.trim()
            val previewEntry = latestOutputEntry?.takeIf { it.command == commandPreview }
            val previewResult = previewEntry?.result
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.os_shell_card_run_output_not_ran)
            val previewTime = previewEntry?.timeLabel
                ?.takeIf { it.isNotBlank() }
                ?: saveSheetTimePlaceholder
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = saveSheetCommandLabel)
                Text(
                    text = "$ $commandPreview",
                    color = shellCommandAccentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = previewResult,
                    color = if (previewEntry?.isStopped == true) {
                        shellStoppedAccentColor
                    } else {
                        shellSuccessAccentColor
                    },
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = previewTime,
                    color = shellSuccessAccentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetFieldBlock(title = saveSheetFieldTitle) {
                    GlassSearchField(
                        value = saveTitleInput,
                        onValueChange = onSaveTitleInputChange,
                        label = saveSheetTitleHint,
                        backdrop = null,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(title = saveSheetFieldSubtitle) {
                    GlassSearchField(
                        value = saveSubtitleInput,
                        onValueChange = onSaveSubtitleInputChange,
                        label = saveSheetSubtitleHint,
                        backdrop = null,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
