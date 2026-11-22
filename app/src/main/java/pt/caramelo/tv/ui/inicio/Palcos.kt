package pt.caramelo.tv.ui.inicio

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import pt.caramelo.tv.R
import pt.caramelo.tv.data.CacheDeMedia
import pt.caramelo.tv.data.model.FotoDeCor
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.colecao.Frasco
import pt.caramelo.tv.ui.colecao.FrascoGeo
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Falso nos testes de imagem (Robolectric): nada de ExoPlayer, fica o cartaz. */
val LocalComVideo = staticCompositionLocalOf { true }

internal val Nicho = Color(0xFF0A2C4D)
private val AzulFoco = Color(0xFF1D4D7C)
private val AzulMeio = Color(0xFF0D3459)
private val Suave = androidx.compose.animation.core.CubicBezierEasing(0.22f, 0.8f, 0.24f, 1f)

// ------------------------------------------------------------------------------------------------------------------
// O mundo de cada seccao: o fundo, em fundido quando se muda de fila
// ------------------------------------------------------------------------------------------------------------------

/**
 * O mundo de cada seccao, em fundido quando se muda de fila. Sem Crossfade (22/11/2026, Francisco: "ve se da para otimizar
 * performance"): o Crossfade punha os dois mundos, cada um a ecra inteiro, em camadas com alfa, e uma camada com alfa e' um
 * buffer fora do ecra de 1920 x 1080 redesenhado em cada fotograma do fundido. Agora o mundo que sai fica opaco por baixo
 * e o que entra aparece por cima com o alfa a multiplicar cada desenho (ModulateAlpha): nenhum buffer. As chaves guardam
 * cada mundo quando passa de cima para baixo (o leitor da cena das piteiras nao se cria duas vezes).
 */
@Composable
fun Mundo(seccao: Seccao, cenaUrl: String, cartazUrl: String, ativo: Boolean) {
    var mundos by remember { mutableStateOf(listOf(seccao)) }
    val entra = remember { Animatable(1f) }
    LaunchedEffect(seccao) {
        if (mundos.last() == seccao) return@LaunchedEffect
        mundos = listOf(mundos.last(), seccao)
        entra.snapTo(0f); entra.animateTo(1f, tween(800, easing = Suave))
        mundos = listOf(seccao)
    }
    mundos.forEachIndexed { i, s ->
        key(s) {
            val porCima = mundos.size > 1 && i == mundos.lastIndex
            Box(Modifier.fillMaxSize().graphicsLayer { if (porCima) { alpha = entra.value; compositingStrategy = CompositingStrategy.ModulateAlpha } }) {
                when (s) {
                    Seccao.FLORES -> FundoComLuz(AzulFoco, AzulMeio, Color(0xFFFFD678))
                    Seccao.PARAFERNALIA -> FundoComLuz(Color(0xFF173F66), Color(0xFF0B2E4E), Color(0xFFFFEDBE))
                    // o mundo que sai para logo o video: liberta o descodificador antes de o seguinte precisar dele
                    Seccao.PITEIRAS -> CenaDaPiteira(cenaUrl, cartazUrl, ativo && s == seccao)
                }
            }
        }
    }
}

/** As cores do mundo das flores, para as paginas que abrem a partir dele (o leitor, a ficha). */
internal val LuzDasFlores = Triple(AzulFoco, AzulMeio, Color(0xFFFFD678))
internal val LuzDaParafernalia = Triple(Color(0xFF173F66), Color(0xFF0B2E4E), Color(0xFFFFEDBE))

/**
 * Um relogio lento, em segundos, para o que mexe sempre mas devagar (a poeira, a luz a respirar): acorda o desenho
 * [hz] vezes por segundo em vez de 60. Le-se so dentro do desenho, por isso nada se recompoe.
 */
@Composable
internal fun relogioLento(hz: Int = 15): State<Float> = produceState(0f) {
    val t0 = withFrameNanos { it }
    while (true) { delay(1000L / hz); value = (withFrameNanos { it } - t0) / 1_000_000_000f }
}

