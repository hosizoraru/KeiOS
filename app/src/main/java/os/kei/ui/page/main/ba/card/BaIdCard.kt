package os.kei.ui.page.main.ba.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.ba.BaGlassCard
import os.kei.ui.page.main.ba.BaGlassPanel
import os.kei.ui.page.main.ba.support.BA_DEFAULT_FRIEND_CODE
import os.kei.ui.page.main.ba.support.BA_DEFAULT_NICKNAME
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.AppLiquidSearchField
import os.kei.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaIdCard(
    backdrop: Backdrop?,
    idNicknameInput: String,
    onIdNicknameInputChange: (String) -> Unit,
    onSaveIdNickname: () -> Unit,
    idFriendCodeInput: String,
    onIdFriendCodeInputChange: (String) -> Unit,
    onSaveIdFriendCode: () -> Unit,
) {
    val nicknameLengthForWidth = idNicknameInput.ifEmpty { BA_DEFAULT_NICKNAME }.length.coerceIn(1, 10)
    val nicknameFieldWidth = (nicknameLengthForWidth * 10 + 24).coerceIn(68, 108).dp
    val friendCodeLengthForWidth = idFriendCodeInput.ifEmpty { BA_DEFAULT_FRIEND_CODE }.length.coerceIn(1, 8)
    val friendCodeFieldWidth = (friendCodeLengthForWidth * 10 + 28).coerceIn(86, 116).dp
    val nicknameSuffixWidth = 44.dp
    val trailingSlotWidth = maxOf(
        nicknameFieldWidth + 4.dp + nicknameSuffixWidth,
        friendCodeFieldWidth
    )
    val accentBlue = Color(0xFF3B82F6)

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentBlue,
        accentAlpha = 0f,
    ) {
        BaCardHeader(title = stringResource(R.string.ba_id_card_title))

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = accentBlue,
        ) {
            BaIdFieldRow(
                label = stringResource(R.string.ba_id_label_nickname),
                trailingSlotWidth = trailingSlotWidth,
                labelIconRes = R.drawable.collectible_icon_guidemission_s3,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppLiquidSearchField(
                        modifier = Modifier.width(nicknameFieldWidth),
                        value = idNicknameInput,
                        onValueChange = onIdNicknameInputChange,
                        onImeActionDone = onSaveIdNickname,
                        label = BA_DEFAULT_NICKNAME,
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        textColor = accentBlue,
                        minHeight = 34.dp,
                        horizontalPadding = 10.dp,
                        verticalPadding = 5.dp,
                    )
                    Box(
                        modifier = Modifier
                            .width(nicknameSuffixWidth)
                            .height(34.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .offset(y = (-1).dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.ba_id_nickname_suffix),
                            color = accentBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize = AppTypographyTokens.Body.fontSize,
                            lineHeight = AppTypographyTokens.Body.fontSize,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = accentBlue,
        ) {
            BaIdFieldRow(
                label = stringResource(R.string.ba_id_label_friend_code),
                trailingSlotWidth = trailingSlotWidth,
            ) {
                AppLiquidSearchField(
                    modifier = Modifier.width(friendCodeFieldWidth),
                    value = idFriendCodeInput,
                    onValueChange = onIdFriendCodeInputChange,
                    onImeActionDone = onSaveIdFriendCode,
                    label = BA_DEFAULT_FRIEND_CODE,
                    backdrop = backdrop,
                    variant = GlassVariant.SheetInput,
                    singleLine = true,
                    textAlign = TextAlign.Center,
                    textColor = accentBlue,
                    minHeight = 34.dp,
                    horizontalPadding = 10.dp,
                    verticalPadding = 5.dp,
                )
            }
        }
    }
}

@Composable
private fun BaIdFieldRow(
    label: String,
    trailingSlotWidth: Dp,
    labelIconRes: Int? = null,
    trailingContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(64.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = MiuixTheme.colorScheme.onBackground,
            )
            if (labelIconRes != null) {
                Image(
                    painter = painterResource(id = labelIconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .width(30.dp)
                        .height(24.dp),
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                modifier = Modifier.width(trailingSlotWidth),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = trailingContent,
            )
        }
    }
}
