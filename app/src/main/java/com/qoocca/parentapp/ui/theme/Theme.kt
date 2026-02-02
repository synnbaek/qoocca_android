package com.qoocca.parentapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.qoocca.parentapp.R

// Define the custom font family
val payboocFontFamily = FontFamily(
    Font(R.font.paybooc_medium, FontWeight.Normal),
    Font(R.font.paybooc_medium, FontWeight.Medium),
    Font(R.font.paybooc_bold, FontWeight.Bold)
)

// Define custom typography
val appTypography = Typography(
    displayLarge = TextStyle(fontFamily = payboocFontFamily),
    displayMedium = TextStyle(fontFamily = payboocFontFamily),
    displaySmall = TextStyle(fontFamily = payboocFontFamily),
    headlineLarge = TextStyle(fontFamily = payboocFontFamily),
    headlineMedium = TextStyle(fontFamily = payboocFontFamily),
    headlineSmall = TextStyle(fontFamily = payboocFontFamily),
    titleLarge = TextStyle(fontFamily = payboocFontFamily),
    titleMedium = TextStyle(fontFamily = payboocFontFamily),
    titleSmall = TextStyle(fontFamily = payboocFontFamily),
    bodyLarge = TextStyle(fontFamily = payboocFontFamily),
    bodyMedium = TextStyle(fontFamily = payboocFontFamily),
    bodySmall = TextStyle(fontFamily = payboocFontFamily),
    labelLarge = TextStyle(fontFamily = payboocFontFamily),
    labelMedium = TextStyle(fontFamily = payboocFontFamily),
    labelSmall = TextStyle(fontFamily = payboocFontFamily)
)

// Define the custom theme
@Composable
fun QooccaParentsTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = QooccaGreen,
        secondary = QooccaGreen,
        tertiary = QooccaGreen,
        onPrimary = Color.White
    )
    MaterialTheme(
        colorScheme = colors,
        typography = appTypography,
        content = content
    )
}

val QooccaGreen = Color(0xFF00CF83)