/**
 * O azul da marca com um foco de luz sobre o eixo do palco e a poeira a dancar nele.
 *
 * Desempenho (22/11/2026): antes era um desenho so, a ecra inteiro, que lia duas animacoes sem fim, e por isso a TV
 * redesenhava os 1920 x 1080 sessenta vezes por segundo mesmo com tudo parado. Agora sao duas pecas. O azul e' fixo:
 * desenha-se uma vez e nunca mais e' invalidado. O cone e a poeira ficam numa camada propria so do tamanho do cone
 * (400 x 380 dp, um quinto do ecra), acordada 15 vezes por segundo pelo [relogioLento]: a luz muda 2 % em 2,5 s e a
 * poeira sobe 3 dp por segundo, a 15 por segundo ninguem ve degraus. Os pinceis e os caminhos guardam-se
 * (drawWithCache) em vez de se criarem em cada fotograma. [vivo] falso (as paginas com video) deixa o cone parado e
 * sem poeira: ai so o video acorda o ecra.
 */
@Composable
internal fun FundoComLuz(centro: Color, meio: Color, luz: Color, eixo: Dp = Grelha.eixo, vivo: Boolean = true) {
    Box(Modifier.fillMaxSize().drawWithCache {
        val azul = Brush.radialGradient(listOf(centro, meio, Tinta), center = Offset(eixo.toPx(), 180.dp.toPx()), radius = 520.dp.toPx())
        onDrawBehind { drawRect(azul) }
    })
    val largura = 400.dp; val altura = 380.dp
    val relogio = if (vivo) relogioLento() else null
    Box(
        Modifier.offset(eixo - largura / 2, 0.dp).size(largura, altura).graphicsLayer().drawWithCache {
            val meioX = size.width / 2
            // o cone: tres trapezios sobrepostos para as bordas sairem macias (o blur do Android 12 custa uma camada)
            val topo = -20.dp.toPx(); val base = size.height
            val cones = listOf(1f, 0.82f, 0.64f).map { f ->
                val cima = 38.dp.toPx() * f; val baixo = 190.dp.toPx() * f
                Path().apply { moveTo(meioX - cima, topo); lineTo(meioX + cima, topo); lineTo(meioX + baixo, base); lineTo(meioX - baixo, base); close() }
            }
            val feixe = Brush.verticalGradient(listOf(luz.copy(alpha = 0.075f), luz.copy(alpha = 0.02f), Color.Transparent), topo, base)
            val grao = Color(0xFFFFF3C9)
            onDrawBehind {
                val t = relogio?.value ?: 0f
                // respira entre 1 e 0,75 de 2,5 em 2,5 s (o mesmo vaivem de antes, em cosseno)
                val respira = if (relogio == null) 0.875f else 0.875f + 0.125f * cos(t * PI.toFloat() / 2.5f)
                cones.forEach { drawPath(it, feixe, alpha = respira) }
                if (relogio == null) return@onDrawBehind
                // a poeira: 14 graos, cada um com o seu ritmo, a subir devagar dentro do cone
                val tempo = (t / 20f) % 1f
                for (i in 0 until 14) {
                    val periodo = 0.25f + (i * 37 % 11) / 22f
                    val p = ((tempo / periodo) + i * 0.137f) % 1f
                    val x = meioX + (((i * 73) % 100) / 100f - 0.5f) * 220.dp.toPx() + (((i * 29) % 7) - 3) * 4.dp.toPx() * p
                    val y = (60 + (i * 53) % 220).dp.toPx() - 60.dp.toPx() * p
                    val a = (sin(p * PI).toFloat() * 0.5f).coerceAtLeast(0f)
                    drawCircle(grao.copy(alpha = a), (1f + (i % 3)).dp.toPx(), Offset(x, y))
                }
            }
        },
    )
}

/**
 * O palco de uma seccao a aparecer e a desaparecer. Faz o que o AnimatedVisibility fazia, mas sem buffer fora do ecra
 * (22/11/2026, desempenho): o palco ocupa o ecra inteiro, e com o fadeIn do AnimatedVisibility cada fotograma do fundido
 * desenhava-o primeiro num buffer de 1920 x 1080. Aqui o alfa multiplica cada desenho (ModulateAlpha). [p] vai de 0 a 1
 * ([entra] e [sai] em ms) e [forma] da-lhe o deslize ou a escala; fora do ecra, o palco sai da composicao.
 */
