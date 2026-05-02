package os.kei.ui.page.main.mcp.skill.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.ui.page.main.os.osLucideCopyIcon
import os.kei.ui.page.main.mcp.skill.support.buildInlineStyledText
import os.kei.ui.page.main.mcp.util.copyToClipboard
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import os.kei.ui.page.main.widget.support.copyModeAwareRow
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun McpSkillClawGuideCard(
    title: String,
    summary: String,
    prompt: String,
    copyContentDescription: String,
    copiedToastText: String,
    titleColor: Color,
    subtitleColor: Color,
    accentColor: Color,
    codeColor: Color
) {
    val context = LocalContext.current
    val copyIcon = osLucideCopyIcon()
    AppSurfaceCard(
        contentColor = titleColor,
        showIndication = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                AppStandaloneLiquidIconButton(
                    icon = copyIcon,
                    contentDescription = copyContentDescription,
                    onClick = {
                        copyToClipboard(context, "claw-skill-prompt", prompt)
                        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                    },
                    width = 34.dp,
                    height = 28.dp,
                    variant = GlassVariant.Content,
                    iconTint = accentColor
                )
            }

            CopyModeSelectionContainer {
                Text(
                    text = buildInlineStyledText(
                        text = summary,
                        baseStyle = SpanStyle(color = subtitleColor),
                        accentStyle = SpanStyle(
                            color = accentColor,
                            background = accentColor.copy(alpha = 0.10f),
                            fontWeight = FontWeight.Medium
                        ),
                        linkStyle = SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ),
                    color = subtitleColor,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }

            AppSurfaceCard(
                containerColor = codeColor,
                contentColor = titleColor,
                showIndication = false
            ) {
                CopyModeSelectionContainer {
                    Text(
                        text = prompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .copyModeAwareRow(copyPayload = prompt),
                        color = titleColor,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
