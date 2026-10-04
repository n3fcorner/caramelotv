package pt.caramelo.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

/**
 * A sombra do produto pousado no papel (4/10/2026, Francisco: "as sombras por baixo dos artigos estao pessimas; podes colocar
 * sombras, mas em condicoes"). A de antes era um gradiente redondo dentro de uma caixa oval: via-se a borda da caixa. Esta e' a
 * sombra de contacto de uma fotografia de estudio, em duas camadas: uma larga e muito leve que se perde no papel sem contorno, e
 * um nucleo estreito e mais escuro mesmo debaixo da base. Cada camada e' um circulo com gradiente radial que chega a transparente
 * antes da borda, achatado so no desenho (scale), por isso nunca ha aresta. [levanta] (0 a 1) afasta-a quando o produto sobe.
 */
@Composable
fun SombraDePousar(modifier: Modifier = Modifier, levanta: Float = 0f, tinta: Color = Color(0xFF072645)) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.width / 2f
        val leve = 1f - 0.45f * levanta
        scale(scaleX = 1f, scaleY = 0.16f, pivot = c) {
            drawCircle(
                Brush.radialGradient(
                    0f to tinta.copy(alpha = 0.16f * leve), 0.5f to tinta.copy(alpha = 0.07f * leve), 1f to Color.Transparent,
                    center = c, radius = r,
                ),
                radius = r, center = c,
            )
        }
        val r2 = r * (0.52f - 0.08f * levanta)
        scale(scaleX = 1f, scaleY = 0.13f, pivot = c) {
            drawCircle(
                Brush.radialGradient(
                    0f to tinta.copy(alpha = 0.30f * leve), 0.6f to tinta.copy(alpha = 0.10f * leve), 1f to Color.Transparent,
                    center = c, radius = r2,
                ),
                radius = r2, center = c,
            )
        }
    }
}
