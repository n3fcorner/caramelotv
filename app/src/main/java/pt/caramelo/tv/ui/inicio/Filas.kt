package pt.caramelo.tv.ui.inicio

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.colecao.Frasco
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Vermelho
import pt.caramelo.tv.ui.theme.corDaSeccao

/** Uma entrada de uma fila: uma peca ou o marcador de um grupo da fila das flores. */
sealed interface Entrada_ { val chave: String }
data class EntradaPeca(val peca: Peca, val indice: Int) : Entrada_ { override val chave get() = peca.chave }
data class EntradaGrupo(val seccao: Section) : Entrada_ { override val chave get() = "g:" + seccao.name }

/**
 * O estado do foco nas tres filas: a ultima peca de cada uma (para ▲▼ voltar onde se estava) e os FocusRequester das
 * pecas, por chave. A fila activa e' a da peca em foco.
 */
class FocoDasFilas {
    val ultimo = mutableMapOf<Seccao, String>()
    val pedidos = mutableMapOf<String, FocusRequester>()
    var activa by mutableStateOf(Seccao.FLORES)
    fun pedido(chave: String) = pedidos.getOrPut(chave) { FocusRequester() }
    fun daFila(s: Seccao): FocusRequester? = ultimo[s]?.let { pedidos[it] }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Filas(
    flores: List<MenuItem>,
    piteiras: List<Parafernalia>,
    pecas: List<Parafernalia>,
    foco: FocoDasFilas,
    barra: FocusRequester,
    fotoFlor: (MenuItem) -> String,
    imagemDe: (Parafernalia) -> String,
    onFoco: (Peca, Seccao) -> Unit,
    onAbrir: (Peca) -> Unit,
    onLongo: (Peca) -> Unit,
    onAnel: (x: Float, largura: Float) -> Unit,
) {
    val densidade = LocalDensity.current
    val linha = when (foco.activa) { Seccao.FLORES -> 0; Seccao.PITEIRAS -> 1; Seccao.PARAFERNALIA -> 2 }
    val sobe by animateFloatAsState(linha.toFloat(), tween(800, easing = androidx.compose.animation.core.CubicBezierEasing(0.22f, 0.8f, 0.24f, 1f)), label = "filas")
    // O foco nunca passa da terceira peca a contar do fim, nem fica antes da margem (Inicio.deslize).
    val espec = remember(densidade) {
        val m = with(densidade) { Grelha.margem.toPx() }; val p = with(densidade) { Grelha.passo.toPx() }
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = Inicio.deslize(offset, size, containerSize, m, p)
        }
    }
    val grupos = remember(flores) {
        buildList<Entrada_> {
            var ultimo: Section? = null
            flores.forEachIndexed { i, f -> if (f.section != ultimo) { add(EntradaGrupo(f.section)); ultimo = f.section }; add(EntradaPeca(PecaFlor(f), i)) }
        }
    }
    val filas = listOf(
        Triple(Seccao.FLORES, grupos, Pair(tr("FLORES", "FLOWERS"), tr("${flores.size} frascos · CBD · Infusão · Hash", "${flores.size} jars · CBD · Infused · Hash"))),
        Triple(Seccao.PITEIRAS, piteiras.mapIndexed { i, p -> EntradaPeca(PecaArtigo(p), i) }, Pair(tr("PITEIRAS", "TIPS"), tr("${piteiras.count { it.slug.startsWith("piteira-") }} artes · ${piteiras.count { it.slug.startsWith("kit-") }} kits", "${piteiras.count { it.slug.startsWith("piteira-") }} artworks · ${piteiras.count { it.slug.startsWith("kit-") }} sets"))),
        Triple(Seccao.PARAFERNALIA, pecas.mapIndexed { i, p -> EntradaPeca(PecaArtigo(p), i) }, Pair(tr("PARAFERNÁLIA", "ACCESSORIES"), tr("grinders · cuias · cinzeiros · bandejas", "grinders · bowls · ashtrays · trays"))),
    )
    Box(
        Modifier.offset(y = Grelha.filasY).fillMaxWidth().height(540.dp - Grelha.filasY)
            // as pontas e o fundo esvaem-se: uma camada so, por cima das tres filas
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; clip = true }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0.66f to Color.Black, 0.88f to Color.Black.copy(alpha = 0.35f), 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                val m = Grelha.margem.toPx()
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, (20.dp.toPx() / size.width) to Color.Transparent, (m / size.width) to Color.Black, (1f - m / size.width) to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            },
    ) {
        // as tres filas tem a altura toda (360 dp), maior que a janela: sem o unbounded a coluna dava a segunda o que
        // sobrava e a terceira ficava com zero
        Column(Modifier.wrapContentHeight(Alignment.Top, unbounded = true).graphicsLayer { translationY = -sobe * Grelha.filaAltura.toPx() }) {
            filas.forEachIndexed { n, (s, entradas, titulos) ->
                val activa = s == foco.activa
                val opacidade by animateFloatAsState(if (activa) 1f else 0.42f, tween(500), label = "fila")
                Column(Modifier.height(Grelha.filaAltura).graphicsLayer { alpha = opacidade }) {
                    Cabecalho(titulos.first, titulos.second, corDaFila(s))
                    CompositionLocalProvider(LocalBringIntoViewSpec provides espec) {
                        LazyRow(
                            contentPadding = PaddingValues(start = Grelha.margem, end = Grelha.margem),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Grelha.cartaoEspaco),
                            modifier = Modifier.fillMaxWidth().height(Grelha.cartaoAltura + 26.dp),
                        ) {
                            itemsIndexed(entradas, key = { _, e -> e.chave }) { _, e ->
                                when (e) {
                                    is EntradaGrupo -> Separador(e.seccao)
                                    is EntradaPeca -> Cartao(
                                        e.peca, s, n, foco, barra, fotoFlor, imagemDe, onFoco, onAbrir, onLongo, onAnel,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun corDaFila(s: Seccao): Color = when (s) {
    Seccao.FLORES -> corDaSeccao(Section.INFUSAO)
    Seccao.PITEIRAS -> pt.caramelo.tv.ui.theme.Amarelo
    Seccao.PARAFERNALIA -> pt.caramelo.tv.ui.theme.Laranja
}

@Composable
private fun Cabecalho(titulo: String, sub: String, cor: Color) {
    Row(Modifier.padding(start = Grelha.margem, top = 6.dp).height(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(cor, RoundedCornerShape(1.5.dp)))
        Spacer(Modifier.width(8.dp))
        Text(titulo, color = Papel, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontFamily = Carbona, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(sub, color = Papel.copy(alpha = 0.6f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, fontFamily = Carbona, maxLines = 1)
    }
    Spacer(Modifier.height(7.dp))
}

/** Roda o filho um quarto de volta, trocando a largura pela altura na medicao (o rotationZ so, cortava o texto). */
private fun Modifier.aoAlto() = layout { m, _ ->
    val p = m.measure(androidx.compose.ui.unit.Constraints())
    layout(p.height, p.width) { p.placeWithLayer(-(p.width - p.height) / 2, (p.width - p.height) / 2) { rotationZ = -90f } }
}

/** O marcador de um grupo dentro da fila das flores: um traco na cor e o nome ao alto. */
@Composable
private fun Separador(s: Section) {
    val cor = corDaSeccao(s)
    Box(Modifier.width(Grelha.separadorLargura).height(Grelha.cartaoAltura), contentAlignment = Alignment.Center) {
        Box(Modifier.width(2.dp).fillMaxSize().padding(vertical = 4.dp).background(cor, RoundedCornerShape(1.dp)))
        Text(
            when (s) { Section.CBD -> "CBD"; Section.INFUSAO -> tr("INFUSÃO", "INFUSED"); Section.HASH -> "HASH" },
            color = cor, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, fontFamily = Carbona, maxLines = 1, softWrap = false,
            modifier = Modifier.aoAlto().background(Tinta).padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun Cartao(
    peca: Peca, s: Seccao, n: Int, foco: FocoDasFilas, barra: FocusRequester,
    fotoFlor: (MenuItem) -> String, imagemDe: (Parafernalia) -> String,
    onFoco: (Peca, Seccao) -> Unit, onAbrir: (Peca) -> Unit, onLongo: (Peca) -> Unit, onAnel: (Float, Float) -> Unit,
) {
    var focado by remember { mutableStateOf(false) }
    val e by animateFloatAsState(if (focado) 1f else 0f, spring(0.55f, 380f), label = "cartao")
    val nome = when (peca) { is PecaFlor -> peca.item.displayTitle; is PecaArtigo -> Inicio.nomeDe(peca.artigo) }
    val filas = listOf(Seccao.FLORES, Seccao.PITEIRAS, Seccao.PARAFERNALIA)
    Column(Modifier.width(Grelha.cartaoLargura), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { onAbrir(peca) },
            onLongClick = { onLongo(peca) },
            modifier = Modifier.size(Grelha.cartaoLargura, Grelha.cartaoAltura)
                .focusRequester(foco.pedido(peca.chave))
                .focusProperties {
                    up = filas.getOrNull(n - 1)?.let { foco.daFila(it) } ?: barra
                    down = filas.getOrNull(n + 1)?.let { foco.daFila(it) } ?: FocusRequester.Default
                }
                .onFocusChanged { f ->
                    focado = f.isFocused
                    if (f.isFocused) { foco.ultimo[s] = peca.chave; foco.activa = s; onFoco(peca, s) }
                }
                .onGloballyPositioned { if (focado) { val r = it.boundsInRoot(); onAnel(r.left, r.width) } }
                .graphicsLayer { val k = 1f + 0.06f * e; scaleX = k; scaleY = k; translationY = -2.dp.toPx() * e },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent, pressedContainerColor = Color.Transparent),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
            glow = ClickableSurfaceDefaults.glow(),
        ) {
            when (peca) {
                is PecaFlor -> FrascoPequeno(peca.item, fotoFlor(peca.item))
                is PecaArtigo -> CaixaDoArtigo(peca.artigo, imagemDe(peca.artigo), escura = s == Seccao.PITEIRAS)
            }
        }
        Text(
            nome, color = Papel.copy(alpha = if (focado) 1f else 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.5.dp).height(10.dp),
        )
    }
}


@Composable
private fun FrascoPequeno(item: MenuItem, foto: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Frasco(item = item, fotoUrl = foto, focado = false, modifier = Modifier.width(50.dp), comHero = false, cresce = false, comRotulo = false)
        val selo = when { item.brevemente -> tr("EM BREVE", "SOON"); item.esgotado -> tr("ESGOTADO", "SOLD OUT"); else -> null }
        if (selo != null) Text(
            selo, color = if (item.esgotado) Vermelho else Tinta, fontSize = 6.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp, fontFamily = Carbona, maxLines = 1, softWrap = false,
            modifier = Modifier.align(Alignment.Center).offset(y = 10.dp).graphicsLayer { rotationZ = -8f }
                .background(Papel, RoundedCornerShape(3.dp)).border(1.5.dp, if (item.esgotado) Vermelho else Tinta, RoundedCornerShape(3.dp)).padding(horizontal = 3.5.dp, vertical = 1.5.dp),
        )
    }
}

/** As pecas: as piteiras num nicho escuro (o azul do giro), a parafernalia num estudio claro com contorno tinta. */
@Composable
private fun CaixaDoArtigo(p: Parafernalia, imagem: String, escura: Boolean) {
    val forma = RoundedCornerShape(8.dp)
    val fundo = if (escura) Modifier.background(Nicho, forma).border(1.5.dp, Papel.copy(alpha = 0.16f), forma)
    else Modifier.background(Brush.radialGradient(listOf(Color.White, Color(0xFFF7F1E0), Color(0xFFE9DEBE))), forma).border(1.5.dp, Tinta, forma)
    Box(Modifier.fillMaxSize().then(fundo).padding(7.dp)) {
        AsyncImage(imagem, p.nome, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        if (p.esgotado) Text(
            tr("ESGOTADO", "SOLD OUT"), color = Vermelho, fontSize = 6.5.sp, fontWeight = FontWeight.Black, fontFamily = Carbona, maxLines = 1,
            modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = -8f }.background(Papel, RoundedCornerShape(3.dp)).border(1.5.dp, Vermelho, RoundedCornerShape(3.dp)).padding(horizontal = 3.5.dp, vertical = 1.5.dp),
        )
    }
}
