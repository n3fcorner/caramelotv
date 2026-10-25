package pt.caramelo.tv.ui.parafernalia

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import pt.caramelo.tv.R
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.ui.components.FundoDePapel
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.FaixaDaMarca
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Movimento
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.PapelTheme
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.TintaFraca
import pt.caramelo.tv.ui.theme.Vermelho

/**
 * A pagina da parafernalia na TV (25/10/2026, Francisco: "integrar a parafernalia na app da tv, especialmente as
 * piteiras e a sua visualizacao 3d"). Pagina propria, separada da flor: uma grelha dos artigos por categoria e, ao
 * escolher um, a peca grande a rodar em loop (o video tirado do modelo 3D) com o nome, a categoria e o preco ao lado.
 *
 * A grelha e' uma coluna de filas horizontais, uma por categoria, como as prateleiras de uma montra. O video so toca
 * na montra do artigo escolhido, nunca varios ao mesmo tempo, para nao pesar nas boxes de TV fracas.
 */
private val LARANJA = Laranja // o acento da parafernalia, como no universo laranja do site

@Composable
fun ParafernaliaScreen(
    parafernalia: List<Parafernalia>,
    imagemDe: (Parafernalia) -> String,
    giroDe: (Parafernalia) -> String,
    onInteracao: () -> Unit = {},
) {
    var aberto by remember { mutableStateOf<Parafernalia?>(null) }

    PapelTheme {
        Box(Modifier.fillMaxSize()) {
            FundoDePapel()
            Column(Modifier.fillMaxSize().padding(bottom = Overscan.vertical)) {
                Cabecalho()
                if (parafernalia.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = tr("Ainda sem artigos.", "No items yet."),
                            style = MaterialTheme.typography.titleMedium,
                            color = TintaFraca,
                        )
                    }
                } else {
                    Grelha(
                        parafernalia = parafernalia,
                        imagemDe = imagemDe,
                        onAbrir = { aberto = it },
                        onInteracao = onInteracao,
                    )
                }
            }

            // A montra do artigo escolhido por cima de tudo. O Voltar fecha-a; so depois e' que o Voltar sai da pagina.
            // key(slug): trocar de artigo cria uma montra nova e larga a anterior (liberta o ExoPlayer no onDispose).
            aberto?.let { item ->
                androidx.compose.runtime.key(item.slug) {
                    BackHandler(enabled = true) { aberto = null }
                    Montra(
                        item = item,
                        imagemUrl = imagemDe(item),
                        giroUrl = giroDe(item),
                    )
                }
            }
        }
    }
}

