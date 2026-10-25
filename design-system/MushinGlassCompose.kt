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

@Composable
fun MushinGlassTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val cfg = MushinGlass.config(context)
    val fontId = remember { context.resources.getIdentifier("mushin", "font", context.packageName) }
    val family = remember(fontId) { if (fontId != 0) FontFamily(Font(fontId)) else FontFamily.Default }
    val alpha = (cfg.glassOpacity / 100f).coerceIn(0.35f, 0.95f)
    val scheme = lightColorScheme(
        primary = Color(cfg.palette.primary),
        onPrimary = Color(cfg.textColor),
        primaryContainer = Color(cfg.band).copy(alpha = alpha),
        onPrimaryContainer = Color(cfg.textColor),
        background = Color(cfg.background),
        onBackground = Color(cfg.textColor),
        surface = Color(cfg.rowBackground).copy(alpha = alpha),
        onSurface = Color(cfg.textColor),
        surfaceVariant = Color(cfg.listBackground).copy(alpha = alpha),
        onSurfaceVariant = Color(cfg.auxiliary),
        outline = Color(cfg.middle).copy(alpha = 0.55f)
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography().withMushin(family),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}

object MushinGlassCompose {
    @Composable
    fun Theme(content: @Composable () -> Unit) = MushinGlassTheme(content)
}

private fun TextStyle.mushin(family: FontFamily) = copy(fontFamily = family)
private fun Typography.withMushin(family: FontFamily) = Typography(
    displayLarge = displayLarge.mushin(family),
    displayMedium = displayMedium.mushin(family),
    displaySmall = displaySmall.mushin(family),
    headlineLarge = headlineLarge.mushin(family),
    headlineMedium = headlineMedium.mushin(family),
    headlineSmall = headlineSmall.mushin(family),
    titleLarge = titleLarge.mushin(family),
    titleMedium = titleMedium.mushin(family),
    titleSmall = titleSmall.mushin(family),
    bodyLarge = bodyLarge.mushin(family),
    bodyMedium = bodyMedium.mushin(family),
    bodySmall = bodySmall.mushin(family),
    labelLarge = labelLarge.mushin(family),
    labelMedium = labelMedium.mushin(family),
    labelSmall = labelSmall.mushin(family)
)
