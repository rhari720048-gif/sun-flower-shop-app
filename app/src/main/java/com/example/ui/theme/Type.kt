package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairDisplayFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
)

val NotoSansTamilFontFamily = FontFamily(
    Font(R.font.noto_sans_tamil, FontWeight.Normal),
    Font(R.font.noto_sans_tamil, FontWeight.Medium),
    Font(R.font.noto_sans_tamil, FontWeight.SemiBold),
    Font(R.font.noto_sans_tamil, FontWeight.Bold)
)

fun buildSunFlowersTypography(isTamil: Boolean = false): Typography {
    val uiFont = if (isTamil) NotoSansTamilFontFamily else PoppinsFontFamily
    val displayFont = if (isTamil) NotoSansTamilFontFamily else PlayfairDisplayFontFamily
    val lineScale = if (isTamil) 1.5f else 1.38f

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = (32 * lineScale).sp,
            letterSpacing = 0.5.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = (24 * lineScale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = (20 * lineScale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = (18 * lineScale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = (16 * lineScale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = (14 * lineScale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = (16 * lineScale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = (14 * lineScale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = (12 * lineScale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = (14 * lineScale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = (12 * lineScale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = (11 * lineScale).sp
        )
    )
}
