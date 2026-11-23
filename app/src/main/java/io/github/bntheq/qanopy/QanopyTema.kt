package io.github.bntheq.qanopy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Qanopy'nin kendi tema sistemi. Hicbir hazir tasarim kutuphanesine bagli degil.

class QRenkler(
    val background: Color, val surface: Color, val surfaceVariant: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color, val onSurfaceVariant: Color,
    val outline: Color, val outlineVariant: Color,
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val secondaryContainer: Color, val tertiaryContainer: Color,
    val onContainer: Color, val error: Color
) {
    val onBackground: Color get() = onSurface
    val secondary: Color get() = onSurfaceVariant
    val onSecondary: Color get() = onPrimary
    val tertiary: Color get() = onSurfaceVariant
    val onTertiary: Color get() = onPrimary
    val onPrimaryContainer: Color get() = onContainer
    val onSecondaryContainer: Color get() = onContainer
    val onTertiaryContainer: Color get() = onContainer
    val onError: Color get() = onPrimary
    val errorContainer: Color get() = surfaceVariant
    val onErrorContainer: Color get() = error
    val surfaceContainer: Color get() = surfaceVariant
    val surfaceContainerHigh: Color get() = surfaceContainerHighest
    val surfaceContainerLow: Color get() = surface
    val surfaceContainerLowest: Color get() = background
    val inverseSurface: Color get() = onSurface
    val inverseOnSurface: Color get() = background
    val inversePrimary: Color get() = primary
    val scrim: Color get() = Color.Black
}

val KOYU_RENK = QRenkler(
    background = Color(0xFF111114), surface = Color(0xFF18181C), surfaceVariant = Color(0xFF2A2A30),
    surfaceContainerHighest = Color(0xFF24242A),
    onSurface = Color(0xFFEDEDF0), onSurfaceVariant = Color(0xFFA3A3AD),
    outline = Color(0xFF6A6A76), outlineVariant = Color(0xFF3A3A42),
    primary = Color(0xFFE6E6EA), onPrimary = Color(0xFF111114),
    primaryContainer = Color(0xFF26262C), secondaryContainer = Color(0xFF222228),
    tertiaryContainer = Color(0xFF1E1E24),
    onContainer = Color(0xFFEDEDF0), error = Color(0xFFFF7A7A)
)

val ACIK_RENK = QRenkler(
    background = Color(0xFFF3F3F5), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE2E2E7),
    surfaceContainerHighest = Color(0xFFE9E9ED),
    onSurface = Color(0xFF17171B), onSurfaceVariant = Color(0xFF5C5C66),
    outline = Color(0xFF8A8A94), outlineVariant = Color(0xFFC8C8D0),
    primary = Color(0xFF1E1E22), onPrimary = Color(0xFFF3F3F5),
    primaryContainer = Color(0xFFE4E4E9), secondaryContainer = Color(0xFFEAEAEE),
    tertiaryContainer = Color(0xFFF0F0F3),
    onContainer = Color(0xFF17171B), error = Color(0xFFC0392B)
)

class QYazi(
    val displayLarge: TextStyle, val displayMedium: TextStyle, val displaySmall: TextStyle,
    val headlineLarge: TextStyle, val headlineMedium: TextStyle, val headlineSmall: TextStyle,
    val titleLarge: TextStyle, val titleMedium: TextStyle, val titleSmall: TextStyle,
    val bodyLarge: TextStyle, val bodyMedium: TextStyle, val bodySmall: TextStyle,
    val labelLarge: TextStyle, val labelMedium: TextStyle, val labelSmall: TextStyle
)

