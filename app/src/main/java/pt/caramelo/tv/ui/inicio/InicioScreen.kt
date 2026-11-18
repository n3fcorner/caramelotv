package pt.caramelo.tv.ui.inicio

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.caramelo.tv.R
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.caramelo.Caramelo
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.components.Aroma
import pt.caramelo.tv.ui.components.precoCurto
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.corDaSeccao

/**
 * O ecra inicial da TV nova (18/11/2026; maqueta 4 aprovada pelo Francisco: "gogo"). Em cima o produto em foco, em
 * tres colunas: a ficha, o palco (o frasco no balcao, a piteira na janela da marca sobre a arte animada, ou a peca
 * real a trocar de cor no pedestal do estudio) e o Caramelo curador. Em baixo as tres filas ao mesmo nivel: Flores
 * (com o CBD, a infusao e o hash), Piteiras e Parafernalia. ◀▶ escolhe, ▲▼ muda de fila, OK abre.
 *
 * [ativo] falso quando outro ecra esta por cima (o leitor, a peca aberta): o video da cena para logo, para nunca haver
 * dois descodificadores ao mesmo tempo, e o Caramelo fica parado.
 */
@Composable
fun InicioScreen(
    catalog: Catalog,
    focoGuardado: String?,
    ativo: Boolean,
    onFoco: (String) -> Unit,
    onFocoFlor: (MenuItem) -> Unit,
    onAbrirFlor: (MenuItem) -> Unit,
    onAbrirArtigo: (Parafernalia) -> Unit,
    onEditar: (MenuItem) -> Unit,
    onAnalises: () -> Unit,
    onSettings: () -> Unit,
) {
    val flores = remember(catalog.items) { Inicio.flores(catalog) }
    val piteiras = remember(catalog.parafernalia) { Inicio.piteiras(catalog) }
    val pecas = remember(catalog.parafernalia) { Inicio.parafernalia(catalog) }
    val foco = remember { FocoDasFilas() }
    val barra = remember { FocusRequester() }
    var emFoco by remember { mutableStateOf<Peca?>(null) }
    // a ultima de cada seccao, para o palco nao ficar vazio a meio de uma troca de fila
    var ultimaFlor by remember { mutableStateOf<MenuItem?>(null) }
    var ultimoArtigo by remember { mutableStateOf<Parafernalia?>(null) }

    // o primeiro foco: onde se estava, senao a primeira flor
    LaunchedEffect(Unit) {
        delay(16)
        val chave = focoGuardado?.takeIf { foco.pedidos.containsKey(it) } ?: flores.firstOrNull()?.let { PecaFlor(it).chave } ?: piteiras.firstOrNull()?.let { PecaArtigo(it).chave }
        chave?.let { runCatching { foco.pedido(it).requestFocus() } }
    }

    val seccao = foco.activa
    val artigo = (emFoco as? PecaArtigo)?.artigo ?: ultimoArtigo
    val flor = (emFoco as? PecaFlor)?.item ?: ultimaFlor ?: flores.firstOrNull()

    // a parafernalia: as fotos reais das cores a trocar de 1,5 em 1,5 s
    var indiceFoto by remember { mutableIntStateOf(0) }
    LaunchedEffect(artigo?.slug, seccao) {
        indiceFoto = 0
        if (seccao != Seccao.PARAFERNALIA || artigo == null || artigo.fotos.size < 2) return@LaunchedEffect
        while (true) { delay(1500); indiceFoto++ }
    }
    val fotos = remember(artigo) { artigo?.let { catalog.fotosDe(it) }.orEmpty() }
    val corAtual = fotos.getOrNull(indiceFoto % fotos.size.coerceAtLeast(1))?.cor.orEmpty()

    // o anel do foco: um so, que desliza para a peca em foco (a altura e' sempre a da fila de cima)
    val anelX = remember { Animatable(0f) }; val anelW = remember { Animatable(0f) }
    var anelAlvo by remember { mutableStateOf(0f to 0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { anelAlvo }.collect { (x, w) ->
            if (anelW.value == 0f) { anelX.snapTo(x); anelW.snapTo(w) }
            else { kotlinx.coroutines.coroutineScope { launch { anelX.animateTo(x, spring(0.62f, Spring.StiffnessMediumLow)) }; launch { anelW.animateTo(w, spring(0.62f, Spring.StiffnessMediumLow)) } } }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Tinta)
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_MENU) { onSettings(); true } else false },
    ) {
        Mundo(
            seccao = seccao,
            cenaUrl = if (seccao == Seccao.PITEIRAS && artigo != null) catalog.cenaDe(artigo) else "",
            cartazUrl = if (artigo != null) catalog.cartazDe(artigo) else "",
            ativo = ativo,
        )

        // os palcos de cada seccao, em fundido com a troca de fila
        AnimatedVisibility(seccao == Seccao.FLORES, enter = fadeIn(tween(600)) + slideInVertically(tween(800)) { it / 14 }, exit = fadeOut(tween(400)) + slideOutVertically(tween(600)) { it / 14 }) {
            PalcoFlores(flores, flores.indexOf(flor).coerceAtLeast(0)) { catalog.imageUrlOf(it) }
        }
        AnimatedVisibility(seccao == Seccao.PITEIRAS, enter = fadeIn(tween(500)) + scaleIn(spring(0.6f, Spring.StiffnessLow), 0.86f), exit = fadeOut(tween(300))) {
            Box(Modifier.fillMaxSize()) {
                MolduraJanela(SombraPiteiras)
                artigo?.let { Etiqueta(precoCurto(it.preco), Inicio.etiquetaDe(it)) }
            }
        }
        AnimatedVisibility(seccao == Seccao.PARAFERNALIA, enter = fadeIn(tween(500)) + scaleIn(spring(0.6f, Spring.StiffnessLow), 0.86f), exit = fadeOut(tween(300)) + scaleOut(tween(300), 0.92f)) {
            Box(Modifier.fillMaxSize()) {
                MolduraJanela(SombraParafernalia, boia = true) { Estudio(fotos, indiceFoto) }
                artigo?.let { Etiqueta(precoCurto(it.preco), Inicio.etiquetaDe(it)) }
            }
        }

        CarameloDoInicio(seccao, ativo)

        val conteudo = when (seccao) {
            Seccao.FLORES -> flor?.let { fichaDaFlor(it) }
            else -> artigo?.let { fichaDoArtigo(it, seccao) }
        }
        if (conteudo != null) {
            val olho = if (seccao == Seccao.PARAFERNALIA && artigo != null) Inicio.olhoComCor(artigo, corAtual) else conteudo.olho
            Ficha(conteudo, olho)
        }

        Cabecalho(seccao, barra, foco, onAnalises)

        Filas(
            flores = flores, piteiras = piteiras, pecas = pecas, foco = foco, barra = barra,
            fotoFlor = { catalog.imageUrlOf(it) }, imagemDe = { catalog.imagemDe(it) },
            onFoco = { p, _ ->
                emFoco = p; onFoco(p.chave)
                when (p) { is PecaFlor -> { ultimaFlor = p.item; onFocoFlor(p.item) }; is PecaArtigo -> ultimoArtigo = p.artigo }
            },
            onAbrir = { p -> when (p) { is PecaFlor -> onAbrirFlor(p.item); is PecaArtigo -> onAbrirArtigo(p.artigo) } },
            onLongo = { p -> (p as? PecaFlor)?.let { onEditar(it.item) } },
            onAnel = { x, w -> anelAlvo = x to w },
        )
        Anel({ anelX.value }, { anelW.value })
        Dica()
    }
}

