package com.example.keios.ui.page.main.widget

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class AppTypographyToken(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val fontWeight: FontWeight = FontWeight.Normal
)

object AppTypographyTokens {
    val Caption = AppTypographyToken(
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val Body = AppTypographyToken(
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val BodyEmphasis = AppTypographyToken(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    )

    val CardHeader = AppTypographyToken(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium
    )

    val CompactTitle = AppTypographyToken(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )

    val SectionTitle = AppTypographyToken(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold
    )
}
