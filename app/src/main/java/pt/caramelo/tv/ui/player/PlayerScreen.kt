package pt.caramelo.tv.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Spa
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.components.FundoDaMarca
import pt.caramelo.tv.ui.components.formatarTempo
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.RoxoProfundo
import pt.caramelo.tv.ui.theme.VerdeClaro

private const val SALTO_MS = 10_000L

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
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: MenuItem,
    videoUrl: String,
    startPositionMs: Long,
    loop: Boolean,
    atrator: Boolean,
    onExit: (positionMs: Long, finished: Boolean) -> Unit,
    onSkip: (delta: Int) -> Unit,
    onFimDoAtrator: () -> Unit,
    onControlo: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exitCallback by rememberUpdatedState(onExit)
    var erro by remember { mutableStateOf<String?>(null) }
    var aReproduzir by remember { mutableStateOf(true) }
    var posicao by remember { mutableLongStateOf(startPositionMs) }
    var duracao by remember { mutableLongStateOf(0L) }
    var progresso by remember { mutableFloatStateOf(0f) }

    val fimDoAtrator by rememberUpdatedState(onFimDoAtrator)

    val player = remember(item.id, videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(videoUrl))
            if (startPositionMs > 0) seekTo(startPositionMs)
            // Na montra automatica o clipe nao repete: acaba e passa ao seguinte.
            repeatMode = if (loop && !atrator) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
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
                if (atrator && estado == Player.STATE_ENDED) fimDoAtrator()
            }
        }
        player.addListener(listener)

        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Ler a posicao antes de libertar: depois do release o player e inutilizavel.
            val fim = player.currentPosition
            val terminado = player.playbackState == Player.STATE_ENDED
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
            exitCallback(fim, terminado)
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

    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

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
                    Key.DirectionUp -> { onSkip(-1); true }
                    Key.DirectionDown -> { onSkip(1); true }
                    else -> false
                }
            },
    ) {
        FundoDaMarca()

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
                    player = player,
                    progresso = progresso,
                    posicao = posicao,
                    duracao = duracao,
                    erro = erro,
                    videoUrl = videoUrl,
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
}

@OptIn(UnstableApi::class)
@Composable
private fun PainelDeVideo(
    item: MenuItem,
    player: ExoPlayer,
    progresso: Float,
    posicao: Long,
    duracao: Long,
    erro: String?,
    videoUrl: String,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .border(BorderStroke(2.dp, Amarelo)),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(RoxoProfundo.toArgb())
                    setBackgroundColor(RoxoProfundo.toArgb())
                }
            },
        )

        // A linha do produto fica sobre o video, como uma etiqueta de montra.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Espaco.medio)
                .background(RoxoProfundo.copy(alpha = 0.88f))
                .padding(horizontal = Espaco.medio, vertical = Espaco.curto),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(18.dp).height(3.dp).background(Amarelo))
            Text(
                text = item.section.label.uppercase(),
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
                .background(RoxoProfundo.copy(alpha = 0.88f))
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
    Column(modifier, verticalArrangement = Arrangement.Center) {
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
            text = item.title,
            style = MaterialTheme.typography.displayMedium,
            color = Creme,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Espaco.curto),
        )

        val resumo = listOfNotNull(
            item.strain.format.takeIf { it.isNotBlank() },
            item.strain.cultivation.takeIf { it.isNotBlank() },
            "Infundido".takeIf { item.strain.infused },
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
            if (item.strain.cultivation.isNotBlank()) {
                CartaoDeFicha(Icons.Outlined.Spa, "Cultivo", item.strain.cultivation)
            }
            if (item.strain.aroma.isNotBlank()) {
                CartaoDeFicha(Icons.Outlined.Air, "Aroma", item.strain.aroma)
            }
        }

        Text(
            text = when {
                atrator -> "▶ Montra automática · carrega numa tecla"
                aReproduzir -> "● A reproduzir"
                else -> "❚❚ Em pausa"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (aReproduzir) Amarelo else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espaco.largo),
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
            text = "Não foi possível reproduzir",
            style = MaterialTheme.typography.titleMedium,
            color = Creme,
        )
        Text(
            text = codigo,
            style = MaterialTheme.typography.labelSmall,
            color = Amarelo,
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
