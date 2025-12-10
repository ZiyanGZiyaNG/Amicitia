package com.example.amicitia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import com.example.amicitia.R

// ----------------------------
// Google Font Provider
// ----------------------------
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ----------------------------
// 中文字體：Noto Sans TC
// ----------------------------
val NotoSansTC = FontFamily(
    Font(
        googleFont = GoogleFont("Noto Sans TC"),
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Noto Sans TC"),
        fontProvider = provider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Noto Sans TC"),
        fontProvider = provider,
        weight = FontWeight.Bold
    )
)

// ----------------------------
// 全域 Typography
// ----------------------------
val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = NotoSansTC,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansTC,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansTC,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansTC,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)