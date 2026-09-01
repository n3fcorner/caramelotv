package pt.caramelo.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import pt.caramelo.tv.R
import pt.caramelo.tv.ui.theme.Lilas

/**
 * Fundo da marca, partilhado pelos tres ecras para nao haver duas ideias de fundo.
 *
 * As duas camadas sao formas cheias, em tom sobre tom, e nao contornos: com linhas
 * a mascote e o logotipo cruzavam-se e o fundo virava um emaranhado. Os PNG ja vem
 * em monocromatico roxo, por isso nao levam tinta por cima — o relevo vem da propria
 * ilustracao.
 *
 *  1. o logotipo geometrico, ampliado ate sair do ecra, confinado a esquerda;
 *  2. a mascote, encostada ao canto direito e a sangrar pelos bordos.
 *
 * Nada disto pode disputar atencao com o produto.
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
        // Padrao espalhado em vez do logotipo ampliado: as pecas de que o logo e
        // feito, giradas e de tamanhos varios. Le como textura e nao entra em
        // conflito com a mascote.
        if (comMarca) {
            PadraoCaramelo(
                cor = Lilas.copy(alpha = 0.13f),
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (comMascote) {
            Image(
                painter = painterResource(R.drawable.mascote_fundo),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                alpha = 0.85f,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // A altura do ecra e o limite; o que sobra em largura sangra pelo
                    // bordo direito, como um carimbo cortado pela moldura.
                    .fillMaxHeight()
                    .offset(x = 168.dp, y = 30.dp),
            )
        }
    }
}