/** O conteudo da ficha de uma flor: o grupo, o lote (ou o cultivo), o tipo, a barra, os terpenos e os aromas. */
@Composable
private fun fichaDaFlor(item: MenuItem): ConteudoFicha {
    val s = item.strain
    val chip = when (item.section) { Section.CBD -> "CBD"; Section.INFUSAO -> tr("INFUSÃO", "INFUSED"); Section.HASH -> "HASH" }
    val base = if (item.section == Section.HASH) "HASH" else tr("FLOR", "FLOWER")
    val lote = item.analise?.lote?.takeIf { it.isNotBlank() }
    val olho = when {
        lote != null -> "$base · ${tr("LOTE", "BATCH")} $lote"
        s.cultivation.isNotBlank() -> "$base · ${s.cultivation.uppercase()}"
        else -> base
    }
    return ConteudoFicha(
        chave = "f:" + item.id, chip = chip, corChip = corDaSeccao(item.section), chipClaro = item.section == Section.HASH,
        olho = olho, nome = item.displayTitle,
        tipo = Inicio.tipoSemInfusao(s.type).ifBlank { s.genetics },
        indica = s.indicaEfectiva.takeIf { it in 0..100 },
        terpenos = Inicio.terpenosDe(s.terpenes, s.terpenesPct),
        aromas = Aroma.de(s.aroma, 3),
    )
}

@Composable
private fun fichaDoArtigo(p: Parafernalia, s: Seccao): ConteudoFicha {
    val piteira = s == Seccao.PITEIRAS
    return ConteudoFicha(
        chave = "p:" + p.slug,
        chip = if (piteira) tr("PITEIRAS", "TIPS") else tr("PARAFERNÁLIA", "ACCESSORIES"),
        corChip = if (piteira) Amarelo else Laranja, chipClaro = true,
        olho = Inicio.olhoDe(p), nome = Inicio.nomeDe(p), tipo = Inicio.tipoDe(p),
        especificacoes = Inicio.especificacoesDe(p).ifEmpty { listOf(pt.caramelo.tv.data.model.Especificacao(tr("Preço", "Price"), precoCurto(p.preco))) },
    )
}