@Composable
internal fun Aparece(visivel: Boolean, entra: Int, sai: Int, forma: GraphicsLayerScope.(p: Float) -> Unit = {}, conteudo: @Composable () -> Unit) {
    val p = remember { Animatable(if (visivel) 1f else 0f) }
    LaunchedEffect(visivel) { p.animateTo(if (visivel) 1f else 0f, tween(if (visivel) entra else sai, easing = if (visivel) Suave else LinearEasing)) }
    val presente by remember { derivedStateOf { p.value > 0f } }
    if (visivel || presente) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = p.value.coerceIn(0f, 1f); compositingStrategy = CompositingStrategy.ModulateAlpha; forma(p.value) }) { conteudo() }
    }
}

// ------------------------------------------------------------------------------------------------------------------
// Piteiras: a cena (a arte e o giro num video so) a ecra inteiro, escurecida do lado da ficha, e a moldura por cima
// ------------------------------------------------------------------------------------------------------------------

@Composable
private fun CenaDaPiteira(url: String, cartaz: String, ativo: Boolean) {
    Box(Modifier.fillMaxSize().background(Nicho)) {
        if (LocalComVideo.current && url.isNotBlank()) LeitorDaCena(url, cartaz, ativo)
        else AsyncImage(cartaz, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // o lado da ficha escuro, o topo escuro para o menu se ler, e o fundo a fechar por baixo das filas
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.horizontalGradient(0f to Tinta.copy(alpha = 0.96f), 0.33f to Tinta.copy(alpha = 0.9f), 0.5f to Tinta.copy(alpha = 0.35f), 0.66f to Tinta.copy(alpha = 0.05f), 1f to Tinta.copy(alpha = 0.1f)))
            drawRect(Brush.verticalGradient(0f to Tinta.copy(alpha = 0.96f), 0.09f to Tinta.copy(alpha = 0.7f), 0.22f to Color.Transparent, 0.56f to Color.Transparent, 0.68f to Tinta.copy(alpha = 0.94f), 0.76f to Tinta))
        })
    }
}

/**
 * Um ExoPlayer so, que vive enquanto a fila das piteiras esta em foco e e' libertado ao sair dela ou do ecra (a TCL nao
 * aguenta dois descodificadores). Troca de cena so quando o foco pousa (350 ms): correr a fila nao abre 15 videos.
 * O cartaz fica por cima ate ao primeiro fotograma da cena nova.
 */
@Composable
private fun LeitorDaCena(url: String, cartaz: String, ativo: Boolean) {
    val context = LocalContext.current
    val dono = LocalLifecycleOwner.current
    val player = remember { CacheDeMedia.leitor(context).apply { volume = 0f; repeatMode = Player.REPEAT_MODE_ONE } }
    var primeiro by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val ouvinte = object : Player.Listener { override fun onRenderedFirstFrame() { primeiro = true } }
        val ciclo = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() else if (e == Lifecycle.Event.ON_START && ativo) player.play() }
        player.addListener(ouvinte); dono.lifecycle.addObserver(ciclo)
        onDispose { player.removeListener(ouvinte); dono.lifecycle.removeObserver(ciclo); player.release() }
    }
    LaunchedEffect(url, ativo) {
        if (!ativo) { player.stop(); return@LaunchedEffect }
        primeiro = false
        delay(350)
        player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.playWhenReady = true
    }
    val veu by animateFloatAsState(if (primeiro) 0f else 1f, tween(360), label = "cartaz")
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                    this.player = player
                    setShutterBackgroundColor(Nicho.toArgb())
                    setBackgroundColor(Nicho.toArgb())
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            onRelease = { it.player = null },
        )
        AsyncImage(cartaz, null, Modifier.fillMaxSize().graphicsLayer { alpha = veu }, contentScale = ContentScale.Crop)
    }
}

/**
 * A moldura da janela da marca: contorno creme por dentro de um contorno tinta, cantos de 14 dp e a sombra dura na cor
 * da seccao, so por fora da janela (por dentro fica o que estiver por baixo: a cena das piteiras ou o estudio).
 */
