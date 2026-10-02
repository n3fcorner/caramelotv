@file:kotlin.OptIn(ExperimentalSharedTransitionApi::class)

package pt.caramelo.tv.ui.player

import pt.caramelo.tv.ui.components.AromasIlustrados
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import android.view.LayoutInflater
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import pt.caramelo.tv.R
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.animation.Entrada
import pt.caramelo.tv.ui.animation.LocalSharedTransition
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.animation.heroBounds
import pt.caramelo.tv.ui.components.FundoDePapel
import pt.caramelo.tv.ui.theme.PapelTheme
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.components.PerfilIndicaSativa
import pt.caramelo.tv.ui.components.TerpenosDominantes
import pt.caramelo.tv.ui.components.formatarTempo
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.corDaSeccao
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.RoxoProfundo
import pt.caramelo.tv.ui.theme.VerdeClaro

private const val SALTO_MS = 10_000L

/** Ultimo fotograma de um produto, guardado para o painel dele levar ao sair. */
private class Fotograma(val id: String, val imagem: ImageBitmap)

/**
 * Leitor da marca. O controlador do Media3 sai por completo — era cinzento, generico
 * e nao respondia bem ao D-pad.
 *
 *   +---------------+----------------+
 *   | [linha]       |  NOME          |
 *   |    VÍDEO      |  formato…      |
 *   |   quadrado    |  [ ] CULTIVO   |
 *   | ▌▌▌░░  0:12   |  [ ] AROMA     |
 *   +---------------+----------------+
 *
 * O quadrado deixa uma coluna livre num ecra 16:9. E ai que vive a ficha, e no fundo
 * a mascote em contorno lilas — pouco contraste de proposito, para ser textura da
 * marca e nao mais uma coisa a disputar a atencao.
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

    val player = remember { ExoPlayer.Builder(context).build() }

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
    val ladoDoPainel = with(LocalDensity.current) {
        (LocalConfiguration.current.screenHeightDp.dp - Overscan.vertical * 2).roundToPx()
    }
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

    // 250ms chega para a barra parecer continua sem acordar a composicao a cada frame.
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

    // O leitor no papel, como a lista e a ficha (1/10/2026, "os videos ainda estao no tema antigo azul"): bege e tinta,
    // o video pousado sem moldura, e as etiquetas sobre o video a tinta, que se le em cima de qualquer fotograma.
    PapelTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    // fotograma que o painel leva no voo de regresso ao mosaico.
                    Key.Back, Key.Escape -> { guardarFotograma(); false }
                    else -> false
                }
            },
    ) {
        FundoDePapel()

        // A vista de video, na geometria exacta do painel (a mesma Row que o Leitor
        // usa), por baixo de tudo o que anima. Enquanto um ecra voa para o outro, ou
        // um produto desliza para o seguinte, esta escondida: o que se ve e' o painel
        // a levar o cartaz ou o ultimo fotograma, sobre o fundo da marca. Se ficasse
        // visivel, aparecia por entre os dois paineis meio transparentes.
        val transicaoProduto = updateTransition(targetState = item, label = "produto")
        val transicaoActiva = transicao?.isTransitionActive == true
        val aAnimar = transicaoActiva || transicaoProduto.isRunning
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical)
                .graphicsLayer { alpha = if (aAnimar) 0f else 1f },
            horizontalArrangement = Arrangement.spacedBy(Espaco.seccao),
        ) {
            Box(
                modifier = Modifier.weight(0.55f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                            this.player = player
                            keepScreenOn = true
                            setShutterBackgroundColor(Papel.toArgb())
                            setBackgroundColor(Papel.toArgb())
                            vista = this
                        }
                    },
                    onRelease = { v ->
                        v.player = null
                        v.keepScreenOn = false
                        vista = null
                    },
                )
            }
            Spacer(Modifier.weight(0.45f))
        }

        transicaoProduto.AnimatedContent(
            contentKey = { it.id },
            transitionSpec = {
                // Seguinte: o novo sobe do fundo e o velho sai por cima. Anterior: o
                // inverso. Um quinto do ecra chega para se ler o sentido sem parecer
                // que a montra deu um salto. O que sai nao se esbate: fica opaco a
                // deslizar, com o ultimo fotograma, ate o novo (que entra a esbater-se
                // por cima) o tapar. Assim nunca ha um buraco entre os dois.
                (fadeIn(tween(300)) + slideInVertically(tween(420)) { altura -> sentido * altura / 5 })
                    .togetherWith(
                        slideOutVertically(tween(420)) { altura -> -sentido * altura / 5 },
                    )
            },
        ) { alvo ->
            // `item` e' sempre o alvo mais recente; o `alvo` de um conteudo a sair ja
            // nao lhe e' igual. O cartaz so se levanta quando este painel esta
            // assente (nem a deslizar nem a voar) e o video ja tem fotograma: antes
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
                progresso = progresso,
                posicao = posicao,
                duracao = duracao,
                erro = erro,
                aReproduzir = aReproduzir,
                atrator = atrator,
            )
        }
    }
    }
}

/** Um produto no leitor: o painel a esquerda e a ficha ao lado. So desenho; o video esta por baixo. */
@Composable
private fun Leitor(
    item: MenuItem,
    videoUrl: String,
    posterUrl: String,
    fotograma: ImageBitmap?,
    mostrarVideo: Boolean,
    progresso: Float,
    posicao: Long,
    duracao: Long,
    erro: String?,
    aReproduzir: Boolean,
    atrator: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
        horizontalArrangement = Arrangement.spacedBy(Espaco.seccao),
    ) {
        Box(
            modifier = Modifier.weight(0.55f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            PainelDeVideo(
                item = item,
                progresso = progresso,
                posicao = posicao,
                duracao = duracao,
                erro = erro,
                videoUrl = videoUrl,
                posterUrl = posterUrl,
                fotograma = fotograma,
                mostrarVideo = mostrarVideo,
            )
        }

        FichaDoProduto(
            item = item,
            aReproduzir = aReproduzir,
            atrator = atrator,
            modifier = Modifier.weight(0.45f).fillMaxHeight(),
        )
    }
}