@Composable
private fun Cabecalho() {
    // A faixa da marca no topo, como no ecra inicial: azul escuro, com o logotipo e o nome da pagina.
    CarameloTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FaixaDaMarca)
                .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.logo_caramelo),
                    contentDescription = "Caramelo do Brasil",
                    modifier = Modifier.height(50.dp),
                )
                Text(
                    text = tr("Parafernália", "Accessories").uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Creme,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = Espaco.largo),
                )
            }
            Text(
                text = tr("Voltar: ◀", "Back: ◀"),
                style = MaterialTheme.typography.labelSmall,
                color = Creme.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun Grelha(
    parafernalia: List<Parafernalia>,
    imagemDe: (Parafernalia) -> String,
    onAbrir: (Parafernalia) -> Unit,
    onInteracao: () -> Unit,
) {
    // Por categoria, pela ordem que o servidor manda (categoria e depois nome).
    val categorias = remember(parafernalia) { parafernalia.groupBy { it.categoria } }
    val primeiro = remember { FocusRequester() }
    var temFoco by remember { mutableStateOf(false) }
    LaunchedEffect(parafernalia.size) {
        withFrameNanos { }
        if (!temFoco) runCatching { primeiro.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { temFoco = it.hasFocus }
            .verticalScroll(rememberScrollState())
            .padding(top = Espaco.largo),
        verticalArrangement = Arrangement.spacedBy(Espaco.largo),
    ) {
        var indiceCategoria = 0
        categorias.forEach { (categoria, itens) ->
            val primeiraFila = indiceCategoria == 0
            indiceCategoria++
            Column(verticalArrangement = Arrangement.spacedBy(Espaco.curto)) {
                Row(
                    modifier = Modifier.padding(start = Overscan.horizontal),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(22.dp).height(3.dp).background(LARANJA))
                    Text(
                        text = categoria.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = LARANJA,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(start = Espaco.curto),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Overscan.horizontal, vertical = Espaco.curto),
                    horizontalArrangement = Arrangement.spacedBy(Espaco.largo),
                ) {
                    itens.forEachIndexed { i, item ->
                        Cartao(
                            item = item,
                            posterUrl = imagemDe(item),
                            onClick = { onAbrir(item) },
                            onFocus = onInteracao,
                            focusRequester = if (primeiraFila && i == 0) primeiro else null,
                            modifier = Modifier.width(196.dp).height(248.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Cartao(
    item: Parafernalia,
    posterUrl: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    var focado by remember { mutableStateOf(false) }
    val mola by animateFloatAsState(if (focado) 1f else 0f, Movimento.foco, label = "foco-paraf")
    val p = mola.coerceIn(0f, 1f)
    val forma = RoundedCornerShape(8.dp)

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                val escala = 1f + (Movimento.ESCALA_FOCO - 1f) * mola
                scaleX = escala; scaleY = escala
            }
            .onFocusChanged { focado = it.isFocused; if (it.isFocused) onFocus() }
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = forma),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = LARANJA.copy(alpha = 0.10f),
            contentColor = Tinta,
            focusedContainerColor = LARANJA,
            focusedContentColor = Creme,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(2.dp, Tinta), shape = forma),
            focusedBorder = Border(BorderStroke(4.dp, Amarelo), shape = forma),
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(Espaco.curto)) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                // O estudio claro por tras da foto, como nos cartoes da loja: branco ao centro a fugir para o creme.
                Box(
                    Modifier.fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
                )
                if (posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(Espaco.curto)
                            .graphicsLayer { alpha = if (item.esgotado) 0.6f else 1f },
                    )
                }
                if (item.esgotado) {
                    Text(
                        text = "SOLD OUT",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 18.sp),
                        color = Vermelho,
                        letterSpacing = 2.sp,
                        modifier = Modifier.align(Alignment.Center)
                            .graphicsLayer { rotationZ = -14f }
                            .background(Papel.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .border(3.dp, Vermelho, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                if (item.temGiro && !item.esgotado) {
                    Text(
                        text = "360°",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Papel,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .background(LARANJA, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Box(Modifier.padding(top = Espaco.minimo).width(30.dp * p).height(3.dp).background(Amarelo.copy(alpha = p)))
            Text(
                text = item.nome,
                style = MaterialTheme.typography.titleSmall,
                color = if (focado) Creme else if (item.esgotado) TintaFraca else Tinta,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Espaco.minimo),
            )
            Text(
                text = precoTxt(item.preco),
                style = MaterialTheme.typography.labelMedium,
                color = if (focado) Creme else LARANJA,
                modifier = Modifier.padding(top = Espaco.minimo),
            )
        }
    }
}

/** A montra do artigo escolhido: a peca grande a rodar em loop (ou a foto, sem video) e a ficha ao lado. */
@OptIn(UnstableApi::class)
@Composable
private fun Montra(
    item: Parafernalia,
    imagemUrl: String,
    giroUrl: String,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val temVideo = giroUrl.isNotBlank()
    // Um player por montra (a chamada esta dentro de key(slug)): liberta-se sempre no onDispose.
    val player = remember { if (temVideo) ExoPlayer.Builder(context).build() else null }

    if (player != null) {
        DisposableEffect(Unit) {
            player.setMediaItem(ExoMediaItem.fromUri(giroUrl))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.volume = 0f
            player.prepare()
            player.playWhenReady = true
            val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() else if (e == Lifecycle.Event.ON_START) player.play() }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                player.release()
            }
        }
    }

    // A montra apanha o foco: sem isto o D-pad ia para a grelha escondida por baixo e trocava de artigo as cegas.
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    PapelTheme {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .focusRequester(foco)
                .focusable(),
        ) {
            FundoDePapel()
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
                horizontalArrangement = Arrangement.spacedBy(Espaco.seccao),
            ) {
                Box(Modifier.weight(0.55f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxHeight().aspectRatio(1f).border(2.dp, Tinta),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (player != null) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                                        this.player = player
                                        setShutterBackgroundColor(Papel.toArgb())
                                        setBackgroundColor(Papel.toArgb())
                                    }
                                },
                                onRelease = { it.player = null },
                            )
                        } else if (imagemUrl.isNotBlank()) {
                            AsyncImage(
                                model = imagemUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(Espaco.largo),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(22.dp).height(3.dp).background(LARANJA))
                        Text(
                            text = item.categoria.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = LARANJA,
                            letterSpacing = 3.sp,
                            modifier = Modifier.padding(start = Espaco.curto),
                        )
                    }
                    Text(
                        text = item.nome,
                        style = MaterialTheme.typography.displayMedium,
                        color = Tinta,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Espaco.curto),
                    )
                    precoTxt(item.preco).takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineMedium,
                            color = LARANJA,
                            modifier = Modifier.padding(top = Espaco.medio),
                        )
                    }
                    if (item.esgotado) {
                        Text(
                            text = tr("Esgotado", "Sold out"),
                            style = MaterialTheme.typography.titleSmall,
                            color = Vermelho,
                            modifier = Modifier.padding(top = Espaco.curto),
                        )
                    }
                    Text(
                        text = if (temVideo) tr("A peça a 360° · Voltar para a lista", "The piece at 360° · Back to the list")
                        else tr("Voltar para a lista", "Back to the list"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TintaFraca,
                        modifier = Modifier.padding(top = Espaco.largo),
                    )
                }
            }
        }
    }
}

private fun precoTxt(preco: Double?): String =
    preco?.let { String.format(java.util.Locale("pt", "PT"), "%.2f €", it) } ?: ""
