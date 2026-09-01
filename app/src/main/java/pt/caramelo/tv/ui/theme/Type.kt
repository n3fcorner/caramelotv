package pt.caramelo.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import pt.caramelo.tv.R

/**
 * Poppins, a geometrica do logotipo Caramelo do Brasil — o mesmo O circular do
 * wordmark. Embutida no APK, para nao depender da rede nem da letra do fabricante
 * da TV, que varia de modelo para modelo.
 */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/**
 * Tamanhos de 10-foot UI: nada abaixo de 14sp, que a tres metros ja e o limite do
 * legivel. A Poppins tem olho pequeno para o corpo, por isso os tamanhos sobem um
 * ponto em relacao ao que se usaria com a Roboto.
 */
val CarameloTypography = Typography(
    displayLarge = estilo(54.sp, 60.sp, FontWeight.Bold),
    displayMedium = estilo(42.sp, 48.sp, FontWeight.Bold),
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
    fontFamily = Poppins,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
)
