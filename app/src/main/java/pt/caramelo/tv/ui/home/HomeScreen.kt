package pt.caramelo.tv.ui.home

import androidx.compose.animation.AnimatedContent
import pt.caramelo.tv.ui.theme.Laranja
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.R
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.animation.Entrada
import pt.caramelo.tv.ui.animation.heroBounds
import pt.caramelo.tv.ui.components.FundoDaMarca
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.CremeFraco
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Movimento
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.RoxoProfundo
import pt.caramelo.tv.ui.theme.VerdeClaro

/**
 * Ecra inicial: tres submenus, CBD, Infusao e Hash (Francisco, 29/9/2026: "tens que
 * conseguir encaixar os artigos todos no ecra da TV, mesmo que separes 3 submenus").
 *
 *   +--------------------------------------------------+
 *   |  logo            [CBD] [INFUSAO] [HASH]   botoes  |
 *   |  +--------------------------------------------+  |
 *   |  |  todos os artigos do submenu, sem rolar    |  |
 *   |  +--------------------------------------------+  |
 *   +--------------------------------------------------+
 *
 * Com as tres seccoes lado a lado cabiam 24 mosaicos pequenos e nada mais; o catalogo
 * cresce. Um submenu de cada vez da o ecra inteiro a uma linha, e a grelha escolhe o
 * numero de colunas que da o maior mosaico onde cabem todos (TileGrid, colunas 0).
 * O separador muda ao ganhar foco, como nas apps de TV: esquerda e direita trocam de
 * linha, baixo entra nos produtos. A troca e' um esbatimento curto, sem escala: numa
 * TCL cada camada a mais conta.
 */
private val ORDEM_SECCOES = listOf(Section.CBD, Section.INFUSAO, Section.HASH)

/** A cor de cada categoria: a barra do cabecalho e o separador fazem par. */
private fun corDe(s: Section): Color = when (s) {
    Section.CBD -> VerdeClaro
    Section.INFUSAO -> Amarelo
    Section.HASH -> Laranja
}

@Composable
fun HomeScreen(
    catalog: Catalog,
    focusedId: String?,
    onFocusItem: (MenuItem) -> Unit,
    onOpenItem: (MenuItem) -> Unit,
    onEditItem: (MenuItem) -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onMontra: () -> Unit,
    onCoa: () -> Unit,
    onInteracao: () -> Unit,
    onIdioma: () -> Unit = {},
    /** Verdadeiro na primeira vez que o menu aparece: os mosaicos entram em cascata. */
    cascata: Boolean = false,
    onMostrado: () -> Unit = {},
) {
    // O submenu volta a ser o do ultimo produto visto: ao sair dos detalhes nao se
    // cai noutra linha.
    val inicial = catalog.items.firstOrNull { it.id == focusedId }?.section ?: ORDEM_SECCOES.first()
    var seccao by rememberSaveable { mutableStateOf(inicial) }
    val itens = remember(catalog.items, seccao) { catalog.section(seccao) }

    // Um FocusRequester por mosaico: e o que devolve o foco ao sitio certo ao voltar
    // dos detalhes ou do leitor.
    val requesters = remember(catalog.items.size) {
        catalog.items.associate { it.id to FocusRequester() }
    }
    val separadores = remember { ORDEM_SECCOES.associateWith { FocusRequester() } }

    LaunchedEffect(Unit) {
        // Esperar pelo primeiro frame: o FocusRequester so responde depois do no colocado.
        withFrameNanos { }
        val alvo = focusedId?.takeIf { id -> itens.any { it.id == id } }?.let { requesters[it] }
            ?: requesters[itens.firstOrNull()?.id] ?: separadores[seccao]
        runCatching { alvo?.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
    FundoDaMarca()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { evento ->
                // Qualquer tecla adia a montra automatica, mesmo as que nao mexem
                // o foco (por exemplo carregar na margem da grelha).
                onInteracao()
                if (evento.type == KeyEventType.KeyUp && evento.key == Key.Menu) {
                    onSettings()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
        verticalArrangement = Arrangement.spacedBy(Espaco.medio),
    ) {
        // Depois da primeira cascata, marca-se: nas vezes seguintes o menu ja
        // esta composto quando a foto chega do leitor.
        LaunchedEffect(Unit) { onMostrado() }

        Cabecalho(
            temContinuar = catalog.lastWatched() != null,
            onContinue = onContinue,
            onSettings = onSettings,
            onMontra = onMontra,
            onCoa = onCoa,
            onIdioma = onIdioma,
        )

        // Tres barras por baixo do cabecalho, uma por categoria na cor dela (pedido do Leo e do Francisco,
        // 29/9/2026): cada barra faz par com o seu separador. A da categoria aberta engrossa.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            ORDEM_SECCOES.forEach { s ->
                val grossura by animateDpAsState(if (s == seccao) 7.dp else 3.dp, tween(220), label = "barra")
                Box(Modifier.fillMaxWidth().height(grossura).background(corDe(s)))
            }
        }

        // Os tres submenus a esquerda e a legenda dos pontos a direita, na mesma linha:
        // a grelha fica com a altura toda que sobra.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(Espaco.curto)) {
                ORDEM_SECCOES.forEach { s ->
                    Separador(
                        titulo = termo(s.label),
                        total = catalog.section(s).size,
                        activo = s == seccao,
                        onFoco = { seccao = s },
                        focusRequester = separadores.getValue(s),
                        cor = corDe(s),
                    )
                }
            }
            LegendaDeCultivo()
        }

        // So a grelha troca; o cabecalho fica parado. contentKey pela seccao: gravar a
        // posicao de um video nao volta a animar a grelha.
        AnimatedContent(
            targetState = seccao,
            transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(120))) },
            label = "submenu",
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { s ->
            val daSeccao = if (s == seccao) itens else catalog.section(s)
            Grelha(daSeccao, catalog, requesters, onFocusItem, onOpenItem, onEditItem, cascata = cascata)
        }

    }
    }
}

