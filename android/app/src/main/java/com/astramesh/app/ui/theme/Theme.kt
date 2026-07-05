package com.astramesh.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

fun Modifier.glassmorphism(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Color(0x15FFFFFF),
    borderColor: Color = Color(0x20FFFFFF)
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))

// Premium dark palette
val DeepBlack = Color(0xFF111B21)
val DarkSurface = Color(0xFF111B21) // Same as background for clean look
val CardSurface = Color(0xFF202C33)
val AccentViolet = Color(0xFF8696A0) // Using as secondary text
val AccentBlue = Color(0xFF00A884)
val AccentPink = Color(0xFF00A884)
val AccentCyan = Color(0xFF00A884) // Main Accent
val NeonGreen = Color(0xFF00A884)
val SoftWhite = Color(0xFFE9EDEF)
val MutedGray = Color(0xFF8696A0)
val DimGray = Color(0xFF3B4A54)

// Gradient brushes (Replacing with solid colors for professional look)
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(AccentCyan, AccentCyan)
)
val SentBubbleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF005C4B), Color(0xFF005C4B)) // WhatsApp sent bubble color
)
val AccentGradient = Brush.linearGradient(
    colors = listOf(AccentCyan, AccentCyan)
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentCyan,
    tertiary = AccentCyan,
    background = DeepBlack,
    surface = DarkSurface,
    surfaceVariant = CardSurface,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    onPrimary = DeepBlack,
    onSecondary = DeepBlack,
    onTertiary = DeepBlack,
    outline = DimGray,
    onSurfaceVariant = MutedGray
)

private val AstraTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        color = SoftWhite
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp,
        color = SoftWhite
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = SoftWhite
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = SoftWhite
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = SoftWhite
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = MutedGray
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = MutedGray
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = SoftWhite
    )
)

@Composable
fun AstraMeshTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBlack.toArgb()
            window.navigationBarColor = DeepBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AstraTypography,
        content = content
    )
}
