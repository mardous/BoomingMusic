package com.mardous.booming.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mardous.booming.R

val GoogleSansFlex = FontFamily(
    Font(R.font.googlesansflex_extralight, FontWeight.ExtraLight),
    Font(R.font.googlesansflex_light, FontWeight.Light),
    Font(R.font.googlesansflex_regular, FontWeight.Normal),
    Font(R.font.googlesansflex_medium, FontWeight.Medium),
    Font(R.font.googlesansflex_semibold, FontWeight.SemiBold),
    Font(R.font.googlesansflex_bold, FontWeight.Bold),
    Font(R.font.googlesansflex_extrabold, FontWeight.ExtraBold)
)

val defaultTypography = Typography()
val customTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSansFlex),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSansFlex),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSansFlex),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSansFlex),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSansFlex),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSansFlex),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSansFlex),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSansFlex),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSansFlex),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSansFlex),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSansFlex),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSansFlex),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSansFlex),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSansFlex),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSansFlex),
)