@Composable
private fun Grelha(
    itens: List<MenuItem>,
    catalog: Catalog,
    requesters: Map<String, FocusRequester>,
    onFocusItem: (MenuItem) -> Unit,
    onOpenItem: (MenuItem) -> Unit,
    onEditItem: (MenuItem) -> Unit,
    cascata: Boolean = false,
) {
    // colunas = 0: a grelha escolhe quantas, para os artigos todos caberem no ecra.
    TileGrid(items = itens, columns = 0, modifier = Modifier.fillMaxSize()) { item, mod ->
        // Na primeira vez que o menu aparece os mosaicos entram em cascata, por
        // ordem. Nas vezes seguintes — ao voltar de um produto — ja estao la: a
        // foto que vem a voar precisa de encontrar o mosaico parado no sitio.
        Entrada(ordem = itens.indexOf(item).let { it % 4 + it / 4 }, modifier = mod, activa = cascata) {
        ProductTile(
            item = item,
            posterUrl = catalog.imageUrlOf(item),
            onClick = { onOpenItem(item) },
            onLongClick = { onEditItem(item) },
            onFocus = { onFocusItem(item) },
            focusRequester = requesters[item.id],
            modifier = Modifier.fillMaxSize(),
        )
        }
    }
}

/** Um separador do submenu: o nome e quantos artigos tem. Muda ao ganhar foco. */
@Composable
private fun Separador(titulo: String, total: Int, activo: Boolean, onFoco: () -> Unit, focusRequester: FocusRequester, cor: Color) {
    var focado by remember { mutableStateOf(false) }
    val fundo = when {
        focado -> cor
        activo -> cor.copy(alpha = 0.22f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { f -> focado = f.isFocused; if (f.isFocused) onFoco() }
            .focusable()
            .background(fundo)
            .padding(horizontal = Espaco.largo, vertical = Espaco.curto),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = if (focado) RoxoProfundo else if (activo) cor else CremeFraco,
            letterSpacing = 2.sp,
        )
        Text(
            text = " $total",
            style = MaterialTheme.typography.labelSmall,
            color = if (focado) RoxoProfundo else CremeFraco,
        )
    }
}

/** Titulo de seccao mais o conteudo, com o mesmo ritmo nas tres. */
@Composable
private fun Seccao(
    titulo: String,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    // Cada seccao assenta num painel proprio, um degrau acima do fundo. Com as
    // tres lado a lado e o mesmo ritmo de mosaico, sem isto lia-se como uma
    // grelha unica de oito colunas e as categorias desapareciam.
    Column(
        modifier
            .background(RoxoProfundo.copy(alpha = 0.35f))
            .padding(horizontal = Espaco.curto, vertical = Espaco.curto),
        verticalArrangement = Arrangement.spacedBy(Espaco.minimo),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(20.dp).height(3.dp).background(Amarelo))
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Amarelo,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = Espaco.curto),
            )
        }
        Box(Modifier.weight(1f)) { conteudo() }
    }
}

@Composable
private fun Cabecalho(
    temContinuar: Boolean,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onMontra: () -> Unit,
    onCoa: () -> Unit,
    onIdioma: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 58dp e a altura do bloco ao lado — lettering (36) mais o "DO BRASIL" e o
        // espaco entre os dois. Com os 38dp que aqui estavam, o logotipo ficava um
        // terco mais baixo do que ele e lia-se como desalinhado.
        //
        // Nao usar IntrinsicSize.Min para isto: o Image tem a altura nativa do
        // bitmap como intrinseca, e a linha inteira cresce ate ai — o logotipo
        // passa a ocupar o ecra e a grelha encolhe. Ja aconteceu.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo_caramelo),
                contentDescription = "Caramelo do Brasil",
                modifier = Modifier.height(58.dp),
            )
            Column(Modifier.padding(start = Espaco.medio)) {
                // O lettering original da marca em vez do nome composto em Poppins:
                // e desenho proprio e diz mais da marca do que qualquer tipo de letra.
                Image(
                    painter = painterResource(R.drawable.lettering_caramelo),
                    contentDescription = "Caramelo",
                    modifier = Modifier.height(36.dp),
                )
                Text(
                    text = "DO BRASIL",
                    style = MaterialTheme.typography.labelMedium,
                    color = Amarelo,
                    letterSpacing = 5.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Espaco.curto)) {
            // Arranca a passagem automatica ja, em vez de esperar pelo tempo de
            // inactividade. E o botao que se carrega ao abrir a loja.
            TvButton(text = tr("Montra", "Showcase"), onClick = onMontra, primary = true)
            if (temContinuar) {
                TvButton(text = tr("Continuar", "Continue"), onClick = onContinue, discreto = true)
            }
            // Cada botao e' a mesma caixa que o titulo do ecra que abre: carregar em
            // Definicoes faz o botao crescer ate ser o cabecalho das Definicoes, e
            // voltar faz o cabecalho encolher outra vez ate ao botao.
            Box(Modifier.heroBounds("ecra-analises")) {
                TvButton(text = tr("Análises", "Lab reports"), onClick = onCoa, discreto = true)
            }
            Box(Modifier.heroBounds("ecra-definicoes")) {
                TvButton(text = tr("Definições", "Settings"), onClick = onSettings, discreto = true)
            }
            // A outra lingua, escrita na propria lingua: quem nao le portugues reconhece "EN".
            TvButton(text = tr("EN", "PT"), onClick = onIdioma, discreto = true)
        }
    }
}
