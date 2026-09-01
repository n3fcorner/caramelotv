package pt.caramelo.tv.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val CarameloColors = darkColorScheme(
    primary = Amarelo,
    onPrimary = Marinho,
    primaryContainer = Laranja,
    onPrimaryContainer = Marinho,
    secondary = Laranja,
    onSecondary = Marinho,
    background = RoxoFundo,
    onBackground = Creme,
    surface = Roxo,
    onSurface = Creme,
    surfaceVariant = RoxoClaro,
    onSurfaceVariant = CremeFraco,
)

/** Margens de seguranca contra overscan: muitas TVs cortam os primeiros pixeis do painel. */
object Overscan {
    val horizontal = 40.dp
    val vertical = 22.dp
}

/**
 * Escala unica de espacamento. Antes havia 9, 13, 14, 26 e 36dp espalhados pelos
 * ecras; com uma escala so, ecras diferentes ganham o mesmo ritmo.
 */
object Espaco {
    val minimo = 4.dp
    val curto = 8.dp
    val medio = 12.dp
    val largo = 20.dp
    val seccao = 32.dp
}

/**
 * Molas usadas em todo o lado, para o movimento ter a mesma assinatura no ecra inteiro.
 * [Movimento.foco] e curta e firme; [Movimento.abertura] e mais macia, para o percurso
 * maior de um mosaico ate ao ecra de detalhes.
 */
object Movimento {
    val foco = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
    const val ESCALA_FOCO = 1.09f
}

@Composable
fun CarameloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CarameloColors,
        typography = CarameloTypography,
    ) {
        // Fora de uma Surface o LocalContentColor da lib de TV e preto, o que torna
        // qualquer Text sem cor explicita invisivel no fundo escuro.
        CompositionLocalProvider(LocalContentColor provides Creme, content = content)
    }
}
