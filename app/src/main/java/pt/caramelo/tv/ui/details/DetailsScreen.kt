package pt.caramelo.tv.ui.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import pt.caramelo.tv.data.model.ItemState
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.FundoDaMarca
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.components.formatarTempo
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.RoxoProfundo

/**
 * Ficha do produto: cartaz quadrado a esquerda, texto a direita — a mesma composicao
 * do leitor, de proposito.
 *
 * A ficha diz o que se ve e se cheira. Sem texto corrido: e materia regulada
 * e tem de sair da marca.
 */
@Composable
fun DetailsScreen(
    item: MenuItem,
    posterUrl: String,
    resumePositionMs: Long,
    onPlay: (resume: Boolean) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val primeiroBotao = remember { FocusRequester() }
    LaunchedEffect(item.id, item.hasVideo, resumePositionMs > 0) {
        withFrameNanos { }
        runCatching { primeiroBotao.requestFocus() }
    }

    var entrou by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) { entrou = true }
    val ficha by animateFloatAsState(
        targetValue = if (entrou) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = 90),
        label = "entrada-ficha",
    )

    Box(Modifier.fillMaxSize()) {
    FundoDaMarca()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
        horizontalArrangement = Arrangement.spacedBy(Espaco.seccao),
    ) {
        Cartaz(
            item = item,
            posterUrl = posterUrl,
            modifier = Modifier.weight(0.56f).fillMaxHeight(),
        )

        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = ficha
                    // Entra a deslizar da direita, o sentido de leitura da ficha.
                    translationX = (1f - ficha) * 48.dp.toPx()
                },
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(22.dp).height(3.dp).background(Amarelo))
                Text(
                    text = item.section.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amarelo,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = Espaco.curto),
                )
            }

            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.displayMedium,
                color = Creme,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Espaco.curto),
            )

            val ficheiro = listOfNotNull(
                item.strain.format.takeIf { it.isNotBlank() },
                item.strain.cultivation.takeIf { it.isNotBlank() },
                "Infundido".takeIf { item.strain.infused },
            )
            if (ficheiro.isNotEmpty()) {
                Text(
                    text = ficheiro.joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Espaco.minimo),
                )
            }

            if (item.strain.aroma.isNotBlank()) {
                Text(
                    text = "AROMA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Amarelo,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = Espaco.largo),
                )
                Text(
                    text = item.strain.aroma,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Creme,
                    modifier = Modifier.padding(top = Espaco.minimo),
                )
            }

            Row(
                modifier = Modifier.padding(top = Espaco.seccao),
                horizontalArrangement = Arrangement.spacedBy(Espaco.curto),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val podeVer = item.hasVideo
                if (podeVer && resumePositionMs > 0) {
                    TvButton(
                        text = "Retomar em ${formatarTempo(resumePositionMs)}",
                        onClick = { onPlay(true) },
                        primary = true,
                        focusRequester = primeiroBotao,
                    )
                    TvButton(text = "Do início", onClick = { onPlay(false) })
                } else if (podeVer) {
                    TvButton(
                        text = "Reproduzir",
                        onClick = { onPlay(false) },
                        primary = true,
                        focusRequester = primeiroBotao,
                    )
                }
                TvButton(
                    text = "Editar",
                    onClick = onEdit,
                    primary = !podeVer,
                    focusRequester = if (podeVer) null else primeiroBotao,
                )
                TvButton(text = "Voltar", onClick = onBack)
            }
        }
    }
    }
}

@Composable
private fun Cartaz(item: MenuItem, posterUrl: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val forma = Modifier
            .fillMaxHeight()
            .aspectRatio(item.orientation.aspect)
            .hero(item.id)

        if (posterUrl.isBlank()) {
            Box(
                modifier = forma.background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (item.state) {
                        ItemState.EM_BREVE -> "EM BREVE"
                        else -> "SLOT LIVRE"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Amarelo.copy(alpha = 0.5f),
                    letterSpacing = 4.sp,
                )
            }
        } else {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = forma.background(RoxoProfundo),
            )
        }
    }
}
