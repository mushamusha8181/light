package jp.mushamusha.mushinglass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

object MushinGlassCompose {
    @Composable
    fun Theme(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val cfg = remember { MushinGlass.config(context) }
        val alpha = (cfg.glassOpacity / 100f).coerceIn(0.35f, 0.95f)
        val key = Color(cfg.palette.primary)
        val text = Color(cfg.textColor)
        val background = Color(cfg.background)
        val row = Color(cfg.rowBackground)
        val middle = Color(cfg.middle)
        val auxiliary = Color(cfg.auxiliary)

        val scheme = remember(cfg.palette.id, cfg.textColor, cfg.glassOpacity) {
            lightColorScheme(
                primary = key.copy(alpha = 0.82f),
                onPrimary = text,
                primaryContainer = key.copy(alpha = 0.20f),
                onPrimaryContainer = text,
                secondary = middle,
                onSecondary = text,
                secondaryContainer = row.copy(alpha = alpha),
                onSecondaryContainer = text,
                tertiary = auxiliary,
                onTertiary = text,
                background = background,
                onBackground = text,
                surface = Color.White.copy(alpha = alpha),
                onSurface = text,
                surfaceVariant = row.copy(alpha = alpha),
                onSurfaceVariant = text,
                outline = Color.White.copy(alpha = 0.72f),
                outlineVariant = key.copy(alpha = 0.18f),
                scrim = Color.Black.copy(alpha = 0.30f)
            )
        }

        val fontId = remember { context.resources.getIdentifier("mushin", "font", context.packageName) }
        val family = remember(fontId) { if (fontId != 0) FontFamily(Font(fontId)) else FontFamily.Default }
        val typography = remember(family) { Typography().withFamily(family) }
        val shapes = remember {
            Shapes(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(34.dp)
            )
        }
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
    }

    private fun Typography.withFamily(family: FontFamily) = Typography(
        displayLarge = displayLarge.withFamily(family),
        displayMedium = displayMedium.withFamily(family),
        displaySmall = displaySmall.withFamily(family),
        headlineLarge = headlineLarge.withFamily(family),
        headlineMedium = headlineMedium.withFamily(family),
        headlineSmall = headlineSmall.withFamily(family),
        titleLarge = titleLarge.withFamily(family),
        titleMedium = titleMedium.withFamily(family),
        titleSmall = titleSmall.withFamily(family),
        bodyLarge = bodyLarge.withFamily(family),
        bodyMedium = bodyMedium.withFamily(family),
        bodySmall = bodySmall.withFamily(family),
        labelLarge = labelLarge.withFamily(family),
        labelMedium = labelMedium.withFamily(family),
        labelSmall = labelSmall.withFamily(family)
    )

    private fun TextStyle.withFamily(family: FontFamily) = copy(fontFamily = family)
}