@Composable
fun MolduraJanela(
    sombra: Color, modifier: Modifier = Modifier, boia: Boolean = false,
    /** Onde fica e quanto mede: por omissao a janela do ecra inicial; as paginas que abrem dao a sua, maior. */
    x: Dp = Grelha.janelaX, y: Dp = Grelha.janelaY, largura: Dp = Grelha.janelaLargura, altura: Dp = Grelha.janelaAltura,
    /** Falso deixa o vidro sem o reflexo (por cima de um video, o reflexo lavava a imagem). */
    reflexo: Boolean = true,
    conteudo: @Composable () -> Unit = {},
) {
    val t = rememberInfiniteTransition(label = "boia")
    val sobe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "boia-y")
    val forma = RoundedCornerShape(Grelha.janelaCanto)
    Box(
        modifier.offset(x, y).size(largura, altura)
            .graphicsLayer { if (boia) translationY = -4.dp.toPx() * sobe }
            .drawBehind {
                val b = Grelha.janelaBorda.toPx(); val s = Grelha.janelaSombra.toPx(); val r = Grelha.janelaCanto.toPx()
                val janela = Path().apply { addRoundRect(RoundRect(-b, -b, size.width + b, size.height + b, CornerRadius(r + b))) }
                val desvio = Path().apply { addRoundRect(RoundRect(s - b, s - b, size.width + s + b, size.height + s + b, CornerRadius(r + b))) }
                drawPath(Path.combine(PathOperation.Difference, desvio, janela), sombra)
            },
    ) {
        Box(Modifier.fillMaxSize().clip(forma)) { conteudo() }
        // o reflexo do vidro e os dois contornos, por cima de tudo
        if (reflexo) Box(Modifier.fillMaxSize().clip(forma).background(Brush.linearGradient(0f to Color.White.copy(alpha = 0.16f), 0.32f to Color.Transparent)))
        Canvas(Modifier.fillMaxSize()) {
            val b = Grelha.janelaBorda.toPx(); val r = Grelha.janelaCanto.toPx()
            drawRoundRect(Tinta, Offset(-b * 1.5f, -b * 1.5f), Size(size.width + 3 * b, size.height + 3 * b), CornerRadius(r + b), style = Stroke(b))
            drawRoundRect(Papel, Offset(-b * 0.5f, -b * 0.5f), Size(size.width + b, size.height + b), CornerRadius(r), style = Stroke(b))
        }
    }
}

/** A etiqueta do preco, pendurada por um fio no canto da janela e a balancar; salta quando o preco muda. */
@Composable
fun Etiqueta(
    preco: String, sub: String,
    /** O ponto de onde pende: por omissao o canto da janela do ecra inicial. */
    x: Dp = Grelha.etiquetaX, y: Dp = Grelha.etiquetaY,
    /** Falso deixa-a parada, um nada inclinada: nas paginas com video nada mexe alem do video. */
    balanca: Boolean = true,
) {
    val ang = if (balanca) {
        val t = rememberInfiniteTransition(label = "etiqueta")
        t.animateFloat(-6f, 5f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "balanca")
    } else remember { mutableStateOf(4f) }
    val salto = remember { Animatable(1f) }
    LaunchedEffect(preco, sub) { salto.snapTo(0.6f); salto.animateTo(1f, spring(0.45f, Spring.StiffnessMediumLow)) }
    Box(
        Modifier.offset(x, y)
            // centrada no ponto de onde pende
            .layout { m, c -> val p = m.measure(c.copy(minWidth = 0, minHeight = 0)); layout(0, 0) { p.place(-p.width / 2, 0) } }
            .graphicsLayer { rotationZ = ang.value; transformOrigin = TransformOrigin(0.5f, 0f) },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(1.5.dp).size(1.5.dp, 22.dp).background(Papel))
            Box(
                Modifier.graphicsLayer { scaleX = salto.value; scaleY = salto.value }
                    .drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.38f), Offset(3.dp.toPx(), 3.dp.toPx()), size, CornerRadius(7.dp.toPx())) }
                    .background(Papel, RoundedCornerShape(7.dp))
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(Tinta, Offset.Zero, size, CornerRadius(7.dp.toPx()), style = Stroke(2.dp.toPx()))
                        drawCircle(Tinta, 3.5.dp.toPx(), Offset(size.width / 2, 7.dp.toPx()))
                        drawCircle(Color(0xFFB9B29A), 1.5.dp.toPx(), Offset(size.width / 2, 7.dp.toPx()))
                    }
                    .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                    Text(preco, color = Tinta, fontSize = 29.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false, letterSpacing = (-0.5).sp)
                    if (sub.isNotBlank()) Text(sub, color = Tinta.copy(alpha = 0.7f), fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------
// Flores: o balcao e o carrossel dos frascos no eixo
// ------------------------------------------------------------------------------------------------------------------

@Composable
fun PalcoFlores(flores: List<MenuItem>, foco: Int, fotoDe: (MenuItem) -> String) {
    val t = rememberInfiniteTransition(label = "tampa")
    val tampa by t.animateFloat(0f, 0.1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tampa-flutua")
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, spring(0.55f, Spring.StiffnessLow)) }
    Box(Modifier.fillMaxSize()) {
        Balcao()
        Box(
            Modifier.offset(Grelha.eixo - 80.dp, Grelha.balcaoY - 9.dp).size(160.dp, 14.dp)
                .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)), RoundedCornerShape(50)),
        )
        for (i in (foco - 2).coerceAtLeast(0)..(foco + 2).coerceAtMost(flores.lastIndex)) {
            val item = flores[i]
            key(item.id) { FrascoNoPalco(item, fotoDe(item), i - foco, { if (i == foco) tampa else 0f }, entrada) }
        }
    }
}

