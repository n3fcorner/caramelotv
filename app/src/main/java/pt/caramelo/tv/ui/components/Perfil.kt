package pt.caramelo.tv.ui.components

import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Lilas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.Strain
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.CremeFraco
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.RoxoProfundo
import pt.caramelo.tv.ui.theme.VerdeClaro

/**
 * O grafismo de indica e sativa, o mesmo da montra do telemovel: amarelo para a
 * indica, verde para a sativa, os dois numeros nas pontas e o tipo em cima.
 * So se desenha com peso documentado — uma barra a meio para toda a gente seria
 * decoracao, nao dado.
 */
@Composable
fun PerfilIndicaSativa(strain: Strain, modifier: Modifier = Modifier) {
    if (!strain.temPerfil) return
    val peso = strain.indicaEfectiva.coerceIn(0, 100)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Espaco.medio, vertical = Espaco.curto),
    ) {
        // Uma linha so: o numero da indica a esquerda, o tipo ao meio, o da sativa
        // a direita. Num ecra de TV a ficha nao tem altura para mais.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val indicaTxt = termo("Índica"); val sativaTxt = termo("Sativa")
            Text(
                buildAnnotatedString {
                    append("$indicaTxt ")
                    withStyle(SpanStyle(color = Creme, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)) { append("$peso") }
                },
                style = MaterialTheme.typography.labelMedium,
                color = CremeFraco,
            )
            Text(
                text = (if (strain.type.isBlank()) tr("Perfil", "Profile") else termo(strain.type)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Amarelo,
                letterSpacing = 2.sp,
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Creme, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)) { append("${100 - peso}") }
                    append(" $sativaTxt")
                },
                style = MaterialTheme.typography.labelMedium,
                color = CremeFraco,
            )
        }
        BarraIndicaSativa(peso, altura = 8.dp, modifier = Modifier.padding(top = Espaco.curto))
        if (strain.perfilEstimado) {
            // Sem percentagem publicada: o peso sai do tipo, e diz-se.
            Text(
                text = tr("Estimado pelo tipo.", "Estimated from the type."),
                style = MaterialTheme.typography.labelSmall,
                color = CremeFraco,
                modifier = Modifier.padding(top = Espaco.minimo),
            )
        }
    }
}

/** A barra em si; tambem serve, fina, no fundo de cada mosaico. */
@Composable
fun BarraIndicaSativa(peso: Int, altura: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(altura),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.weight(peso.coerceAtLeast(1).toFloat()).fillMaxHeight().background(Amarelo))
        Box(Modifier.weight((100 - peso).coerceAtLeast(1).toFloat()).fillMaxHeight().background(VerdeClaro))
    }
}

/** Os terpenos dominantes em etiquetas, com a origem dita por baixo. */
/**
 * Os terpenos em grafico de barras (29/9/2026, pedido do Leo). Com o perfil publicado
 * pelo produtor, cada barra e' a percentagem e o numero vai ao lado; sem ele, a barra
 * diz so a ordem de dominancia — mais comprida a primeira — e nao ha numeros, para
 * nao parecer um dado que nao temos.
 */
@Composable
fun TerpenosDominantes(terpenos: List<String>, percentagens: List<Int> = emptyList(), modifier: Modifier = Modifier) {
    val lista = terpenos.filter { it.isNotBlank() }.take(5)
    if (lista.isEmpty()) return
    val comPct = percentagens.size >= lista.size && percentagens.take(lista.size).all { it > 0 }
    val maximo = if (comPct) percentagens.take(lista.size).max().toFloat() else 1f
    val ORDEM = listOf(1f, 0.72f, 0.5f, 0.36f, 0.26f)
    val CORES = listOf(Amarelo, Laranja, VerdeClaro, Lilas, Creme)
    Column(modifier) {
        Text(tr("TERPENOS DOMINANTES", "DOMINANT TERPENES"), style = MaterialTheme.typography.labelSmall, color = Amarelo, letterSpacing = 2.sp)
        Column(Modifier.padding(top = Espaco.curto), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lista.forEachIndexed { i, nome ->
                val fraccao = if (comPct) percentagens[i] / maximo else ORDEM[i]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = termo(nome),
                        style = MaterialTheme.typography.labelMedium,
                        color = Creme,
                        maxLines = 1,
                        modifier = Modifier.width(118.dp),
                    )
                    Box(Modifier.weight(1f).height(10.dp)) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight().background(RoxoProfundo.copy(alpha = 0.6f)))
                        Box(Modifier.fillMaxWidth(fraccao.coerceIn(0.04f, 1f)).fillMaxHeight().background(CORES[i % CORES.size]))
                    }
                    if (comPct) {
                        Text(
                            text = "${percentagens[i]}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = CremeFraco,
                            modifier = Modifier.width(40.dp).padding(start = Espaco.curto),
                        )
                    }
                }
            }
        }
        Text(
            text = if (comPct) tr("Perfil publicado pelo produtor, em % dos terpenos.", "Profile published by the grower, as % of terpenes.")
            else tr("Por ordem de dominância, segundo o produtor ou a genética.", "In order of dominance, according to the grower or the genetics."),
            style = MaterialTheme.typography.labelSmall,
            color = CremeFraco,
            maxLines = 1,
            modifier = Modifier.padding(top = Espaco.minimo),
        )
    }
}

/** Amostra de cor para a legenda: um traco curto. */
@Composable
fun AmostraDeCor(cor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier.height(4.dp).background(cor))
}

@Suppress("unused")
private val fundoDaBarra = RoxoProfundo
