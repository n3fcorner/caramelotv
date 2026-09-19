package pt.caramelo.tv.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import pt.caramelo.tv.data.model.ItemState
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.BarraIndicaSativa
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Movimento
import pt.caramelo.tv.ui.theme.RoxoProfundo

/**
 * Mosaico de produto. So tipografia sobre a foto — descricao, aroma e cultivo vivem
 * na ficha, nao aqui.
 *
 * O nome cresce com o foco em vez de aparecer e desaparecer: em repouso e pequeno e
 * muito espacejado, e ao ganhar foco fecha o espacejamento, cresce e recebe uma
 * regua amarela. Fica sempre legivel de longe, mas o item focado destaca-se pela
 * hierarquia e nao por ser o unico com texto.
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
) {
    var focused by remember { mutableStateOf(false) }
    val mola by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = Movimento.foco,
        label = "foco-mosaico",
    )
    // A mola ultrapassa os limites de proposito — e o que da o ressalto na escala.
    // Tudo o resto (paddings, larguras, alfas) tem de ver o valor ja limitado, ou
    // rebenta com "Padding must be non-negative" no primeiro desfoque.
    val p = mola.coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            // Cresce por cima dos vizinhos em vez de ser cortado por eles.
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
        shape = ClickableSurfaceDefaults.shape(shape = RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Creme,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = Creme,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Amarelo), shape = RectangleShape),
        ),
    ) {
        Box(Modifier.fillMaxSize().clipToBounds()) {
            Fotografia(item = item, posterUrl = posterUrl, realce = p)

            if (item.state == ItemState.EM_BREVE) {
                Text(
                    text = "EM BREVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 12.sp),
                    color = RoxoProfundo,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Espaco.minimo)
                        .background(Amarelo)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }

            // O cultivo no canto de cima, fora da faixa e longe do EM BREVE.
            Cultivo.de(item)?.let { cultivo ->
                PontoDeCultivo(
                    cultivo = cultivo,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Espaco.curto),
                )
            }

            // A faixa assenta no terco de baixo do mosaico, que nos cartazes e'
            // margem roxa: o produto e' composto alto (CENTRO 0,33 no
            // recortar-flores.py) precisamente para nada lhe ficar por cima.
            // Faixa solida, nao degrade: sem ela o creme perde-se nas fotos claras.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(RoxoProfundo.copy(alpha = 0.90f))
                    .padding(horizontal = Espaco.curto, vertical = 4.dp),
            ) {
                // A regua abre da esquerda a medida que o foco entra.
                Box(
                    Modifier
                        .padding(bottom = Espaco.minimo * p)
                        .width(26.dp * p)
                        .height(3.dp * p)
                        .background(Amarelo),
                )
                Text(
                    text = item.displayTitle.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = lerp(11.sp, 13.sp, p),
                        lineHeight = lerp(13.5.sp, 16.sp, p),
                        letterSpacing = lerp(1.4.sp, 0.3.sp, p),
                    ),
                    color = Creme.copy(alpha = 0.80f + 0.20f * p),
                    // Duas linhas sempre reservadas: com nomes de uma e de duas
                    // linhas misturados, as faixas ficavam com alturas aos saltos.
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

            }

            // Indica e sativa numa barra fina no fundo, como nos cartoes do telemovel.
            if (item.strain.temPerfil) {
                BarraIndicaSativa(
                    peso = item.strain.indica,
                    altura = 4.dp,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
    }
}

@Composable
private fun Fotografia(item: MenuItem, posterUrl: String, realce: Float) {
    Box(Modifier.fillMaxSize().background(RoxoProfundo)) {
        if (posterUrl.isNotBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                // Fit e nao Crop: o cartaz e' quadrado e a caixa tambem, mas se um
                // dia nao forem, corta-se margem roxa e nunca produto.
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().hero(item.id),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .hero(item.id),
            )
        }

        // Em repouso a foto fica um pouco recolhida; ao focar acende. E o mesmo
        // gesto do nome a crescer, so que na imagem.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f * (1f - realce.coerceIn(0f, 1f)))),
        )
    }
}
