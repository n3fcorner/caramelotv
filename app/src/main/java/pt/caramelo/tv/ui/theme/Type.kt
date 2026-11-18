package pt.caramelo.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import pt.caramelo.tv.R

/**
 * Carbona, a letra principal do manual da marca (18/11/2026, Francisco: a licenca cobre apps). Os OTF vem do manual
 * (6 TIPOGRAFIA - FONTE) tal e qual, embutidos no APK para nao depender da rede nem da letra do fabricante da TV.
 * Substitui a Poppins, que era a reserva enquanto a licenca so se sabia para a web.
 */
val Carbona = FontFamily(
    Font(R.font.carbona_regular, FontWeight.Normal),
    Font(R.font.carbona_medium, FontWeight.Medium),
    Font(R.font.carbona_semibold, FontWeight.SemiBold),
    Font(R.font.carbona_bold, FontWeight.Bold),
    Font(R.font.carbona_extrabold, FontWeight.ExtraBold),
    Font(R.font.carbona_black, FontWeight.Black),
)

/**
 * Tamanhos de 10-foot UI: nada abaixo de 14sp, que a tres metros ja e o limite do
 * legivel. Os tamanhos ficaram os da Poppins, que tambem tinha o olho pequeno para o corpo, e sobem um
 * ponto em relacao ao que se usaria com a Roboto.
 */
val CarameloTypography = Typography(
    displayLarge = estilo(54.sp, 60.sp, FontWeight.Bold),
    displayMedium = estilo(42.sp, 48.sp, FontWeight.Bold),
    // O ecra das analises usava displaySmall sem ele existir aqui, e o titulo caia na letra do sistema.
    displaySmall = estilo(36.sp, 42.sp, FontWeight.Bold),
    headlineMedium = estilo(31.sp, 38.sp, FontWeight.SemiBold),
    headlineSmall = estilo(25.sp, 32.sp, FontWeight.SemiBold),
    titleLarge = estilo(22.sp, 28.sp, FontWeight.SemiBold),
    titleMedium = estilo(19.sp, 25.sp, FontWeight.SemiBold),
    titleSmall = estilo(17.sp, 22.sp, FontWeight.Medium),
    bodyLarge = estilo(18.sp, 27.sp, FontWeight.Normal),
    bodyMedium = estilo(16.sp, 24.sp, FontWeight.Normal),
    bodySmall = estilo(14.sp, 21.sp, FontWeight.Normal),
    labelLarge = estilo(16.sp, 20.sp, FontWeight.Medium),
    labelMedium = estilo(14.sp, 18.sp, FontWeight.Medium),
    labelSmall = estilo(13.sp, 17.sp, FontWeight.Medium),
)

private fun estilo(
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
) = TextStyle(
    fontFamily = Carbona,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
)