@Composable
private fun PainelDeVideo(
    item: MenuItem,
    progresso: Float,
    posicao: Long,
    duracao: Long,
    erro: String?,
    videoUrl: String,
    posterUrl: String,
    fotograma: ImageBitmap?,
    mostrarVideo: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            // A mesma chave do mosaico e do cartaz: a foto voa ate aqui e o video
            // aparece por baixo dela, em vez de o quadrado surgir do nada.
            .hero(item.id),
    ) {
        // O cartaz tapa o video ate haver um fotograma para mostrar e o painel estar
        // assente, e depois esbate-se. E' a mesma imagem que veio a voar do mosaico:
        // o quadrado nunca esta vazio, nem no arranque do clipe, nem ao mudar de erva.
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
                // O recorte do site pousado no papel: inteiro, sem cortar a flor.
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = veuDoCartaz },
            )
        }

        // A linha do produto fica sobre o video, como uma etiqueta de montra.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Espaco.medio)
                .background(Tinta.copy(alpha = 0.88f))
                .padding(horizontal = Espaco.medio, vertical = Espaco.curto),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(18.dp).height(3.dp).background(Amarelo))
            Text(
                text = termo(item.section.label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Amarelo,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = Espaco.curto),
            )
        }

        // Progresso dentro do painel, como no leitor de referencia.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Tinta.copy(alpha = 0.88f))
                .padding(horizontal = Espaco.medio, vertical = Espaco.curto),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatarTempo(posicao),
                    style = MaterialTheme.typography.labelSmall,
                    color = Creme,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = Espaco.medio)
                        .height(4.dp)
                        .background(Creme.copy(alpha = 0.22f)),
                ) {
                    Box(Modifier.fillMaxWidth(progresso).height(4.dp).background(Amarelo))
                }
                Text(
                    text = formatarTempo(duracao),
                    style = MaterialTheme.typography.labelSmall,
                    color = Creme,
                )
            }
        }

        erro?.let { codigo ->
            ErroDeLeitura(
                codigo = codigo,
                url = videoUrl,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun FichaDoProduto(
    item: MenuItem,
    aReproduzir: Boolean,
    atrator: Boolean,
    modifier: Modifier = Modifier,
) {
    // A ficha em cima, centrada no espaco que sobra, e a assinatura agarrada ao
    // fundo. Antes era tudo uma coluna centrada: nos artigos com cultivo entra
    // mais um cartao, a coluna cresce, e o que estava em baixo saia do ecra — a
    // frase aparecia nuns artigos e noutros nao.
    Column(modifier) {
    Column(
        modifier = Modifier.weight(1f),
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
            text = item.title,
            style = MaterialTheme.typography.displayMedium,
            color = Tinta,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = Espaco.curto)
                // O nome do detalhe e o do leitor sao a mesma caixa a voar.
                .heroBounds("titulo-${item.id}"),
        )

        val infundidoTxt = tr("Infundido", "Infused")
        val resumo = listOfNotNull(
            item.strain.format.takeIf { it.isNotBlank() }?.let { termo(it) },
            item.strain.cultivation.takeIf { it.isNotBlank() }?.let { termo(it) },
            infundidoTxt.takeIf { item.strain.infused },
        )
        if (resumo.isNotEmpty()) {
            Text(
                text = resumo.joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espaco.minimo),
            )
        }

        Column(
            modifier = Modifier.padding(top = Espaco.largo),
            verticalArrangement = Arrangement.spacedBy(Espaco.curto),
        ) {
            // Os cartoes entram em cascata, um atras do outro, em vez de aparecerem
            // todos de uma vez com o resto do ecra.
            // O cultivo ja vai na linha de resumo e no ponto do mosaico: aqui o
            // espaco e' para o aroma, o perfil e os terpenos, como no telemovel.
            if (item.strain.aroma.isNotBlank()) {
                Entrada(ordem = 1) {
                    // Os aromas em desenhos de traco (pedido do Leo, 29/9/2026), no mesmo cartao que a ficha.
                    AromasIlustrados(
                        item.strain.aroma,
                        termo(item.strain.aroma),
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(Espaco.medio),
                        tamanho = 38.dp,
                    )
                }
            }
            Entrada(ordem = 2) { PerfilIndicaSativa(item.strain) }
            Entrada(ordem = 3) { TerpenosDominantes(item.strain.terpenes, item.strain.terpenesPct, Modifier.padding(top = Espaco.minimo)) }
        }

    }

        Text(
            text = when {
                atrator -> tr("Montra automática · carrega numa tecla", "Automatic showcase · press any key")
                aReproduzir -> tr("● A reproduzir", "● Playing")
                else -> tr("❚❚ Em pausa", "❚❚ Paused")
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (aReproduzir && !atrator) Laranja else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espaco.medio),
        )

        Assinatura(Modifier.padding(top = Espaco.curto))
    }
}

