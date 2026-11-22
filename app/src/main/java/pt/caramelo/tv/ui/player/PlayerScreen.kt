@file:kotlin.OptIn(ExperimentalSharedTransitionApi::class)

package pt.caramelo.tv.ui.player

import android.view.LayoutInflater
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import pt.caramelo.tv.R
import pt.caramelo.tv.data.CacheDeMedia
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.animation.LocalSharedTransition
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.precoCurto
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.inicio.BarraDeTempo
import pt.caramelo.tv.ui.inicio.Estado
import pt.caramelo.tv.ui.inicio.Etiqueta
import pt.caramelo.tv.ui.inicio.Ficha
import pt.caramelo.tv.ui.inicio.FundoDaPagina
import pt.caramelo.tv.ui.inicio.Grelha
import pt.caramelo.tv.ui.inicio.GrelhaPagina
import pt.caramelo.tv.ui.inicio.LocalComVideo
import pt.caramelo.tv.ui.inicio.LuzDasFlores
import pt.caramelo.tv.ui.inicio.MolduraJanela
import pt.caramelo.tv.ui.inicio.Nicho
import pt.caramelo.tv.ui.inicio.NichoDaJanela
import pt.caramelo.tv.ui.inicio.PeDaPagina
import pt.caramelo.tv.ui.inicio.TopoDaPagina
import pt.caramelo.tv.ui.inicio.fichaDaFlor
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Verde
import pt.caramelo.tv.ui.theme.corDaSeccao

private const val SALTO_MS = 10_000L

/** Ultimo fotograma de um produto, guardado para o painel dele levar ao sair. */
private class Fotograma(val id: String, val imagem: ImageBitmap)

