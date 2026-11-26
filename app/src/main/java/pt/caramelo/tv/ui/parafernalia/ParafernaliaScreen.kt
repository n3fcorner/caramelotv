package pt.caramelo.tv.ui.parafernalia

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import pt.caramelo.tv.R
import pt.caramelo.tv.data.CacheDeMedia
import pt.caramelo.tv.data.model.FotoDeCor
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.ui.components.precoCurto
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.inicio.Estado
import pt.caramelo.tv.ui.inicio.Estudio
import pt.caramelo.tv.ui.inicio.Etiqueta
import pt.caramelo.tv.ui.inicio.Ficha
import pt.caramelo.tv.ui.inicio.FundoDaPagina
import pt.caramelo.tv.ui.inicio.Grelha
import pt.caramelo.tv.ui.inicio.GrelhaPagina
import pt.caramelo.tv.ui.inicio.Inicio
import pt.caramelo.tv.ui.inicio.LuzDaParafernalia
import pt.caramelo.tv.ui.inicio.MolduraJanela
import pt.caramelo.tv.ui.inicio.Nicho
import pt.caramelo.tv.ui.inicio.PeDaPagina
import pt.caramelo.tv.ui.inicio.Seccao
import pt.caramelo.tv.ui.inicio.TopoDaPagina
import pt.caramelo.tv.ui.inicio.fichaDoArtigo
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Vermelho

/**
 * Uma peca aberta (uma piteira, um kit, um artigo de parafernalia): o OK numa peca do ecra inicial.
 *
 * No mundo do ecra inicial (22/11/2026, Francisco, na TV da loja: "o player e a expansao das paginas ainda esta no tema
 * antigo, preciso que coloques tudo consistente"). Era o papel bege de 25/10, com uma grelha por categoria por baixo; a
 * grelha saiu, porque as pecas escolhem-se nas filas do ecra inicial e ja ninguem la chegava. Fica so a peca: a mesma
 * ficha na primeira coluna (o que e', o nome, o material, a ficha tecnica) e a janela da marca maior no lugar do palco.
 * La dentro, a peca a dar a volta inteira em loop (o video tirado do modelo 3D, nas piteiras e nos kits) ou, sem video,
 * as fotos reais das cores a trocar no pedestal do estudio, como no ecra inicial. O preco pende do canto na etiqueta.
 *
 * Um video de cada vez: o ecra inicial para o dele antes de este comecar ([ativo] no InicioScreen).
 */
@OptIn(UnstableApi::class)
@Composable
fun ParafernaliaScreen(
    /** A peca a mostrar; nula se entretanto saiu do catalogo (a pagina fecha-se sozinha). */
    item: Parafernalia?,
    giroUrl: String,
    fotos: List<FotoDeCor>,
    onSair: () -> Unit,
) {
    if (item == null) { LaunchedEffect(Unit) { onSair() }; return }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val temVideo = giroUrl.isNotBlank()
    // Um leitor por peca aberta: liberta-se sempre no onDispose.
    val player = remember(item.slug) { if (temVideo) CacheDeMedia.leitor(context) else null }
    if (player != null) {
        DisposableEffect(player) {
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

    // Sem video, as fotos reais das cores trocam de 1,5 em 1,5 s, como no pedestal do ecra inicial.
    var indice by remember(item.slug) { mutableIntStateOf(0) }
    LaunchedEffect(item.slug, temVideo, fotos.size) {
        if (temVideo || fotos.size < 2) return@LaunchedEffect
        while (true) { delay(1500); indice++ }
    }

    // A pagina apanha o foco: nao ha nada para escolher aqui, e o Voltar sai pela raiz.
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    CarameloTheme {
        Box(Modifier.fillMaxSize().background(Tinta).focusRequester(foco).focusable()) {
            PaginaDaPeca(
                item = item, fotos = fotos, indice = indice, abre = true,
                video = if (player == null) null else {
                    {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                                    this.player = player
                                    setShutterBackgroundColor(Nicho.toArgb())
                                    setBackgroundColor(Nicho.toArgb())
                                }
                            },
                            onRelease = { it.player = null },
                        )
                    }
                },
            )
        }
    }
}

