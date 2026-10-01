package pt.caramelo.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import pt.caramelo.tv.ui.theme.Papel

/**
 * O papel do site (1/10/2026, "no mesmo estilo do site e do manual"): o bege do manual, com uma luz suave de cima
 * e o canto de baixo um nada mais quente, para nao ser um bege chapado no ecra grande. So cor, nada desenhado por
 * cima: o produto pousado e' o protagonista, como na loja.
 */
@Composable
fun FundoDePapel(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Papel)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.3f, 0f),
                        radius = size.width * 0.8f,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x14B58A3C)),
                        startY = size.height * 0.5f,
                        endY = size.height,
                    ),
                )
            },
    )
}
