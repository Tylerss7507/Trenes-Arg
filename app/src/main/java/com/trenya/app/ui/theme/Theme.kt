package com.trenya.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// -----------------------------------------------------------------------
// Paleta de marca. Un azul "riel" saturado como color de acción, y un ámbar
// cálido para la cuenta regresiva del próximo tren (el elemento más
// importante de toda la app: tiene que saltar a la vista).
// -----------------------------------------------------------------------
object TrenYaColors {
    val RailBlue = Color(0xFF2954E3)
    val RailBlueDark = Color(0xFF7C9CFF)
    val DeepNavy = Color(0xFF0D1B3E)
    val Amber = Color(0xFFF5A623)
    val Teal = Color(0xFF1FB6A6)
    val Delayed = Color(0xFFE14545)
    val OnTime = Color(0xFF2FAE60)
    val SurfaceLight = Color(0xFFF7F8FC)
    val SurfaceDark = Color(0xFF10142B)

    // Colores por línea, para las chips e íconos. Son una elección de diseño
    // propia (la API no expone colores oficiales); el naranja de San Martín
    // sí replica su color identificatorio histórico real.
    fun forLine(lineName: String?): Color {
        val n = lineName?.trim()?.lowercase().orEmpty()
        return when {
            n.contains("mitre") -> Color(0xFF2954E3)
            n.contains("sarmiento") -> Color(0xFF29ABE2)
            n.contains("roca") -> Color(0xFFE14545)
            n.contains("san mart") -> Color(0xFFF5A623)
            n.contains("belgrano norte") -> Color(0xFF2FAE60)
            n.contains("belgrano sur") -> Color(0xFF8E4EC6)
            n.contains("urquiza") -> Color(0xFFC2185B)
            n.contains("costa") -> Color(0xFF29ABE2)
            else -> RailBlue
        }
    }
}

private val LightColors = lightColorScheme(
    primary = TrenYaColors.RailBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF001453),
    secondary = TrenYaColors.Amber,
    onSecondary = Color(0xFF2A1800),
    secondaryContainer = Color(0xFFFFE3B8),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = TrenYaColors.Teal,
    background = TrenYaColors.SurfaceLight,
    onBackground = Color(0xFF15182B),
    surface = Color.White,
    onSurface = Color(0xFF15182B),
    surfaceVariant = Color(0xFFE5E8F5),
    onSurfaceVariant = Color(0xFF464A5E),
    error = TrenYaColors.Delayed,
    outline = Color(0xFFC5C9DB)
)

private val DarkColors = darkColorScheme(
    primary = TrenYaColors.RailBlueDark,
    onPrimary = Color(0xFF001453),
    primaryContainer = Color(0xFF1D3585),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = TrenYaColors.Amber,
    onSecondary = Color(0xFF2A1800),
    secondaryContainer = Color(0xFF573E00),
    onSecondaryContainer = Color(0xFFFFE3B8),
    tertiary = TrenYaColors.Teal,
    background = TrenYaColors.SurfaceDark,
    onBackground = Color(0xFFE4E6F5),
    surface = Color(0xFF181C36),
    onSurface = Color(0xFFE4E6F5),
    surfaceVariant = Color(0xFF2A2F4C),
    onSurfaceVariant = Color(0xFFC5C9DB),
    error = Color(0xFFFF8A80),
    outline = Color(0xFF464A5E)
)

// Tipografía basada en la fuente de sistema (Roboto): sin un archivo de
// fuente propio embebido, la identidad visual se construye con escala y
// peso -numerales grandes y bien pesados para la cuenta regresiva, títulos
// ajustados- en vez de con un tipo de letra distinto. Se puede reemplazar
// por una fuente propia agregando archivos a res/font y referenciándolos acá.
val TrenYaTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

/** Estilo grande y pesado reservado para la cuenta regresiva del próximo tren. */
val CountdownTextStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 40.sp,
    letterSpacing = (-1).sp
)

@Composable
fun TrenYaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = TrenYaTypography,
        content = content
    )
}