/**
 * Leitor da marca. O controlador do Media3 sai por completo — era cinzento, generico
 * e nao respondia bem ao D-pad.
 *
 *   [logo]                         OK pausa  ◀▶ 10 s  ▲▼ outra flor
 *   [chip] FLOR · LOTE        +------------------+ etiqueta
 *   NOME                      |                  |  38 €
 *   tipo                      |      VÍDEO       |
 *   indica / sativa           |     quadrado     |
 *   terpenos, aromas          +------------------+
 *   ● a reproduzir            0:12 ▬▬▬▬░░░░ 0:52
 *
 * E' o ecra inicial com a janela maior (ver ui/inicio/Pagina.kt): a ficha fica onde estava, e no
 * lugar do frasco esta a janela da marca com o video quadrado la dentro.
 *
 * D-pad: OK pausa, esquerda/direita saltam 10s, cima/baixo mudam de produto.
 *
 * Ha UM ExoPlayer e UMA vista de video para todos os produtos, e vivem fora do
 * AnimatedContent e da transicao partilhada. Antes cada produto criava os seus, e
 * o Compose retinha cada PlayerView que saia de dentro de um sharedElement: ao fim
 * de uma hora de montra automatica a app ia em 900 MB e a TV inteira em swap. O
 * que desliza e voa e' so o painel com o cartaz (ou o ultimo fotograma), que e'
 * desenho puro; o video fica quieto por baixo, no sitio onde o painel assenta.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: MenuItem,
    videoUrlOf: (MenuItem) -> String,
    posterUrlOf: (MenuItem) -> String,
    startPositionMs: Long,
    loop: Boolean,
    atrator: Boolean,
    /** +1 quando se passou ao produto seguinte, -1 ao anterior: decide para onde desliza. */
    sentido: Int,
    onExit: (id: String, positionMs: Long, finished: Boolean) -> Unit,
    onSkip: (delta: Int) -> Unit,
    onFimDoAtrator: () -> Unit,
    onControlo: () -> Unit,
    /** Cartazes dos produtos para onde se pode saltar a seguir, para ja estarem em memoria. */
    cartazesVizinhos: List<String> = emptyList(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val transicao = LocalSharedTransition.current
    val exitCallback by rememberUpdatedState(onExit)
    val fimDoAtrator by rememberUpdatedState(onFimDoAtrator)
    val atratorActual by rememberUpdatedState(atrator)
    val itemActual by rememberUpdatedState(item)

    var erro by remember { mutableStateOf<String?>(null) }
    var aReproduzir by remember { mutableStateOf(true) }
    var primeiroFotograma by remember { mutableStateOf(false) }
    var posicao by remember { mutableLongStateOf(startPositionMs) }
    var duracao by remember { mutableLongStateOf(0L) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var vista by remember { mutableStateOf<PlayerView?>(null) }
    var fotogramaDeSaida by remember { mutableStateOf<Fotograma?>(null) }
    val primeiroId = remember { item.id }

    // Sempre sem som (18/11/2026): a TV e uma montra numa loja, e 5 dos clipes traziam faixa de audio (AUDITORIA, ponto 2).
    val player = remember { CacheDeMedia.leitor(context).apply { volume = 0f } }

    // Tira uma fotografia ao que esta no ecra antes de o painel se ir embora: e' ela
    // que o painel leva a deslizar ou a voar, e nao um cartaz que nao se parece com
    // o fotograma onde o video ia. Custa uma leitura da GPU, uma vez por mudanca.
    fun guardarFotograma() {
        if (!primeiroFotograma) return
        val textura = vista?.videoSurfaceView as? TextureView ?: return
        val imagem = runCatching { textura.bitmap }.getOrNull() ?: return
        fotogramaDeSaida = Fotograma(itemActual.id, imagem.asImageBitmap())
    }

    // Trocar de produto e' trocar de clipe no mesmo player. Ao sair (de produto ou do
    // ecra) grava-se a posicao do clipe que ficou para tras.
    val videoUrl = videoUrlOf(item)
    DisposableEffect(item.id, videoUrl) {
        erro = null
        primeiroFotograma = false
        player.setMediaItem(ExoMediaItem.fromUri(videoUrl))
        player.prepare()
        if (startPositionMs > 0) player.seekTo(startPositionMs)
        player.playWhenReady = true
        onDispose {
            // Ler a posicao antes de trocar: e' ainda a do clipe que sai.
            exitCallback(item.id, player.currentPosition, player.playbackState == Player.STATE_ENDED)
        }
    }

    // Os cartazes dos vizinhos vao para a cache do Coil ja ao tamanho do painel:
    // ler e descodificar um JPEG de 720px demora um quarto de segundo nesta TV, e
    // nesse tempo o painel novo entrava vazio.
    val ladoDoPainel = with(LocalDensity.current) { GrelhaPagina.janela.roundToPx() }
    LaunchedEffect(cartazesVizinhos) {
        val loader = context.imageLoader
        cartazesVizinhos.filter { it.isNotBlank() }.forEach { url ->
            loader.enqueue(ImageRequest.Builder(context).data(url).size(ladoDoPainel).build())
        }
    }

    // Na montra automatica o clipe nao repete: acaba e passa ao seguinte.
    LaunchedEffect(loop, atrator) {
        player.repeatMode = if (loop && !atrator) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                erro = e.errorCodeName
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                aReproduzir = isPlaying
            }

            override fun onPlaybackStateChanged(estado: Int) {
                if (atratorActual && estado == Player.STATE_ENDED) {
                    guardarFotograma()
                    fimDoAtrator()
                }
            }

            override fun onRenderedFirstFrame() {
                primeiroFotograma = true
            }
        }
        player.addListener(listener)

        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    // 250ms chega para a barra parecer continua. Os tres valores so se leem no desenho da barra e nos dois textos
    // dos minutos (BarraDeTempo): o leitor nao recompoe com eles.
    LaunchedEffect(player) {
        while (true) {
            posicao = player.currentPosition
            duracao = player.duration.coerceAtLeast(0L)
            progresso = if (duracao > 0) (posicao.toFloat() / duracao).coerceIn(0f, 1f) else 0f
            delay(250)
        }
    }

    // O fotograma de saida serve a transicao e mais nada: se por alguma razao ela
    // nao acontecer, cai sozinho em vez de ficar a tapar o video.
    LaunchedEffect(fotogramaDeSaida) {
        if (fotogramaDeSaida != null) {
            delay(1500)
            fotogramaDeSaida = null
        }
    }

    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    // O leitor no mundo do ecra inicial (22/11/2026, Francisco, na TV da loja: "o player e a expansao das paginas ainda esta no
    // tema antigo, preciso que coloques tudo consistente"). Era o papel bege de 1/10; agora e' o azul das flores com o foco de
    // luz, a mesma ficha de revista no mesmo sitio, e o video na janela da marca, maior, com a etiqueta do preco no canto.
    // Nada mexe alem do video (a luz parada, a etiqueta parada): quem acorda o ecra e' so o fotograma do video.
    val progressoActual = { progresso }
    CarameloTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tinta)
            .focusRequester(foco)
            .focusable()
            .onPreviewKeyEvent { evento ->
                if (evento.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // Alguem pegou no comando: a montra deixa de andar sozinha.
                onControlo()
                when (evento.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        if (player.isPlaying) player.pause() else player.play()
                        true
                    }
                    Key.DirectionLeft -> {
                        player.seekTo((player.currentPosition - SALTO_MS).coerceAtLeast(0))
                        true
                    }
                    Key.DirectionRight -> {
                        val limite = player.duration.coerceAtLeast(0)
                        player.seekTo((player.currentPosition + SALTO_MS).coerceAtMost(limite))
                        true
                    }
                    Key.DirectionUp -> { guardarFotograma(); onSkip(-1); true }
                    Key.DirectionDown -> { guardarFotograma(); onSkip(1); true }
                    // O voltar segue para o BackHandler da raiz; aqui so se guarda o
                    // fotograma que o painel leva no voo de regresso ao frasco.
                    Key.Back, Key.Escape -> { guardarFotograma(); false }
                    else -> false
                }
            },
    ) {
        // A vista de video, na geometria exacta da janela, por baixo de tudo o que anima. Enquanto um ecra voa para o
        // outro, ou um produto da lugar ao seguinte, esta escondida: o que se ve e' o cartaz ou o ultimo fotograma,
        // dentro da mesma janela. Se ficasse visivel, aparecia por entre os dois cartazes meio transparentes.
        val transicaoProduto = updateTransition(targetState = item, label = "produto")
        val transicaoActiva = transicao?.isTransitionActive == true
        val aAnimar = transicaoActiva || transicaoProduto.isRunning
        PaginaDoLeitor(
            item = item, atrator = atrator, aReproduzir = aReproduzir,
            progresso = progressoActual, posicao = { posicao }, duracao = { duracao },
            video = {
                AndroidView(
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (aAnimar) 0f else 1f },
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                            this.player = player
                            keepScreenOn = true
                            setShutterBackgroundColor(Nicho.toArgb())
                            setBackgroundColor(Nicho.toArgb())
                            vista = this
                        }
                    },
                    onRelease = { v ->
                        v.player = null
                        v.keepScreenOn = false
                        vista = null
                    },
                )
            },
        ) {
            transicaoProduto.AnimatedContent(
                contentKey = { it.id },
                transitionSpec = {
                    // Sem o deslize para cima (4/10/2026, Francisco: "o swipe up nao e' o que adoro, e esta mal desenhado"): o
                    // produto que sai esbate-se em 160 ms, e o seguinte aparece depois dele. Os dois nunca se sobrepoem; no
                    // intervalo ve-se o fundo e a janela vazia, que sao de todos.
                    fadeIn(tween(280, delayMillis = 140)).togetherWith(fadeOut(tween(160)))
                },
            ) { alvo ->
                // `item` e' sempre o alvo mais recente; o `alvo` de um conteudo a sair ja
                // nao lhe e' igual. O cartaz so se levanta quando este painel esta
                // assente (nem a trocar nem a voar) e o video ja tem fotograma: antes
                // disso, levantar o cartaz mostrava um buraco onde o video ainda nao esta.
                val activo = alvo.id == item.id
                val assente = !transicaoProduto.isRunning
                val fotograma = fotogramaDeSaida?.takeIf { it.id == alvo.id }
                // O fotograma de saida morre com o painel que o levou.
                DisposableEffect(alvo.id) {
                    onDispose { if (fotogramaDeSaida?.id == alvo.id) fotogramaDeSaida = null }
                }
                Leitor(
                    item = alvo,
                    videoUrl = videoUrlOf(alvo),
                    posterUrl = posterUrlOf(alvo),
                    fotograma = fotograma?.imagem,
                    mostrarVideo = activo && assente && !aAnimar && primeiroFotograma && fotograma == null,
                    erro = erro,
                    // a primeira ficha ja estava no ecra inicial, no mesmo sitio: nao volta a entrar. As seguintes (▲▼) entram.
                    entrada = alvo.id != primeiroId,
                )
            }
        }
    }
    }
}

