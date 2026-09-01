package pt.caramelo.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import pt.caramelo.tv.R
import pt.caramelo.tv.ui.theme.Lilas

/**
 * Fundo da marca, partilhado pelos tres ecras para nao haver duas ideias de fundo.
 *
 * Duas camadas, ambas muito esbatidas de proposito:
 *  1. o proprio logotipo geometrico em contorno, ampliado ate sair do ecra — e o
 *     padrao dos materiais da marca, e nao um conjunto de formas inventadas;
 *  2. a mascote, encostada ao canto e a sangrar pelos bordos.
 *
 * Nada disto pode disputar atencao com o produto: sao texturas, nao ilustracoes.
 */
@Composable
fun FundoDaMarca(
    modifier: Modifier = Modifier,
    comMarca: Boolean = true,
    comMascote: Boolean = true,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds(),
    ) {
        // ContentScale.Crop faz o que os materiais da marca fazem: amplia a marca ate
        // ela nao caber e deixa a moldura corta-la. So se leem fragmentos — arcos,
        // quadrados, o triangulo do focinho.
        //
        // Fica confinada aos 62% da esquerda: a mascote ocupa os 30% da direita, e
        // sobrepostas as duas viravam um emaranhado de linhas.
        if (comMarca) {
            Image(
                painter = painterResource(R.drawable.marca_contorno),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterStart,
                alpha = 0.08f,
                colorFilter = ColorFilter.tint(Lilas),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.62f)
                    .offset(x = (-90).dp),
            )
        }

        if (comMascote) {
            Image(
                painter = painterResource(R.drawable.mascote_contorno),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                alpha = 0.14f,
                colorFilter = ColorFilter.tint(Lilas),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // A altura do ecra e o limite; o que sobra em largura sangra pelo
                    // bordo direito, como um carimbo cortado pela moldura.
                    .fillMaxHeight()
                    .offset(x = 132.dp, y = 40.dp),
            )
        }
    }
}
