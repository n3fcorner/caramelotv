package pt.caramelo.tv.ui.details

import pt.caramelo.tv.ui.home.PontoDeCultivo
import pt.caramelo.tv.ui.home.Cultivo
import pt.caramelo.tv.ui.components.AromasIlustrados
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import pt.caramelo.tv.ui.animation.Entrada
import pt.caramelo.tv.ui.components.PerfilIndicaSativa
import pt.caramelo.tv.ui.components.TerpenosDominantes
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.animation.heroBounds
import pt.caramelo.tv.ui.components.FundoDaMarca
import pt.caramelo.tv.ui.components.FundoDePapel
import pt.caramelo.tv.ui.theme.PapelTheme
import pt.caramelo.tv.ui.theme.PapelSombra
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Roxo
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.corDaSeccao
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.components.formatarTempo
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.RoxoProfundo
import pt.caramelo.tv.ui.theme.VerdeClaro

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

    // A ficha no papel, como a do site (1/10/2026): bege e tinta; o leitor de video continua no escuro.
    PapelTheme {
    Box(Modifier.fillMaxSize()) {
    FundoDePapel()
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
                .verticalScroll(rememberScrollState())
                .graphicsLayer {
                    alpha = ficha
                    // Entra a deslizar da direita, o sentido de leitura da ficha.
                    translationX = (1f - ficha) * 48.dp.toPx()
                },
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(22.dp).height(3.dp).background(corDaSeccao(item.section)))
                Text(
                    text = termo(item.section.label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = corDaSeccao(item.section),
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = Espaco.curto),
                )
            }

            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.displayMedium,
                color = Tinta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = Espaco.curto)
                    // A mesma caixa que o nome tem no leitor: ao carregar em
                    // Reproduzir, o nome fica e o resto troca a volta dele.
                    .heroBounds("titulo-${item.id}"),
            )

            val infundidoTxt = tr("Infundido", "Infused")
            val ficheiro = listOfNotNull(
                item.strain.format.takeIf { it.isNotBlank() }?.let { termo(it) },
                item.strain.cultivation.takeIf { it.isNotBlank() }?.let { termo(it) },
                infundidoTxt.takeIf { item.strain.infused },
            )
            if (ficheiro.isNotEmpty()) {
                // O desenho do cultivo antes da linha, o mesmo do canto do mosaico (29/9/2026).
                Row(Modifier.padding(top = Espaco.minimo), verticalAlignment = Alignment.CenterVertically) {
                    Cultivo.de(item)?.let { PontoDeCultivo(it, Modifier.padding(end = Espaco.curto), tamanho = 22.dp) }
                    Text(
                        text = ficheiro.joinToString(" · "),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Cada campo entra um pouco depois do anterior, por cima do deslize da
            // coluna inteira: le-se como a ficha a compor-se, nao a cair de uma vez.
            if (item.strain.aroma.isNotBlank()) {
                Entrada(ordem = 1) { AromasIlustrados(item.strain.aroma, termo(item.strain.aroma), Modifier.padding(top = Espaco.medio)) }
            }

            if (item.strain.genetics.isNotBlank()) {
                Entrada(ordem = 2) { Campo(tr("Genética", "Genetics"), termo(item.strain.genetics)) }
            }

            if (item.strain.terpenes.isNotEmpty()) {
                Entrada(ordem = 3) { TerpenosDominantes(item.strain.terpenes, item.strain.terpenesPct, Modifier.padding(top = Espaco.medio)) }
            }

            if (item.strain.temPerfil) {
                Entrada(ordem = 4) { PerfilIndicaSativa(item.strain, Modifier.padding(top = Espaco.medio)) }
            }

            Row(
                modifier = Modifier.padding(top = Espaco.seccao),
                horizontalArrangement = Arrangement.spacedBy(Espaco.curto),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val podeVer = item.hasVideo
                if (podeVer && resumePositionMs > 0) {
                    TvButton(
                        text = tr("Retomar em ", "Resume at ") + formatarTempo(resumePositionMs),
                        onClick = { onPlay(true) },
                        primary = true,
                        focusRequester = primeiroBotao,
                    )
                    TvButton(text = tr("Do início", "From the start"), onClick = { onPlay(false) })
                } else if (podeVer) {
                    TvButton(
                        text = tr("Reproduzir", "Play"),
                        onClick = { onPlay(false) },
                        primary = true,
                        focusRequester = primeiroBotao,
                    )
                }
                TvButton(
                    text = tr("Editar", "Edit"),
                    onClick = onEdit,
                    primary = !podeVer,
                    focusRequester = if (podeVer) null else primeiroBotao,
                )
                TvButton(text = tr("Voltar", "Back"), onClick = onBack)
            }
        }
    }
    }
    }
}

/** Rotulo em amarelo mais o valor, o par que se repete por toda a ficha. */
@Composable
private fun Campo(rotulo: String, valor: String) {
    Column(Modifier.padding(top = Espaco.medio)) {
        Text(
            text = rotulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Roxo,
            letterSpacing = 2.sp,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = Tinta,
            modifier = Modifier.padding(top = Espaco.minimo),
        )
    }
}

@Composable
private fun Cartaz(item: MenuItem, posterUrl: String, modifier: Modifier = Modifier) {
    // O recorte inteiro pousado no papel, com a sombra por baixo, como na ficha do site (1/10/2026). Fit e nunca Crop.
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Espaco.largo)
                .fillMaxWidth(0.5f)
                .height(22.dp)
                .background(Brush.radialGradient(listOf(PapelSombra, Color.Transparent)), RoundedCornerShape(50)),
        )
        if (posterUrl.isBlank()) {
            Box(Modifier.fillMaxSize().hero(item.id), contentAlignment = Alignment.Center) {
                Text(
                    text = when (item.state) {
                        ItemState.EM_BREVE -> tr("EM BREVE", "COMING SOON")
                        else -> tr("SLOT LIVRE", "EMPTY SLOT")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Roxo,
                    letterSpacing = 4.sp,
                )
            }
        } else {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize().padding(top = Espaco.largo, bottom = Espaco.seccao).hero(item.id),
            )
        }
    }
}
