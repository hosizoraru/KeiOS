package com.example.keios.ui.page.main

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.composables.icons.lucide.R as LucideR

@Composable
internal fun osLucideConsoleIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_square_terminal)

@Composable
internal fun osLucideShellIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_terminal)

@Composable
internal fun osLucideCardIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_square_stack)

@Composable
internal fun osLucideEnterIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_log_in)

@Composable
internal fun osLucideRunIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_play)

@Composable
internal fun osLucideStopIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_square_stop)

@Composable
internal fun osLucideSaveIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_save)

@Composable
internal fun osLucideFormatIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_wand_sparkles)

@Composable
internal fun osLucideClearIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_eraser)

@Composable
internal fun osLucideClearAllIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_trash_2)

@Composable
internal fun osLucideSettingsIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_settings_2)

@Composable
internal fun osLucideCopyIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_copy)

@Composable
internal fun appLucideEditIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_file_pen_line)

@Composable
internal fun appLucideConfigIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_sliders_horizontal)

@Composable
internal fun appLucideSortIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_arrow_up_down)

@Composable
internal fun appLucideRefreshIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_refresh_cw)

@Composable
internal fun appLucideNotesIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_notebook_text)

@Composable
internal fun appLucideAddIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_circle_plus)

@Composable
internal fun appLucideDownloadIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_download)

@Composable
internal fun appLucideShareIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_share_2)

@Composable
internal fun appLucideCloseIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_x)

@Composable
internal fun appLucideConfirmIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_check)

@Composable
internal fun appLucidePauseIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_pause)

@Composable
internal fun appLucideWarningIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_triangle_alert)

@Composable
internal fun appLucideAlertIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_circle_alert)

@Composable
internal fun appLucideMoreIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_ellipsis)

@Composable
internal fun appLucideInfoIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_info)

@Composable
internal fun appLucidePackageIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_package)

@Composable
internal fun appLucideListIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_list)

@Composable
internal fun appLucideLockIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_lock)

@Composable
internal fun appLucideLayersIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_layers_2)

@Composable
internal fun appLucideAppWindowIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_app_window)

@Composable
internal fun appLucideFilterIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_list_filter)

@Composable
internal fun appLucideExternalLinkIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_external_link)

@Composable
internal fun appLucideBackIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_arrow_left)

@Composable
internal fun appLucideTimeIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_clock_3)

@Composable
internal fun appLucideVersionIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_tag)

@Composable
internal fun appLucideMediaIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_image)

@Composable
internal fun appLucideBranchIcon(): ImageVector = osLucideVector(LucideR.drawable.lucide_ic_git_branch)

@Composable
private fun osLucideVector(@DrawableRes drawableRes: Int): ImageVector = ImageVector.vectorResource(drawableRes)
