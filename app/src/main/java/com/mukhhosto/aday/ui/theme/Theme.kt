
package com.mukhhosto.aday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mukhhosto.aday.R

val ForestGreen = Color(0xFF236B4A)
val BluePrimary = ForestGreen // Compatibility alias for existing result components
val BlueDark = Color(0xFF174B35)
val MutedText = Color(0xFF6B756F)
val BorderGray = Color(0xFFE6E5DD)
val OffWhite = Color(0xFFF7F5EF)
val WhiteCard = Color(0xFFFFFEFA)
val DarkGray = Color(0xFF2D342F)
val SuccessGreen = Color(0xFF237A4B)
val ErrorRed = Color(0xFFC43D3D)
val WarnAmber = Color(0xFFB7791F)
val PaleGreen = Color(0xFFE7F2E9)
val PaleRed = Color(0xFFFFEAE7)

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    background = OffWhite,
    onBackground = DarkGray,
    surface = WhiteCard,
    onSurface = DarkGray,
    error = ErrorRed
)

val BengaliFont = FontFamily(
    Font(R.font.noto_sans_bengali_regular, FontWeight.Normal),
    Font(R.font.noto_sans_bengali_bold, FontWeight.Bold)
)

private fun TextStyle.withBengaliFont() = copy(fontFamily = BengaliFont)

val BengaliTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.withBengaliFont(),
        displayMedium = base.displayMedium.withBengaliFont(),
        displaySmall = base.displaySmall.withBengaliFont(),
        headlineLarge = base.headlineLarge.withBengaliFont(),
        headlineMedium = base.headlineMedium.withBengaliFont(),
        headlineSmall = base.headlineSmall.withBengaliFont(),
        titleLarge = base.titleLarge.copy(fontFamily = BengaliFont, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkGray),
        titleMedium = base.titleMedium.withBengaliFont(),
        titleSmall = base.titleSmall.withBengaliFont(),
        bodyLarge = base.bodyLarge.copy(fontFamily = BengaliFont, fontSize = 18.sp, color = DarkGray),
        bodyMedium = base.bodyMedium.copy(fontFamily = BengaliFont, fontSize = 16.sp, color = DarkGray),
        bodySmall = base.bodySmall.withBengaliFont(),
        labelLarge = base.labelLarge.copy(fontFamily = BengaliFont, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.withBengaliFont(),
        labelSmall = base.labelSmall.withBengaliFont()
    )
}

@Composable
fun MukhhostoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = BengaliTypography,
        content = content
    )
}