private fun stil(a: FontFamily, boyut: Int, kal: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(fontFamily = a, fontSize = boyut.sp, fontWeight = kal, lineHeight = (boyut + 8).sp)

fun qYazi(piksel: Boolean): QYazi {
    val a = if (piksel) FontFamily.Monospace else FontFamily.Default
    return QYazi(
        stil(a, 57), stil(a, 45), stil(a, 36),
        stil(a, 32), stil(a, 28), stil(a, 24),
        stil(a, 22), stil(a, 16, FontWeight.Medium), stil(a, 14, FontWeight.Medium),
        stil(a, 16), stil(a, 14), stil(a, 12),
        stil(a, 14, FontWeight.Medium), stil(a, 12, FontWeight.Medium), stil(a, 11, FontWeight.Medium)
    )
}

class QSekiller(
    val extraSmall: PikselSekil, val small: PikselSekil, val medium: PikselSekil,
    val large: PikselSekil, val extraLarge: PikselSekil
)

val LocalQRenkler = staticCompositionLocalOf<QRenkler> { KOYU_RENK }
val LocalQYazi = staticCompositionLocalOf<QYazi> { qYazi(true) }
val LocalQSekiller = staticCompositionLocalOf<QSekiller> { PikselSekiller }
// Vurgu renkleri (Palette Swap). Degerler ADD baslangic degerleridir; yenileri burada degisir.
class QVurgu(val ad: Int, val koyu: Color, val ana: Color, val neon: Color)

val QVURGULAR = listOf(
    QVurgu(R.string.ayar58_c1, Color(0xFF0D423D), Color(0xFF1CB8A5), Color(0xFF58F0DC)),
    QVurgu(R.string.ayar58_c2, Color(0xFF0E2342), Color(0xFF2558A6), Color(0xFF6EA6FA)),
    QVurgu(R.string.ayar58_c3, Color(0xFF1E1B54), Color(0xFF4F46E5), Color(0xFF8E86FC)),
    QVurgu(R.string.ayar58_c4, Color(0xFF3C1B54), Color(0xFF9D4EDD), Color(0xFFC77DFF)),
    QVurgu(R.string.ayar58_c5, Color(0xFF420D18), Color(0xFF8F1D33), Color(0xFFD9415F)),
    QVurgu(R.string.ayar58_c6, Color(0xFF520E2E), Color(0xFFC92A6D), Color(0xFFF763A1)),
    QVurgu(R.string.ayar58_c7, Color(0xFF54190B), Color(0xFFE6451C), Color(0xFFFF7A54)),
    QVurgu(R.string.ayar58_c8, Color(0xFF522B08), Color(0xFFF58216), Color(0xFFFFAE57)),
    QVurgu(R.string.ayar58_c9, Color(0xFF473208), Color(0xFFE0A01E), Color(0xFFFFCD5E)),
    QVurgu(R.string.ayar58_c10, Color(0xFF29420D), Color(0xFF76B828), Color(0xFFA8F250))
)

val LocalQVurgu = staticCompositionLocalOf<QVurgu> { QVURGULAR[0] }

val LocalIcerikRenk = staticCompositionLocalOf<Color> { Color.Unspecified }
val LocalYaziStili = staticCompositionLocalOf<TextStyle> { TextStyle.Default }

object QTema {
    val colorScheme: QRenkler
        @Composable @ReadOnlyComposable get() = LocalQRenkler.current
    val typography: QYazi
        @Composable @ReadOnlyComposable get() = LocalQYazi.current
    val shapes: QSekiller
        @Composable @ReadOnlyComposable get() = LocalQSekiller.current
    val vurgu: QVurgu
        @Composable @ReadOnlyComposable get() = LocalQVurgu.current
}

@Composable
fun QanopyTema(content: @Composable () -> Unit) {
    val sistemKoyu = isSystemInDarkTheme()
    val koyu = when (Tercih.tema) {
        1 -> false
        2 -> true
        else -> sistemKoyu
    }
    val renk = if (koyu) KOYU_RENK else ACIK_RENK
    val piksel = Tercih.pikselYazi
    val yazi = remember(piksel) { qYazi(piksel) }
    CompositionLocalProvider(
        LocalQRenkler provides renk,
        LocalQYazi provides yazi,
        LocalQSekiller provides PikselSekiller,
        LocalQVurgu provides QVURGULAR[Tercih.vurguNo.coerceIn(0, QVURGULAR.size - 1)],
        LocalIcerikRenk provides renk.onSurface,
        LocalYaziStili provides yazi.bodyLarge
    ) {
        content()
    }
}