/** O Caramelo curador na terceira coluna: apresenta as flores, mostra a piteira, acena na parafernalia; salta ao mudar. */
@Composable
private fun CarameloDoInicio(s: Seccao, ativo: Boolean) {
    val salto = remember { Animatable(0f) }
    LaunchedEffect(s) { salto.snapTo(0f); salto.animateTo(1f, tween(700)) }
    val pose = when (s) { Seccao.FLORES -> Pose.Aponta; Seccao.PITEIRAS -> Pose.Mostra; Seccao.PARAFERNALIA -> Pose.Acena }
    Box(
        Modifier.offset(Grelha.carameloX, Grelha.carameloY).size(Grelha.carameloLargura, Grelha.carameloAltura)
            .graphicsLayer {
                val v = salto.value; val ida = if (v < 0.4f) v / 0.4f else (1f - v) / 0.6f
                translationY = -11.dp.toPx() * ida
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
    ) {
        Caramelo(pose, Modifier.fillMaxSize(), parado = !ativo)
    }
}

/** O logotipo e o menu: Flores, Piteiras, Parafernalia, Analises, com o sublinhado na cor da seccao. */
@Composable
private fun Cabecalho(s: Seccao, barra: FocusRequester, foco: FocoDasFilas, onAnalises: () -> Unit) {
    Row(
        Modifier.padding(start = Grelha.margem, end = Grelha.margem, top = 22.dp).fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo_caramelo), "Caramelo do Brasil", Modifier.height(28.dp))
        Image(painterResource(R.drawable.lettering_caramelo), null, Modifier.padding(start = 8.dp).height(21.dp))
        Spacer(Modifier.weight(1f))
        val itens = listOf(
            Triple(tr("Flores", "Flowers"), Seccao.FLORES, corDaFila(Seccao.FLORES)),
            Triple(tr("Piteiras", "Tips"), Seccao.PITEIRAS, Amarelo),
            Triple(tr("Parafernália", "Accessories"), Seccao.PARAFERNALIA, Laranja),
        )
        itens.forEach { (t, sec, cor) ->
            ItemDoMenu(t, s == sec, cor, if (s == sec) Modifier.focusRequester(barra) else Modifier, { foco.daFila(sec) }) { runCatching { foco.daFila(sec)?.requestFocus() } }
        }
        ItemDoMenu(tr("Análises", "Lab reports"), false, Amarelo, Modifier, { foco.daFila(s) }, onAnalises)
    }
}

@Composable
private fun ItemDoMenu(texto: String, activo: Boolean, cor: Color, modifier: Modifier, baixo: () -> FocusRequester?, onClick: () -> Unit) {
    var focado by remember { mutableStateOf(false) }
    val sublinha = remember { Animatable(if (activo) 1f else 0f) }
    LaunchedEffect(activo) { sublinha.animateTo(if (activo) 1f else 0f, spring(0.5f, Spring.StiffnessMediumLow)) }
    Surface(
        onClick = onClick,
        modifier = modifier.padding(start = 24.dp).onFocusChanged { focado = it.isFocused }.focusProperties { baixo()?.let { down = it } },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Papel.copy(alpha = 0.12f), pressedContainerColor = Papel.copy(alpha = 0.2f)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
    ) {
        Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(horizontal = 4.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(texto, color = Papel.copy(alpha = if (activo || focado) 1f else 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1)
            Box(Modifier.padding(top = 3.dp).height(2.5.dp).fillMaxWidth().graphicsLayer { scaleX = sublinha.value }.background(cor, RoundedCornerShape(2.dp)))
        }
    }
}

/** O anel amarelo a volta da peca em foco, desenhado por cima das filas (a peca em foco cresce 6 % e sobe 2 dp). */
@Composable
private fun Anel(x: () -> Float, w: () -> Float) {
    val densidade = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        val largura = w(); if (largura <= 0f) return@Canvas
        val e = 1.06f; val folga = 4.5.dp.toPx()
        val alturaCaixa = Grelha.cartaoAltura.toPx()
        val topo = (Grelha.filasY + 28.dp).toPx()
        val l = x() - largura * (e - 1) / 2 - folga
        val t = topo - alturaCaixa * (e - 1) / 2 - 2.dp.toPx() - folga
        val tam = Size(largura * e + 2 * folga, alturaCaixa * e + 2 * folga)
        drawRoundRect(Amarelo.copy(alpha = 0.16f), Offset(l - 3.5.dp.toPx(), t - 3.5.dp.toPx()), Size(tam.width + 7.dp.toPx(), tam.height + 7.dp.toPx()), CornerRadius(13.dp.toPx()), style = Stroke(3.5.dp.toPx()))
        drawRoundRect(Amarelo, Offset(l, t), tam, CornerRadius(10.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
}

/** A dica do comando, a direita do titulo da fila de cima. */
@Composable
private fun Dica() {
    Row(Modifier.fillMaxWidth().padding(end = Grelha.margem).offset(y = Grelha.filasY + 7.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        listOf("◀ ▶" to tr("escolher", "choose"), "▲ ▼" to tr("secção", "section"), "OK" to tr("abrir", "open")).forEach { (t, d) ->
            Text(
                t, color = Papel.copy(alpha = 0.5f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona,
                modifier = Modifier.padding(start = 7.dp).border(1.dp, Papel.copy(alpha = 0.5f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp),
            )
            Text(d, color = Papel.copy(alpha = 0.5f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, modifier = Modifier.padding(start = 3.dp))
        }
    }
}