/**
 * A pagina do leitor, do fundo para cima: o azul das flores com a luz parada, o nicho da janela, o [video], o [produto]
 * (o cartaz na janela e a ficha, que trocam quando se muda de flor) e, por cima, o que e' de todos os produtos: a moldura
 * da janela com a sombra na cor da seccao, a etiqueta do preco (salta quando o preco muda), a barra do tempo, o estado e
 * a assinatura, e o topo. `internal` para os testes de imagem a desenharem sem ExoPlayer.
 */
@Composable
internal fun PaginaDoLeitor(
    item: MenuItem,
    atrator: Boolean,
    aReproduzir: Boolean,
    progresso: () -> Float,
    posicao: () -> Long,
    duracao: () -> Long,
    video: @Composable () -> Unit,
    produto: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        FundoDaPagina(LuzDasFlores, vivo = false)
        NichoDaJanela()
        Box(Modifier.offset(GrelhaPagina.janelaX, GrelhaPagina.janelaY).size(GrelhaPagina.janela).clip(RoundedCornerShape(Grelha.janelaCanto))) { video() }
        produto()
        MolduraJanela(
            corDaSeccao(item.section), x = GrelhaPagina.janelaX, y = GrelhaPagina.janelaY, largura = GrelhaPagina.janela, altura = GrelhaPagina.janela, reflexo = false,
        )
        precoCurto(item.preco).takeIf { it.isNotBlank() }?.let { Etiqueta(it, "", GrelhaPagina.etiquetaX, GrelhaPagina.etiquetaY, balanca = false) }
        BarraDeTempo(progresso, posicao, duracao)
        PeDaPagina {
            when {
                atrator -> Estado(tr("Montra automática · carrega numa tecla", "Automatic showcase · press any key"), Amarelo)
                aReproduzir -> Estado(tr("A reproduzir", "Playing"), Verde)
                else -> Estado(tr("Em pausa", "Paused"), Laranja)
            }
        }
        TopoDaPagina(
            listOf("OK" to tr("pausa", "pause"), "◀ ▶" to "10 s", "▲ ▼" to tr("outra flor", "next flower"), "↩" to tr("voltar", "back")),
        )
    }
}