@Composable
private fun FrascoNoPalco(item: MenuItem, foto: String, d: Int, tampa: () -> Float, entrada: Animatable<Float, *>) {
    val alvoX = when { d == 0 -> 0f; d == 1 -> 1f; d == -1 -> -1f; d > 1 -> 1.64f; else -> -1.64f }
    val alvoE = when (d) { 0 -> 1f; 1, -1 -> Grelha.frascoLado / Grelha.frascoCentro; else -> 0.33f }
    val alvoA = when (d) { 0 -> 1f; 1, -1 -> 0.42f; else -> 0f }
    val mola = tween<Float>(800, easing = Suave)
    val x by animateFloatAsState(alvoX, mola, label = "x")
    val e by animateFloatAsState(alvoE, mola, label = "e")
    val a by animateFloatAsState(alvoA, tween(600), label = "a")
    val largura = Grelha.frascoCentro; val altura = largura / FrascoGeo.PROPORCAO
    Box(
        Modifier.offset(Grelha.eixo - largura / 2, Grelha.frascoBase - altura).size(largura, altura)
            .graphicsLayer {
                translationX = x * Grelha.frascoAfastamento.toPx()
                scaleX = e; scaleY = e; alpha = a
                transformOrigin = TransformOrigin(0.5f, 1f)
                if (d == 0) { val v = entrada.value; translationY = (1f - v) * -80.dp.toPx(); rotationZ = (1f - v) * -6f; alpha = a * v.coerceIn(0f, 1f) }
            },
    ) {
        Frasco(item = item, fotoUrl = foto, focado = d == 0, modifier = Modifier.fillMaxSize(), abertura = tampa, comHero = d == 0, cresce = false, comRotulo = d == 0)
    }
}

/** O balcao de madeira no traco da marca, centrado no eixo. */
@Composable
private fun Balcao() {
    Canvas(Modifier.offset(Grelha.balcaoX, Grelha.balcaoY).size(Grelha.balcaoLargura, 45.dp)) {
        val b = 2.dp.toPx(); val tampo = 12.dp.toPx(); val r = 5.dp.toPx()
        drawRoundRect(Color(0xFFE3B97C), Offset.Zero, Size(size.width, tampo + r), CornerRadius(r))
        drawRect(Color.White.copy(alpha = 0.25f), Offset(b, b), Size(size.width - 2 * b, 2.5.dp.toPx()))
        drawRect(Brush.verticalGradient(listOf(Color(0xFFC6955A), Color(0xFFA2733F)), tampo, size.height), Offset(0f, tampo), Size(size.width, size.height - tampo))
        var x = 64.dp.toPx()
        while (x < size.width - b) { drawRect(Tinta.copy(alpha = 0.32f), Offset(x, tampo), Size(2.dp.toPx(), size.height - tampo)); x += 70.dp.toPx() }
        drawRoundRect(Tinta, Offset(b / 2, b / 2), Size(size.width - b, size.height - b), CornerRadius(r), style = Stroke(b))
        drawLine(Tinta, Offset(0f, tampo), Offset(size.width, tampo), b)
    }
}

// ------------------------------------------------------------------------------------------------------------------
// Parafernalia: o estudio claro com o pedestal e as fotos reais a trocar de cor
// ------------------------------------------------------------------------------------------------------------------

