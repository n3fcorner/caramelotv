package pt.caramelo.tv.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.theme.Espaco

/**
 * Grelha de tamanho fixo, com as tres seccoes visiveis ao mesmo tempo.
 *
 * Row/Column em vez de LazyVerticalGrid: com contagens conhecidas nao ha ganho em ser
 * lazy, nenhum mosaico sai de composicao, e a pesquisa de foco 2D do Compose fica
 * previsivel — o D-pad nunca perde o alvo nas margens.
 *
 * O mosaico e' medido pelas duas dimensoes: a largura vem das colunas, a altura das
 * linhas, e ficam-se com as duas (nao e' quadrado). A fotografia enche o mosaico a
 * `Fit`, e a faixa do nome assenta por cima do terco de baixo — que nos cartazes e'
 * margem roxa, porque o produto e' composto alto de proposito (recortar-flores.py,
 * CENTRO 0,33). Nada do produto fica tapado.
 *
 * Ja se experimentou uma seccao de cada vez, com mosaicos ao dobro: o Francisco
 * quis as tres a vista. Fica registado que com 24 mosaicos em 880x408dp numa TV a
 * 1080p este e' o tecto: a cabeca sai com ~110px. Mais so tirando referencias do
 * ecra.
 */
@Composable
fun TileGrid(
    items: List<MenuItem>,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = Espaco.medio,
    tile: @Composable (item: MenuItem, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.focusGroup(), contentAlignment = Alignment.Center) {
        // columns = 0: escolhe-se o numero de colunas que da o maior mosaico com os artigos
        // todos no ecra (29/9/2026). O mosaico quer-se ao alto (ASPECTO largura/altura),
        // como as fotos; a escolha e' so aritmetica, feita uma vez por medida.
        val n = items.size.coerceAtLeast(1)
        val colunas = if (columns > 0) columns else melhoresColunas(n, maxWidth, maxHeight, gap)
        val linhas = items.chunked(colunas)
        val nLinhas = linhas.size.coerceAtLeast(1)
        var alto = (maxHeight - gap * (nLinhas - 1)) / nLinhas
        var largo = (maxWidth - gap * (colunas - 1)) / colunas
        if (columns <= 0) { if (largo > alto * ASPECTO) largo = alto * ASPECTO else alto = minOf(alto, largo / ASPECTO) }

        Column(verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)) {
            linhas.forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                    linha.forEach { item -> tile(item, Modifier.width(largo).height(alto)) }
                }
            }
        }
    }
}

/** Largura sobre altura do mosaico na grelha automatica: ao alto, como as fotos dos produtos. */
private const val ASPECTO = 0.78f

/** Quantas colunas dao o maior mosaico com [n] artigos numa area de [w] por [h]. */
private fun melhoresColunas(n: Int, w: Dp, h: Dp, gap: Dp): Int {
    var melhor = 1; var maior = 0f
    for (c in 1..n) {
        val l = (n + c - 1) / c
        val largo = ((w - gap * (c - 1)) / c).value
        val alto = ((h - gap * (l - 1)) / l).value
        if (largo <= 0f || alto <= 0f) continue
        val lado = minOf(largo, alto * ASPECTO)
        if (lado > maior + 0.5f) { maior = lado; melhor = c }
    }
    return melhor
}
