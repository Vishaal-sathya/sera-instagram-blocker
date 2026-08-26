package com.example.leetcodegate.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// OpenCode TUI Aesthetic
private val OpenCodeColorScheme = lightColorScheme(
    primary = Color(0xFF201D1D), // Ink
    onPrimary = Color(0xFFF8F0ED), // Paper
    secondary = Color(0xFF007AFF), // Accent Blue (for subtle highlights)
    onSecondary = Color(0xFFFDFCFC),
    background = Color(0xFFF8F0ED), // Cream/Paper
    surface = Color(0xFFF8F0ED),
    onBackground = Color(0xFF201D1D), // Ink
    onSurface = Color(0xFF201D1D), // Ink
    error = Color(0xFFFF3B30), // Danger
    onError = Color(0xFFFDFCFC),
    outline = Color(0x1F0F0000) // Hairline (rgba(15,0,0,0.12)) -> approx 0x1F0F0000
)

private val OpenCodeTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 57.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 32.sp), // Button text (lineHeight 2.0 -> 32sp)
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 28.sp) // Caption
)

@Composable
fun DystopianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OpenCodeColorScheme,
        typography = OpenCodeTypography,
        content = content
    )
}
