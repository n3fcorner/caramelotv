package pt.caramelo.tv.ui.home

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
 * O desenho do cultivo (29/9/2026, Francisco: "desenhinhos pequenos também no tipo de
 * cultivo"), no mesmo traco dos aromas: linhas simples na cor do cultivo, so o tracado,
 * sem disco nem fundo (Francisco, 30/9/2026: "todos os desenhos sao so o tracado"). Indoor e' a lampada
 * pendurada com a luz, greenhouse o tunel de estufa, glasshouse a casa de vidro com
 * as vidracas. Canvas, sem imagens: nitido em qualquer TV e sem memoria a mais.
 */
@Composable
fun PontoDeCultivo(cultivo: Cultivo, modifier: Modifier = Modifier, tamanho: Dp = 24.dp) {
    Canvas(modifier.size(tamanho)) {
        val w = size.minDimension
        val m = w * 0.06f // margem
        val a = w - 2 * m // lado da area do desenho
        fun p(x: Float, y: Float) = Offset(m + x * a, m + y * a)
        val traco = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cor = cultivo.cor
        when (cultivo) {
            Cultivo.INDOOR -> {
                drawLine(cor, p(0.5f, 0f), p(0.5f, 0.2f), traco.width, StrokeCap.Round)
                drawPath(Path().apply { moveTo(p(0.28f, 0.52f).x, p(0.28f, 0.52f).y); lineTo(p(0.38f, 0.2f).x, p(0.38f, 0.2f).y); lineTo(p(0.62f, 0.2f).x, p(0.62f, 0.2f).y); lineTo(p(0.72f, 0.52f).x, p(0.72f, 0.52f).y); close() }, cor, style = traco)
                listOf(Triple(0.5f, 0.66f, 0.92f), Triple(0.3f, 0.64f, 0.84f), Triple(0.7f, 0.64f, 0.84f)).forEach { (x, y0, y1) ->
                    val dx = (x - 0.5f) * 0.5f
                    drawLine(cor, p(x, y0), p(x + dx, y1), traco.width, StrokeCap.Round)
                }
            }
            Cultivo.GREENHOUSE -> {
                drawArc(cor, 180f, 180f, false, topLeft = p(0.02f, 0.2f), size = Size(a * 0.96f, a * 1.3f), style = traco)
                drawArc(cor, 180f, 180f, false, topLeft = p(0.3f, 0.36f), size = Size(a * 0.4f, a * 0.9f), style = traco)
                drawLine(cor, p(0f, 0.86f), p(1f, 0.86f), traco.width, StrokeCap.Round)
            }
            Cultivo.OUTDOOR -> {
                // O sol: o disco e os oito raios
                drawCircle(cor, radius = a * 0.2f, center = p(0.5f, 0.5f), style = traco)
                for (k in 0 until 8) {
                    val ang = Math.toRadians(k * 45.0)
                    val cx = kotlin.math.cos(ang).toFloat(); val cy = kotlin.math.sin(ang).toFloat()
                    drawLine(cor, p(0.5f + cx * 0.33f, 0.5f + cy * 0.33f), p(0.5f + cx * 0.47f, 0.5f + cy * 0.47f), traco.width, StrokeCap.Round)
                }
            }
            Cultivo.GLASSHOUSE -> {
                drawPath(Path().apply { moveTo(p(0.06f, 0.92f).x, p(0.06f, 0.92f).y); lineTo(p(0.06f, 0.42f).x, p(0.06f, 0.42f).y); lineTo(p(0.5f, 0.06f).x, p(0.5f, 0.06f).y); lineTo(p(0.94f, 0.42f).x, p(0.94f, 0.42f).y); lineTo(p(0.94f, 0.92f).x, p(0.94f, 0.92f).y); close() }, cor, style = traco)
                drawLine(cor, p(0.5f, 0.06f), p(0.5f, 0.92f), traco.width * 0.7f, StrokeCap.Round)
                drawLine(cor, p(0.06f, 0.66f), p(0.94f, 0.66f), traco.width * 0.7f, StrokeCap.Round)
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
            .padding(horizontal = Espaco.medio, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Espaco.largo),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cultivo.entries.forEach { cultivo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoDeCultivo(cultivo, tamanho = 20.dp)
                Text(
                    text = cultivo.nome.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TintaFraca,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = Espaco.curto),
                )
            }
        }
    }
}
