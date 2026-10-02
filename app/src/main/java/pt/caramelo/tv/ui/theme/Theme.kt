package pt.caramelo.tv.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import pt.caramelo.tv.data.model.Section
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

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

/**
 * Margens de seguranca contra overscan: muitas TVs cortam os primeiros pixeis do painel. 1/10/2026 ("os limites de
 * ecra na TV nao estao certos"): passam dos 4 % para cerca de 6 % do ecra (58 de 960 e 34 de 540 dp), acima dos
 * 5 % que o Android TV recomenda, para a TV da loja nao comer o cabecalho nem os mosaicos das pontas.
 */
object Overscan {
    val horizontal = 58.dp
    val vertical = 34.dp
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

/**
 * O tema do papel (1/10/2026, "no mesmo estilo do site"): a lista e a ficha de cada flor em bege com tinta azul
 * escura, como caramelodobrasil.eu. Os botoes (TvButton) leem daqui: o principal continua amarelo com tinta, o
 * discreto e' tinta sobre o papel. O leitor de video, as analises e as definicoes ficam no [CarameloTheme] escuro.
 */
private val PapelColors = lightColorScheme(
    primary = Amarelo,
    onPrimary = Marinho,
    primaryContainer = Laranja,
    onPrimaryContainer = Marinho,
    secondary = Laranja,
    onSecondary = Marinho,
    background = Papel,
    onBackground = Tinta,
    surface = Papel,
    onSurface = Tinta,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE6DCB9),
    onSurfaceVariant = TintaFraca,
)

@Composable
fun PapelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PapelColors, typography = CarameloTypography) {
        CompositionLocalProvider(LocalContentColor provides Tinta, content = content)
    }
}

/** Verdadeiro dentro do [PapelTheme]: a tinta do conteudo e' escura. */
@Composable
fun noPapel(): Boolean = LocalContentColor.current.luminance() < 0.5f

/**
 * A mesma cor, legivel no fundo onde esta (1/10/2026): no papel o bege passa a tinta, o amarelo escurece, o lilas e o
 * verde claro voltam ao roxo e ao verde do manual; no escuro fica tudo como era.
 */
@Composable
fun legivel(c: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    if (!noPapel()) return c
    return when (c) {
        Creme -> Tinta
        CremeFraco -> TintaFraca
        Amarelo -> androidx.compose.ui.graphics.Color(0xFFB98500)
        Lilas -> Roxo
        VerdeClaro -> Verde
        else -> c
    }
}

/**
 * As regras de cor da TV (2/10/2026, Francisco: "tudo colorido e chamativo, com regras claras, com as cores da marca"):
 *
 *  1. O cabecalho e' a faixa da marca: azul escuro a fundir para o roxo tropical, da altura da logomarca.
 *  2. Cada seccao tem uma cor so, a mesma em todo o lado (separador, barra, cartao, etiqueta):
 *     CBD verde Brasil, infusao roxo tropical, hash laranja caramelo.
 *  3. O foco e' sempre amarelo canario: botao, separador ou cartao com o foco acende a amarelo, e mais nada o faz.
 *  4. O papel bege fica por baixo de tudo; o vermelho e' so para o SOLD OUT.
 */
fun corDaSeccao(s: Section): Color = when (s) {
    Section.CBD -> Verde
    Section.INFUSAO -> Roxo
    Section.HASH -> Laranja
}

/** A faixa do cabecalho (2/10/2026, Francisco: "so quero o roxo no header, o papel em baixo"): o mesmo roxo profundo da
 * montra do telemovel, liso de ponta a ponta; por baixo fica o papel. */
val FaixaDaMarca: Brush = Brush.horizontalGradient(listOf(RoxoTopo, RoxoTopo))
