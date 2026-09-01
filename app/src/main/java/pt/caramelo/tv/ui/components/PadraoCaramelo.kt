package pt.caramelo.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

/**
 * Padrao de fundo feito das pecas de que o logotipo da Caramelo do Brasil e
 * construido: quartos de circulo, meios circulos, quadrados, triangulos, circulos
 * e a folha. Espalhadas, giradas e de tamanhos diferentes, para ler como textura
 * e nao como um logotipo ampliado.
 *
 * A semente e fixa. "Aleatorio" aqui quer dizer que nao ha grelha nem simetria a
 * dar nas vistas — nao que mude de cada vez: um padrao que se remexe a cada
 * recomposicao daria um fundo a tremer.
 */
@Composable
fun PadraoCaramelo(
    cor: Color,
    modifier: Modifier = Modifier,
    pecas: Int = 38,
    semente: Int = 20260901,
) {
    Canvas(modifier) { desenha(cor, pecas, semente) }
}

private fun DrawScope.desenha(cor: Color, pecas: Int, semente: Int) {
    val r = Random(semente)
    val menor = minOf(size.width, size.height)

    repeat(pecas) {
        val cx = r.nextFloat() * size.width
        val cy = r.nextFloat() * size.height
        val d = menor * (0.05f + r.nextFloat() * 0.17f)
        val giro = r.nextFloat() * 360f
        // Alguma variacao de peso, ou o padrao fica chapado.
        val tinta = cor.copy(alpha = cor.alpha * (0.45f + r.nextFloat() * 0.55f))
        val canto = Offset(cx - d / 2f, cy - d / 2f)

        rotate(degrees = giro, pivot = Offset(cx, cy)) {
            when (r.nextInt(6)) {
                0 -> drawRect(tinta, topLeft = canto, size = Size(d, d))
                1 -> drawCircle(tinta, radius = d / 2f, center = Offset(cx, cy))
                // quarto de circulo: o modulo das orelhas e das patas
                2 -> drawArc(
                    color = tinta,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(cx - d, cy - d),
                    size = Size(d * 2f, d * 2f),
                )
                // meio circulo: a cabeca
                3 -> drawArc(
                    color = tinta,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = canto,
                    size = Size(d, d),
                )
                // triangulo: o focinho
                4 -> drawPath(
                    path = Path().apply {
                        moveTo(cx, cy - d / 2f)
                        lineTo(cx + d / 2f, cy + d / 2f)
                        lineTo(cx - d / 2f, cy + d / 2f)
                        close()
                    },
                    color = tinta,
                )
                else -> folha(cx, cy, d, tinta)
            }
        }
    }
}

/** A folha do logotipo, reduzida a cinco foliolos que irradiam do mesmo ponto. */
private fun DrawScope.folha(cx: Float, cy: Float, d: Float, cor: Color) {
    val comprimento = d * 0.62f
    val largura = d * 0.17f
    for (i in -2..2) {
        rotate(degrees = i * 26f, pivot = Offset(cx, cy + comprimento / 2f)) {
            drawOval(
                color = cor,
                topLeft = Offset(cx - largura / 2f, cy - comprimento / 2f),
                size = Size(largura, comprimento),
            )
        }
    }
}