/**
 * Um produto no leitor: o cartaz dentro da janela e a ficha na primeira coluna. So desenho; o video esta por baixo e a
 * moldura por cima. `internal` para os testes de imagem o desenharem sem ExoPlayer.
 */
@Composable
internal fun Leitor(
    item: MenuItem,
    videoUrl: String,
    posterUrl: String,
    fotograma: ImageBitmap?,
    mostrarVideo: Boolean,
    erro: String?,
    entrada: Boolean = true,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(GrelhaPagina.janelaX, GrelhaPagina.janelaY)
                .size(GrelhaPagina.janela)
                // A mesma chave da flor dentro do frasco: a foto voa do frasco ate aqui e o video
                // aparece por baixo dela, em vez de a janela surgir do nada.
                .hero(item.id)
                .clip(RoundedCornerShape(Grelha.janelaCanto)),
        ) {
            // O cartaz tapa o video ate haver um fotograma para mostrar e o painel estar
            // assente, e depois esbate-se. E' a mesma imagem que veio a voar do frasco:
            // a janela nunca esta vazia, nem no arranque do clipe, nem ao mudar de flor.
            val veuDoCartaz by animateFloatAsState(
                targetValue = if (mostrarVideo) 0f else 1f,
                animationSpec = tween(360),
                label = "cartaz",
            )
            if (fotograma != null) {
                // A sair: leva o ultimo fotograma do video, e nao o cartaz.
                Image(
                    bitmap = fotograma,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (posterUrl.isNotBlank() && veuDoCartaz > 0.01f) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    // O recorte do site pousado no nicho: inteiro, sem cortar a flor.
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(34.dp).graphicsLayer { alpha = veuDoCartaz },
                )
            }
            erro?.let { codigo ->
                ErroDeLeitura(codigo = codigo, url = videoUrl, modifier = Modifier.align(Alignment.Center))
            }
        }
        Ficha(fichaDaFlor(item), fichaDaFlor(item).olho, entrada = entrada)
    }
}

@Composable
private fun ErroDeLeitura(codigo: String, url: String, modifier: Modifier = Modifier) {
    val caixa = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .padding(24.dp)
            .background(Tinta.copy(alpha = 0.85f), caixa)
            .border(1.5.dp, Papel.copy(alpha = 0.22f), caixa)
            .padding(Espaco.largo),
    ) {
        Text(
            text = tr("Não foi possível reproduzir", "Could not play"),
            style = MaterialTheme.typography.titleMedium,
            color = Papel,
        )
        Text(
            text = codigo,
            style = MaterialTheme.typography.labelSmall,
            color = Laranja,
            modifier = Modifier.padding(top = Espaco.curto),
        )
        Text(
            text = url,
            style = MaterialTheme.typography.labelSmall,
            color = Papel.copy(alpha = 0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Espaco.minimo),
        )
    }
}
