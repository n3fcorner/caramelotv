package pt.caramelo.tv.ui.colecao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import pt.caramelo.tv.R
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.caramelo.Caramelo
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.components.AromasIlustrados
import pt.caramelo.tv.ui.components.precoTxt
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.corDaSeccao

/**
 * A colecao do Caramelo, o ecra inicial da app nova (18/11/2026, Francisco: "a aplicacao da TV para ser um Caramelo; as
 * strains em frasquinhos"). Os frascos de vidro numa parede azul escura, em prateleiras de madeira no traco da marca; o
 * Caramelo no canto, a apresentar o frasco em foco; a legenda por baixo com o nome, o tipo, os aromas e o preco.
 *
 * Navegar: esquerda e direita andam pela prateleira, cima e baixo mudam de prateleira, cima na primeira vai a barra.
 * OK abre o frasco (por agora o leitor de sempre; a ficha nova e' a fase 2 do plano).
 */
private val Parede = Color(0xFF0A2E52)       // um tom acima da Tinta, para os frascos e a madeira saltarem
private val ParedeLinha = Color(0xFF113A63)
private val Madeira = Color(0xFFE3B97C)
private val MadeiraFrente = Color(0xFFC6955A)
private val Chao = Color(0xFF8A5E34)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColecaoScreen(
    catalog: Catalog,
    focusedId: String?,
    onFocusItem: (MenuItem) -> Unit,
    onOpenItem: (MenuItem) -> Unit,
    onEditItem: (MenuItem) -> Unit,
    onSettings: () -> Unit,
    onMontra: () -> Unit,
    onCoa: () -> Unit,
    onParafernalia: () -> Unit,
    onIdioma: () -> Unit = {},
) {
    val prateleiras = remember(catalog.items) { PlanoDaColecao.de(catalog.items.filter { it.title.isNotBlank() }) }
    val todos = remember(prateleiras) { prateleiras.flatMap { it.itens } }
    val requesters = remember(todos.map { it.id }) { todos.associate { it.id to FocusRequester() } }
    val barra = remember { FocusRequester() }
    var emFoco by remember { mutableStateOf(todos.firstOrNull { it.id == focusedId } ?: todos.firstOrNull()) }

    // O foco nasce no ultimo frasco visto (ao voltar do leitor) ou no primeiro; outra vez quando a lista muda.
    var temFoco by remember { mutableStateOf(false) }
    LaunchedEffect(todos.map { it.id }) {
        withFrameNanos { }
        if (temFoco) return@LaunchedEffect
        val alvo = focusedId?.let { requesters[it] } ?: requesters[todos.firstOrNull()?.id]
        runCatching { (alvo ?: barra).requestFocus() }
    }

    // O Caramelo apresenta o frasco em foco: so quando o foco pousa 600 ms, e no maximo uma vez a cada 4 s (revisao
    // tecnica: nao reagir a cada carregar na seta, que o loop novo ainda nao carregou e o processador e' fraco).
    var pose by remember { mutableStateOf(Pose.Espera) }
    var ultimaReacao by remember { mutableStateOf(0L) }
    LaunchedEffect(emFoco?.id) {
        delay(600)
        val agora = System.currentTimeMillis()
        if (agora - ultimaReacao < 4_000) return@LaunchedEffect
        ultimaReacao = agora
        pose = Pose.Aponta
        delay(3_200)
        pose = Pose.Espera
    }

    CarameloTheme {
        Box(
            Modifier.fillMaxSize()
                .onFocusChanged { temFoco = it.hasFocus }
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.Menu) { onSettings(); true } else false }
                .drawBehind { parede() },
        ) {
            Column(Modifier.fillMaxSize()) {
                BarraDeTopo(
                    activa = 0, focusRequester = barra,
                    onFlores = {}, onParafernalia = onParafernalia.takeIf { catalog.parafernalia.isNotEmpty() }, onAnalises = onCoa,
                    onMontra = onMontra, onSettings = onSettings, onIdioma = onIdioma,
                    baixo = requesters[emFoco?.id ?: todos.firstOrNull()?.id],
                )
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().padding(top = Espaco.medio),
                        verticalArrangement = Arrangement.spacedBy(Espaco.medio),
                    ) {
                        prateleiras.forEachIndexed { i, p ->
                            Prateleira(
                                prateleira = p, catalog = catalog, requesters = requesters,
                                emFocoId = emFoco?.id,
                                cima = if (i == 0) barra else null,
                                onFoco = { emFoco = it; onFocusItem(it) },
                                onOpen = onOpenItem, onEdit = onEditItem,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
                        Legenda(emFoco, Modifier.fillMaxWidth().height(96.dp).padding(start = Overscan.horizontal, bottom = Espaco.curto))
                    }
                    Caramelo(pose, Modifier.width(170.dp).fillMaxHeight().padding(bottom = 10.dp, end = Espaco.medio))
                }
            }
        }
    }
}

