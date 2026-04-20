package com.example.keios.ui.page.main.about.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.about.ui.AboutCompactInfoRow
import com.example.keios.ui.page.main.about.ui.AboutSectionCard
import com.example.keios.ui.page.main.os.appLucideExternalLinkIcon
import com.example.keios.ui.page.main.os.appLucideLockIcon
import com.example.keios.ui.page.main.os.appLucideNotesIcon

@Composable
fun AboutProjectLicenseCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenLicenseUrl: (String) -> Unit
) {
    val licenseUrl = stringResource(R.string.about_project_license_url)
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_project_license_title),
        subtitle = stringResource(R.string.about_card_project_license_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideLockIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            AboutCompactInfoRow(
                title = stringResource(R.string.about_project_license_row_name),
                value = stringResource(R.string.about_project_license_value_name),
                titleIcon = appLucideLockIcon()
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_project_license_row_spdx),
                value = stringResource(R.string.about_project_license_value_spdx),
                titleIcon = appLucideNotesIcon()
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_project_license_row_file),
                value = stringResource(R.string.about_project_license_value_file),
                titleIcon = appLucideNotesIcon()
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_project_license_row_copyright),
                value = stringResource(R.string.about_project_license_value_copyright),
                titleIcon = appLucideNotesIcon()
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_project_license_row_url),
                value = licenseUrl,
                titleIcon = appLucideExternalLinkIcon(),
                valueColor = accent,
                onClick = { onOpenLicenseUrl(licenseUrl) }
            )
        }
    }
}