/**
 * A pagina de uma peca, so desenho ([video] nulo: as fotos no pedestal). `internal` para os testes de imagem a
 * desenharem sem ExoPlayer. Com video, nada mais mexe (a luz e a etiqueta paradas); sem video, e' o ecra inicial: a luz
 * a respirar, a etiqueta a balancar e as cores a trocar.
 */
@Composable
internal fun PaginaDaPeca(
    item: Parafernalia, fotos: List<FotoDeCor>, indice: Int,
    /** Falso desenha a janela ja aberta (os testes de imagem); verdadeiro fa-la crescer ao abrir. */
    abre: Boolean = false,
    video: (@Composable () -> Unit)?,
) {
    val piteira = Inicio.ehPiteira(item)
    val seccao = if (piteira) Seccao.PITEIRAS else Seccao.PARAFERNALIA
    val corAtual = fotos.getOrNull(indice % fotos.size.coerceAtLeast(1))?.cor.orEmpty()
    // A expansao: a janela nasce do tamanho da do ecra inicial (260 dp, dois tercos desta), no mesmo canto de cima a esquerda,
    // e cresce ate ao tamanho da pagina com um nada de mola. A etiqueta vem agarrada ao canto. So escala (uma camada): a
    // janela nao foi feita peca partilhada porque, a meio do voo, a camada com alfa cortava a moldura e a sombra.
    val cresce = remember { Animatable(if (abre) Grelha.janelaLargura / GrelhaPagina.janela else 1f) }
    LaunchedEffect(Unit) { if (abre) { delay(110); cresce.animateTo(1f, spring(0.72f, Spring.StiffnessMediumLow)) } }
    Box(Modifier.fillMaxSize()) {
        FundoDaPagina(LuzDaParafernalia, vivo = video == null)
        Box(Modifier.fillMaxSize().graphicsLayer {
            scaleX = cresce.value; scaleY = cresce.value
            transformOrigin = TransformOrigin(GrelhaPagina.janelaX.toPx() / size.width, GrelhaPagina.janelaY.toPx() / size.height)
        }) {
        MolduraJanela(
            if (piteira) Amarelo else Laranja,
            x = GrelhaPagina.janelaX, y = GrelhaPagina.janelaY, largura = GrelhaPagina.janela, altura = GrelhaPagina.janela,
            reflexo = video == null,
        ) {
            if (video != null) Box(Modifier.fillMaxSize().background(Nicho)) { video() }
            else Estudio(fotos, indice, GrelhaPagina.janela, GrelhaPagina.janela)
        }
        precoCurto(item.preco).takeIf { it.isNotBlank() }?.let {
            Etiqueta(it, Inicio.etiquetaDe(item), GrelhaPagina.etiquetaX, GrelhaPagina.etiquetaY, balanca = video == null)
        }
        }
        val conteudo = fichaDoArtigo(item, seccao)
        // a ficha ja estava no ecra inicial, no mesmo sitio: nao volta a entrar
        Ficha(conteudo, if (piteira || video != null) conteudo.olho else Inicio.olhoComCor(item, corAtual), entrada = false)
        PeDaPagina {
            when {
                item.esgotado -> Estado(tr("Esgotado", "Sold out"), Vermelho)
                video != null -> Estado(tr("A peça a 360°", "The piece at 360°"), if (piteira) Amarelo else Laranja)
                fotos.size > 1 -> Estado(tr("${fotos.size} cores, uma a uma", "${fotos.size} colours, one by one"), Laranja)
            }
        }
        TopoDaPagina(listOf("↩" to tr("voltar", "back")))
    }
}