/** A parede: azul escuro com os paineis de madeira pintada em traco fino, o rodape e o chao de madeira. So desenho. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.parede() {
    drawRect(Parede)
    val passo = 150.dp.toPx(); var x = passo / 2
    while (x < size.width) { drawRect(ParedeLinha, Offset(x, 0f), Size(2.dp.toPx(), size.height)); x += passo }
    val chao = 74.dp.toPx()
    drawRect(Chao, Offset(0f, size.height - chao), Size(size.width, chao))
    drawRect(Tinta, Offset(0f, size.height - chao), Size(size.width, 3.dp.toPx()))
    drawRect(Color(0xFF6E4A28), Offset(0f, size.height - chao + 3.dp.toPx()), Size(size.width, 6.dp.toPx()))
}

/** Onde assenta o frasco em foco: nunca encostado a borda, como na fila de sempre (FocoComFolga do ecra antigo). */
@OptIn(ExperimentalFoundationApi::class)
private class FocoComFolga(private val folga: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val fim = offset + size; val antes = folga.coerceAtMost((containerSize - size) / 2f)
        return when { offset < antes -> offset - antes; fim > containerSize - antes -> fim - (containerSize - antes); else -> 0f }
    }
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun Prateleira(
    prateleira: Prateleira,
    catalog: Catalog,
    requesters: Map<String, FocusRequester>,
    emFocoId: String?,
    cima: FocusRequester?,
    onFoco: (MenuItem) -> Unit,
    onOpen: (MenuItem) -> Unit,
    onEdit: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ultimo by remember { mutableStateOf<String?>(null) }
    val densidade = LocalDensity.current
    BoxWithConstraints(modifier) {
        val tabua = 16.dp
        // A folga da mola do foco (12 %) e do levantar (10 dp) por cima do frasco.
        val alto = (maxHeight - tabua) / 1.14f - 10.dp
        val largo = alto * FrascoGeo.PROPORCAO
        val spec = remember(densidade, largo) { with(densidade) { FocoComFolga((Overscan.horizontal + largo * 0.6f).toPx()) } }
        // A tabua: madeira clara com a frente mais escura e o traco a tinta, a toda a largura.
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(tabua).drawBehind {
                val t = 2.5.dp.toPx()
                drawRect(Madeira, size = Size(size.width, size.height * 0.45f))
                drawRect(MadeiraFrente, Offset(0f, size.height * 0.45f), Size(size.width, size.height * 0.55f))
                drawRect(Tinta, size = Size(size.width, t))
                drawRect(Tinta, Offset(0f, size.height * 0.45f - t / 2), Size(size.width, t))
                drawRect(Tinta, Offset(0f, size.height - t), Size(size.width, t))
            },
        )
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
            Row(
                Modifier.fillMaxSize()
                    .focusProperties { enter = { ultimo?.let { requesters[it] } ?: FocusRequester.Default } }
                    .focusGroup()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = Overscan.horizontal, end = Overscan.horizontal * 2, bottom = tabua - 2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Espaco.largo),
            ) {
                prateleira.trocos.forEach { troco ->
                    Placa(troco.seccao, Modifier.padding(bottom = 4.dp))
                    troco.itens.forEach { item ->
                        FrascoNaPrateleira(
                            item = item, fotoUrl = catalog.imageUrlOf(item), focado = item.id == emFocoId,
                            focusRequester = requesters[item.id],
                            onFoco = { ultimo = item.id; onFoco(item) },
                            onClick = { onOpen(item) }, onLongClick = { onEdit(item) },
                            modifier = Modifier.width(largo).height(alto * 1.0f).let { m -> cima?.let { c -> m.focusProperties { up = c } } ?: m },
                        )
                    }
                }
            }
        }
    }
}

/** A placa da seccao na ponta do troco: o nome em Carbona sobre a cor da seccao, com o traco a tinta. */
@Composable
private fun Placa(s: Section, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(4.dp)
    Text(
        termo(s.label).uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, letterSpacing = 2.sp),
        color = if (s == Section.HASH) Tinta else Papel,
        modifier = modifier.background(corDaSeccao(s), forma).border(2.dp, Tinta, forma).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}


