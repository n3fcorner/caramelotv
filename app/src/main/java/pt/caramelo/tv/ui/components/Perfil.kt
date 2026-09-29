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
    val peso = strain.indica.coerceIn(0, 100)
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
@Composable
fun TerpenosDominantes(terpenos: List<String>, modifier: Modifier = Modifier) {
    val lista = terpenos.filter { it.isNotBlank() }
    if (lista.isEmpty()) return
    Column(modifier) {
        Text(tr("TERPENOS DOMINANTES", "DOMINANT TERPENES"), style = MaterialTheme.typography.labelSmall, color = Amarelo, letterSpacing = 2.sp)
        Row(
            modifier = Modifier.padding(top = Espaco.curto),
            horizontalArrangement = Arrangement.spacedBy(Espaco.curto),
        ) {
            lista.forEach { nome ->
                Text(
                    text = termo(nome),
                    style = MaterialTheme.typography.labelMedium,
                    color = Creme,
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colorScheme.surface)
                        .padding(horizontal = Espaco.medio, vertical = Espaco.minimo),
                )
            }
        }
        Text(
            text = tr("Segundo o produtor ou a genética.", "According to the grower or the genetics."),
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
