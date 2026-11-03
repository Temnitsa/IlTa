package com.iltatrainer.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.iltatrainer.app.core.ThemeMode

/** DeepSeek-Harness-inspired flat palette (light). */
private val LightColors = lightColorScheme(
    primary = Color(0xFF4D6BFE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8EDFF),
    onPrimaryContainer = Color(0xFF1B2C7A),
    secondary = Color(0xFF5F6673),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F2F5),
    onSecondaryContainer = Color(0xFF111318),
    tertiary = Color(0xFF6B7A99),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEF1F6),
    onTertiaryContainer = Color(0xFF111318),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF5F6673),
    outline = Color(0xFFC9CFDA),
    outlineVariant = Color(0xFFE4E7EC),
    error = Color(0xFFD92D20),
    onError = Color.White,
    errorContainer = Color(0xFFFEF3F2),
    onErrorContainer = Color(0xFF7A271A),
)

/** DeepSeek-Harness-inspired flat palette (dark). */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C93FF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF26305C),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFF9AA3B2),
    onSecondary = Color(0xFF0B1020),
    secondaryContainer = Color(0xFF20242C),
    onSecondaryContainer = Color(0xFFF2F3F5),
    tertiary = Color(0xFFA9B4C8),
    onTertiary = Color(0xFF0B1020),
    tertiaryContainer = Color(0xFF232832),
    onTertiaryContainer = Color(0xFFF2F3F5),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF2F3F5),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF20242C),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF3A4150),
    outlineVariant = Color(0xFF2A303B),
    error = Color(0xFFF97066),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF4B1D1D),
    onErrorContainer = Color(0xFFFEE4E2),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun IlTaTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // Keep status/navigation bar icons readable when the in-app theme differs from the system one.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
