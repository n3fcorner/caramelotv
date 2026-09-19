package pt.caramelo.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import pt.caramelo.tv.R

/**
 * Fundo da marca, partilhado pelos tres ecras para nao haver duas ideias de fundo.
 *
 * Era montado aqui em duas camadas — um padrao desenhado em codigo mais a mascote
 * em PNG. Passou a ser **uma arte unica**, feita de proposito para a montra: o roxo
 * com textura, o padrao geometrico a ouro nos cantos oposto a oposto, e a mascote
 * encostada a direita. Desenhar isto em codigo nunca ia chegar la, e com a arte
 * pronta as duas camadas so serviam para poder divergir dela.
 *
 * Fica a sangrar: a arte e 16:9 como o painel, mas usa-se [ContentScale.Crop] para
 * que num ecra de outra proporcao corte em vez de deixar barras.
 *
 * Nada disto pode disputar atencao com o produto.
 */
@Composable
fun FundoDaMarca(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds(),
    ) {
        Image(
            painter = painterResource(R.drawable.fundo_marca),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