@Composable
private fun FrascoNaPrateleira(
    item: MenuItem,
    fotoUrl: String,
    focado: Boolean,
    focusRequester: FocusRequester?,
    onFoco: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .zIndex(if (focado) 1f else 0f)
            .onFocusChanged { if (it.isFocused) onFoco() }
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, focusedContainerColor = Color.Transparent, pressedContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Frasco(item, fotoUrl, focado, Modifier.fillMaxHeight())
            // A marca do foco: uma barra amarela por baixo do frasco (regra 3: o foco e' sempre amarelo).
            if (focado) Box(Modifier.align(Alignment.BottomCenter).offset(y = 9.dp).width(36.dp).height(4.dp).background(Amarelo, RoundedCornerShape(2.dp)))
        }
    }
}

/** A legenda do frasco em foco: o nome grande, o tipo e a linha, os aromas desenhados e o preco. Le-se, nao se navega. */
@Composable
private fun Legenda(item: MenuItem?, modifier: Modifier = Modifier) {
    if (item == null) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaco.seccao)) {
        Column(Modifier.width(330.dp)) {
            Text(item.title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp), color = Creme, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val tipo = item.strain.type.substringBefore(", com ").ifBlank { item.strain.line }
            Text(
                listOf(termo(item.section.label), termo(tipo), precoTxt(item.preco)).filter { it.isNotBlank() }.joinToString("  ·  "),
                style = MaterialTheme.typography.titleSmall, color = Amarelo, maxLines = 1,
            )
            if (item.esgotado) Text(tr("Esgotado por agora", "Sold out for now"), style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF8A80))
        }
        if (item.strain.aroma.isNotBlank()) AromasIlustrados(item.strain.aroma, termo(item.strain.aroma), tamanho = 30.dp)
        Spacer(Modifier.weight(1f))
    }
}

/**
 * A barra de cima: o logotipo e o nome da marca a esquerda, as tres salas (Flores, Parafernalia, Analises) e, mais
 * discretos, a montra, a lingua e as definicoes. Baixo volta ao frasco onde se estava.
 */
@Composable
fun BarraDeTopo(
    activa: Int,
    focusRequester: FocusRequester,
    onFlores: () -> Unit,
    onParafernalia: (() -> Unit)?,
    onAnalises: () -> Unit,
    onMontra: () -> Unit,
    onSettings: () -> Unit,
    onIdioma: () -> Unit,
    baixo: FocusRequester?,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(Tinta)
            .focusProperties { if (baixo != null) down = baixo }
            .padding(start = Overscan.horizontal, end = Overscan.horizontal, top = Overscan.vertical * 0.6f, bottom = Espaco.curto),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo_caramelo), "Caramelo do Brasil", Modifier.height(44.dp))
        Image(painterResource(R.drawable.lettering_caramelo), "Caramelo do Brasil", Modifier.padding(start = Espaco.medio).height(34.dp))
        Spacer(Modifier.weight(1f))
        Separador(tr("Flores", "Flowers"), activa == 0, onFlores, if (activa == 0) Modifier.focusRequester(focusRequester) else Modifier)
        onParafernalia?.let { Separador(tr("Parafernália", "Accessories"), activa == 1, it, if (activa == 1) Modifier.focusRequester(focusRequester) else Modifier) }
        Separador(tr("Análises", "Lab reports"), activa == 2, onAnalises, if (activa == 2) Modifier.focusRequester(focusRequester) else Modifier)
        Spacer(Modifier.width(Espaco.largo))
        Separador(tr("Montra", "Showcase"), false, onMontra, discreto = true)
        Separador("PT · EN", false, onIdioma, discreto = true)
        Separador(tr("Definições", "Settings"), false, onSettings, discreto = true)
    }
}

@Composable
private fun Separador(texto: String, activo: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, discreto: Boolean = false) {
    var focado by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged { focado = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, contentColor = if (activo) Amarelo else if (discreto) Creme.copy(alpha = 0.7f) else Creme,
            focusedContainerColor = Amarelo, focusedContentColor = Tinta,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border(BorderStroke(2.dp, Tinta), shape = RoundedCornerShape(8.dp))),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(texto, style = if (discreto) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium)
            if (activo && !focado) Box(Modifier.padding(top = 3.dp).width(28.dp).height(3.dp).background(Amarelo, RoundedCornerShape(2.dp)))
        }
    }
}