@Composable
fun Estudio(
    fotos: List<FotoDeCor>, indice: Int,
    /** O lado da janela onde esta: a do ecra inicial por omissao; na pagina que abre e' maior e tudo cresce com ela. */
    largura: Dp = Grelha.janelaLargura, altura: Dp = Grelha.janelaAltura,
) {
    // tudo desenhado para a janela de 260 x 250 do ecra inicial; noutra, as medidas crescem na mesma proporcao
    val k = largura / Grelha.janelaLargura
    val desce = (altura - Grelha.janelaAltura * k) / 2
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color.White, Color(0xFFFAF6EA), Color(0xFFEDE3C6)), center = Offset.Unspecified, radius = 600f * k),
        ),
    ) {
        Box(Modifier.fillMaxSize().drawBehind { drawRect(Brush.verticalGradient(0.66f to Color.Transparent, 1f to Tinta.copy(alpha = 0.07f))) })
        Pedestal(Modifier.offset((largura - 5.dp - 160.dp * k) / 2, desce + 181.dp * k).size(160.dp * k, 48.dp * k), k)
        val caixa = Modifier.offset((largura - 5.dp - 170.dp * k) / 2, desce + 59.dp * k).size(170.dp * k, 136.dp * k)
        AnimatedContent(
            targetState = indice,
            modifier = caixa,
            transitionSpec = {
                (slideInHorizontally(spring(0.55f, Spring.StiffnessLow)) { it / 4 } + fadeIn(tween(300)) + scaleIn(spring(0.55f, Spring.StiffnessLow), 0.9f)) togetherWith
                    (slideOutHorizontally(tween(550, easing = Suave)) { -it / 4 } + fadeOut(tween(450)) + scaleOut(tween(550), 0.9f))
            },
            label = "foto",
        ) { k ->
            val f = fotos.getOrNull(k % fotos.size.coerceAtLeast(1))
            if (f != null) AsyncImage(f.imagem, f.cor, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, alignment = Alignment.BottomCenter)
        }
    }
}

/** O pedestal: o tampo em elipse e o tambor com as riscas a correr (no desenho), a sombra de contacto por cima. */
@Composable
private fun Pedestal(modifier: Modifier, k: Float = 1f) {
    val t = rememberInfiniteTransition(label = "tambor")
    val corre by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "riscas")
    Canvas(modifier) {
        val b = 2.dp.toPx(); val tampoA = 28.dp.toPx() * k; val lado = 32.dp.toPx() * k
        val w = size.width
        // o tambor: um rectangulo com o fundo em meia elipse, riscado, recortado pela propria forma
        val tambor = Path().apply {
            moveTo(0f, tampoA / 2); lineTo(0f, tampoA / 2 + lado - tampoA / 2)
            arcTo(androidx.compose.ui.geometry.Rect(0f, lado - tampoA / 2, w, lado + tampoA / 2), 180f, -180f, false)
            lineTo(w, tampoA / 2); close()
        }
        drawPath(tambor, Color(0xFFC8641F))
        val risca = 20.dp.toPx() * k
        clipPath(tambor) {
            var x = -2 * risca + corre * 2 * risca
            while (x < w) { drawRect(Color(0xFFDE762E), Offset(x, 0f), Size(risca, size.height)); x += 2 * risca }
            drawRect(Brush.horizontalGradient(0f to Tinta.copy(alpha = 0.4f), 0.22f to Color.Transparent, 0.78f to Color.Transparent, 1f to Tinta.copy(alpha = 0.4f)))
        }
        drawPath(tambor, Tinta, style = Stroke(b))
        // o tampo
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFB774), Laranja), center = Offset(w / 2, tampoA * 0.38f), radius = w * 0.5f), Offset.Zero, Size(w, tampoA))
        drawOval(Tinta, Offset.Zero, Size(w, tampoA), style = Stroke(b))
        drawOval(Brush.radialGradient(listOf(Tinta.copy(alpha = 0.42f), Color.Transparent), center = Offset(w / 2, tampoA * 0.5f), radius = w * 0.34f), Offset(w * 0.15f, tampoA * 0.18f), Size(w * 0.7f, tampoA * 0.6f))
    }
}

/** Para os testes e para quem quiser o mesmo amarelo das piteiras noutro sitio. */
internal val SombraPiteiras = Amarelo
internal val SombraParafernalia = Laranja
internal fun Dp.metade() = this / 2