/**
 * A assinatura da marca, copiada do estendarte da loja.
 *
 * Aparece em todos os artigos, agarrada ao fundo da coluna. Comecou por ser so
 * na montra automatica, mas assim so se via em parte deles — e uma assinatura
 * que aparece a alternar nao se le como assinatura, le-se como falha.
 *
 * O "Caramelos" vai a laranja como no estendarte — e' a unica palavra que la
 * muda de cor, e a marca ja tem esse laranja na paleta.
 */
@Composable
private fun Assinatura(modifier: Modifier = Modifier) {
    val inicio = tr("Afinal, somos todos ", "After all, we're all ")
    val frase = buildAnnotatedString {
        withStyle(SpanStyle(color = Tinta)) { append(inicio) }
        withStyle(SpanStyle(color = Laranja)) { append("Caramelos.") }
    }
    Column(modifier) {
        Box(Modifier.width(28.dp).height(2.dp).background(Laranja))
        Text(
            text = frase,
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = Espaco.curto),
        )
    }
}

@Composable
private fun CartaoDeFicha(icone: ImageVector, titulo: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Espaco.medio),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(RoxoProfundo.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = icone,
                contentDescription = null,
                colorFilter = ColorFilter.tint(VerdeClaro),
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.padding(start = Espaco.medio)) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Amarelo,
                letterSpacing = 2.sp,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyMedium,
                color = Creme,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ErroDeLeitura(codigo: String, url: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(Espaco.largo),
    ) {
        Text(
            text = tr("Não foi possível reproduzir", "Could not play"),
            style = MaterialTheme.typography.titleMedium,
            color = Tinta,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Espaco.minimo),
        )
    }
}
