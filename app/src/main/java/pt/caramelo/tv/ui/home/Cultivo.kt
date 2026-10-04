package pt.caramelo.tv.ui.home

import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import pt.caramelo.tv.ui.i18n.termo
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import pt.caramelo.tv.ui.theme.Roxo
import pt.caramelo.tv.ui.theme.Verde
import pt.caramelo.tv.ui.theme.AzulReal
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.TintaFraca
import androidx.compose.foundation.shape.RoundedCornerShape
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.VerdeClaro

/**
 * Os cultivos (os tres que a Caramelo trabalha e o outdoor que o painel aceita), cada um com o seu desenho e a sua cor
 * no canto do mosaico. As cores sao as da marca: amarelo (luz artificial) para indoor, verde
 * (a estufa) para greenhouse e creme (o vidro) para glasshouse. Um cultivo fora
 * desta lista, ou vazio, nao leva ponto: melhor sem marca do que com marca errada.
 */
enum class Cultivo(val nome: String, val cor: Color) {
    // As cores do site (1/10/2026): indoor roxo, estufa verde, vidro azul, exterior laranja; leem-se no papel e no escuro.
    INDOOR("Indoor", Roxo),
    GREENHOUSE("Greenhouse", Verde),
    GLASSHOUSE("Glasshouse", AzulReal),
    OUTDOOR("Outdoor", Laranja);

    companion object {
        fun de(item: MenuItem): Cultivo? {
            val texto = item.strain.cultivation.trim()
            return entries.firstOrNull { it.nome.equals(texto, ignoreCase = true) }
        }
    }
}

/**
 * O desenho do cultivo, o mesmo da loja e da montra (4/10/2026, Francisco: "os icones estao muito triviais e banais"): o mesmo
 * rebento de duas folhas redondas (nunca uma folha que lembre a planta) no seu sitio: debaixo do candeeiro de cultivo, no tunel de
 * plastico, na casa de vidro e ao sol, na colina. Os caminhos sao os do pictogramas.js do site, na grelha de 48, so traco.
 * [cor] muda no foco: o mosaico enche-se da cor da seccao e o desenho na mesma cor desaparecia (4/10/2026).
 */
private val parser = PathParser()
private fun caminho(d: String): Path = parser.parsePathString(d).toPath().also { parser.clear() }
private val REBENTO = listOf("M0 0v-13", "M0-7c-1.5-5.4-6-7.8-11-7.6c.6 5.2 4.6 8.4 11 7.6", "M0-10.4c1.6-5 5.8-7.2 10.4-7c-.6 4.8-4.4 7.8-10.4 7").map(::caminho)
private class Desenho(val linhas: List<Path>, val rebento: Triple<Float, Float, Float>)
private val DESENHOS: Map<Cultivo, Desenho> by lazy {
    mapOf(
        Cultivo.INDOOR to Desenho(listOf("M24 2v4", "M13.5 13c0-4 4.7-6.8 10.5-6.8s10.5 2.8 10.5 6.8z", "M18.5 17.5l-1.8 2.6M24 17.5v3.2M29.5 17.5l1.8 2.6", "M16.5 37.5h15l-2 7h-11z").map(::caminho), Triple(24f, 37.5f, 0.78f)),
        Cultivo.GREENHOUSE to Desenho(listOf("M4 43V31a20 19 0 0 1 40 0v12", "M12 43V20M36 43V20", "M2 43h44").map(::caminho), Triple(24f, 43f, 0.78f)),
        Cultivo.GLASSHOUSE to Desenho(listOf("M5 43V21L24 6l19 15v22", "M12 16v27M36 16v27M5 31h7M36 31h7", "M2 43h44").map(::caminho), Triple(24f, 43f, 0.78f)),
        Cultivo.OUTDOOR to Desenho(listOf("M16 11.5a4.5 4.5 0 1 0-9 0a4.5 4.5 0 1 0 9 0", "M11.5 2.5v2M11.5 18.5v2M2.5 11.5h2M18.5 11.5h2M5.1 5.1l1.4 1.4M16.5 16.5l1.4 1.4M17.9 5.1l-1.4 1.4", "M2 44c6-9 14-13.5 22-13.5S40 35 46 44").map(::caminho), Triple(30f, 30.8f, 0.82f)),
    )
}

@Composable
fun PontoDeCultivo(cultivo: Cultivo, modifier: Modifier = Modifier, tamanho: Dp = 24.dp, cor: Color = cultivo.cor) {
    Canvas(modifier.size(tamanho)) {
        val k = size.minDimension / 48f
        val d = DESENHOS.getValue(cultivo)
        val largura = 3.6f
        scale(k, pivot = Offset.Zero) {
            val traco = Stroke(width = largura, cap = StrokeCap.Round, join = StrokeJoin.Round)
            d.linhas.forEach { drawPath(it, cor, style = traco) }
            val (x, y, f) = d.rebento
            translate(x, y) {
                scale(f, pivot = Offset.Zero) {
                    val t = Stroke(width = largura / f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    REBENTO.forEach { drawPath(it, cor, style = t) }
                }
            }
        }
    }
}

/** Legenda dos pontos, para o fundo da grelha, numa caixa cinzenta translucida. */
@Composable
fun LegendaDeCultivo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Tinta.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(1.5.dp, Tinta.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = Espaco.medio, vertical = 6.dp),
        // Numa linha so (4/10/2026): com o espacamento largo o OUTDOOR partia em OUTD / OOR ao lado dos separadores.
        horizontalArrangement = Arrangement.spacedBy(Espaco.medio),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cultivo.entries.forEach { cultivo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoDeCultivo(cultivo, tamanho = 18.dp)
                Text(
                    text = cultivo.nome.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TintaFraca,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = Espaco.minimo),
                )
            }
        }
    }
}
