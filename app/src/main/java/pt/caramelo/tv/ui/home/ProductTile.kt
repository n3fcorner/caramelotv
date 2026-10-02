package pt.caramelo.tv.ui.home

import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.PapelSombra
import pt.caramelo.tv.ui.theme.Roxo
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.TintaFraca
import pt.caramelo.tv.ui.theme.Vermelho
import pt.caramelo.tv.data.model.ItemState
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Movimento

/**
 * Mosaico de produto, no estilo do site (1/10/2026, Francisco: "no mesmo estilo do site e do manual"): o recorte da
 * flor pousado no papel, sem caixa, com a sombra por baixo, e o nome em tinta por baixo da foto. O esgotado leva o
 * carimbo SOLD OUT por cima e a foto esbatida; o que ainda nao chegou, a etiqueta EM BREVE.
 *
 * O foco: o mosaico cresce com a mola da casa, a foto levanta um pouco, o cartao enche-se da cor da seccao com a orla
 * amarela e a regua amarela abre por baixo do nome, que passa a bege (2/10/2026, regras de cor em Theme.kt).
 */
@Composable
fun ProductTile(
    item: MenuItem,
    posterUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    /** A cor da seccao (regra 2): o cartao assenta nela em repouso e enche-se dela com o foco. */
    cor: Color = Laranja,
) {
    var focused by remember { mutableStateOf(false) }
    val mola by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = Movimento.foco,
        label = "foco-mosaico",
    )
    // A mola ultrapassa os limites de proposito (o ressalto na escala); o resto ve o valor limitado.
    val p = mola.coerceIn(0f, 1f)
    val forma = RoundedCornerShape(8.dp)

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .zIndex(if (p > 0.001f) 1f else 0f)
            .graphicsLayer {
                val escala = 1f + (Movimento.ESCALA_FOCO - 1f) * mola
                scaleX = escala
                scaleY = escala
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = forma),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = cor.copy(alpha = 0.10f),
            contentColor = Tinta,
            focusedContainerColor = cor,
            focusedContentColor = Creme,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            // Regra 3: o foco e' amarelo.
            focusedBorder = Border(BorderStroke(4.dp, Amarelo), shape = forma),
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(Espaco.curto)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Fotografia(item = item, posterUrl = posterUrl, levanta = p)

                if (item.esgotado) {
                    Carimbo(Modifier.align(Alignment.Center))
                } else if (item.state == ItemState.EM_BREVE) {
                    Text(
                        text = tr("EM BREVE", "COMING SOON"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = Papel,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(cor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                // O cultivo no canto de cima, em traco, como no site.
                Cultivo.de(item)?.let { cultivo ->
                    PontoDeCultivo(
                        cultivo = cultivo,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }

            // A regua abre da esquerda a medida que o foco entra.
            Box(
                Modifier
                    .padding(top = Espaco.minimo)
                    .width(30.dp * p)
                    .height(3.dp)
                    .background(Amarelo.copy(alpha = p)),
            )
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = lerp(15.sp, 17.sp, p),
                    lineHeight = lerp(18.sp, 20.sp, p),
                    letterSpacing = 0.sp,
                ),
                color = when {
                    focused -> Creme
                    item.esgotado -> TintaFraca
                    else -> Tinta
                },
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Espaco.minimo),
            )
        }
    }
}

/** O carimbo de borracha: vermelho do manual, rodado, com o papel a passar por baixo. */
@Composable
private fun Carimbo(modifier: Modifier = Modifier) {
    Text(
        text = "SOLD OUT",
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 22.sp),
        color = Vermelho,
        letterSpacing = 2.sp,
        modifier = modifier
            .graphicsLayer { rotationZ = -14f }
            .background(Papel.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .border(3.dp, Vermelho, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun Fotografia(item: MenuItem, posterUrl: String, levanta: Float) {
    Box(Modifier.fillMaxSize()) {
        // A sombra do produto pousado: uma elipse suave por baixo, que se afasta quando a foto levanta.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.62f)
                .height(14.dp)
                .graphicsLayer { alpha = 1f - 0.4f * levanta; scaleX = 1f - 0.12f * levanta }
                .background(Brush.radialGradient(listOf(PapelSombra, Color.Transparent)), RoundedCornerShape(50)),
        )
        if (posterUrl.isNotBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                // Fit: o recorte inteiro, nunca cortado; assenta no fundo, como na loja.
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 6.dp)
                    .graphicsLayer {
                        translationY = -6.dp.toPx() * levanta
                        alpha = if (item.esgotado) 0.6f else 1f
                    }
                    .hero(item.id),
                colorFilter = if (item.esgotado) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) }) else null,
            )
        } else {
            Box(Modifier.fillMaxSize().hero(item.id))
        }
    }
}
