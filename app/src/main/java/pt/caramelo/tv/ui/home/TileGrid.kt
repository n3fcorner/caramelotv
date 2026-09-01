package pt.caramelo.tv.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.theme.Espaco

/**
 * Grelha de tamanho fixo, com mosaicos quadrados.
 *
 * Row/Column em vez de LazyVerticalGrid: com contagens conhecidas nao ha ganho em ser
 * lazy, nenhum mosaico sai de composicao, e a pesquisa de foco 2D do Compose fica
 * previsivel — o D-pad nunca perde o alvo nas margens.
 *
 * Os mosaicos sao dimensionados pela altura da linha e mantidos a 1:1, que e o formato
 * do media depois do recorte. A folga horizontal que sobra fica repartida pelos dois
 * lados, para ler como margem e nao como desalinhamento.
 */
@Composable
fun TileGrid(
    items: List<MenuItem>,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = Espaco.medio,
    tile: @Composable (item: MenuItem, modifier: Modifier) -> Unit,
) {
    Column(
        modifier = modifier.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        items.chunked(columns).forEach { linha ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            ) {
                linha.forEach { item ->
                    tile(item, Modifier.fillMaxHeight().aspectRatio(1f))
                }
            }
        }
    }
}
