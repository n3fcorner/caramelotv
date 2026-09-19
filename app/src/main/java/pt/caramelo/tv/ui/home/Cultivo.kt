package pt.caramelo.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import pt.caramelo.tv.ui.components.AmostraDeCor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.CremeFraco
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.RoxoProfundo
import pt.caramelo.tv.ui.theme.VerdeClaro
import androidx.compose.foundation.shape.CircleShape

/**
 * Os tres cultivos que a Caramelo trabalha, cada um com a sua cor no canto do
 * mosaico. As cores sao as da marca: amarelo (luz artificial) para indoor, verde
 * (a estufa) para greenhouse e creme (o vidro) para glasshouse. Um cultivo fora
 * desta lista, ou vazio, nao leva ponto: melhor sem marca do que com marca errada.
 */
enum class Cultivo(val nome: String, val cor: Color) {
    INDOOR("Indoor", Amarelo),
    GREENHOUSE("Greenhouse", VerdeClaro),
    GLASSHOUSE("Glasshouse", Creme);

    companion object {
        fun de(item: MenuItem): Cultivo? {
            val texto = item.strain.cultivation.trim()
            return entries.firstOrNull { it.nome.equals(texto, ignoreCase = true) }
        }
    }
}

/** O ponto em si. O contorno na cor da faixa garante que se le sobre uma flor clara. */
@Composable
fun PontoDeCultivo(cultivo: Cultivo, modifier: Modifier = Modifier, tamanho: Dp = 12.dp) {
    Box(
        modifier
            .size(tamanho)
            .background(cultivo.cor, CircleShape)
            .border(2.dp, RoxoProfundo, CircleShape),
    )
}

/** Legenda dos pontos, para o fundo da grelha, numa caixa cinzenta translucida. */
@Composable
fun LegendaDeCultivo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = Espaco.medio, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Espaco.largo),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cultivo.entries.forEach { cultivo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoDeCultivo(cultivo, tamanho = 11.dp)
                Text(
                    text = cultivo.nome.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CremeFraco,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = Espaco.curto),
                )
            }
        }
        // A barra do fundo dos mosaicos: amarelo indica, verde sativa.
        listOf("Índica" to Amarelo, "Sativa" to VerdeClaro).forEach { (nome, cor) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AmostraDeCor(cor, Modifier.width(18.dp))
                Text(
                    text = nome.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CremeFraco,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = Espaco.curto),
                )
            }
        }
    }
}
