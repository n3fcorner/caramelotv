@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class) // focusProperties { enter }: o separador aberto recebe o foco ao subir da grelha

package pt.caramelo.tv.ui.home

import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedContent
import pt.caramelo.tv.ui.theme.Laranja
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import pt.caramelo.tv.ui.components.FundoDePapel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.PapelTheme
import pt.caramelo.tv.ui.theme.TintaFraca
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
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.FaixaDaMarca
import pt.caramelo.tv.ui.theme.corDaSeccao
import androidx.compose.foundation.layout.offset

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

/** O par de cores de cada pagina: as duas barras do cabecalho. A primeira e' tambem a do separador. */
private fun parDe(s: Section): Pair<Color, Color> = corDaSeccao(s) to Amarelo

private fun corDe(s: Section): Color = parDe(s).first

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
    // Pela lista de ids e nao pelo tamanho: uma sincronizacao que troca um artigo por outro
    // mantem o tamanho e deixava o novo sem FocusRequester (o foco saltava para o primeiro).
    val requesters = remember(catalog.items.map { it.id }) {
        catalog.items.associate { it.id to FocusRequester() }
    }
    val separadores = remember { ORDEM_SECCOES.associateWith { FocusRequester() } }

    // Se ninguem tem o foco, pede-se para o produto certo; e outra vez quando a lista muda. Na primeira instalacao o ecra nasce com
    // os produtos de fabrica e, segundos depois, chegam os do painel, que trocam os mosaicos: o foco pedido ao primeiro ficava num
    // mosaico que ja nao existia, e a primeira tecla caia no separador (4/10/2026, visto na TV da loja).
    var temFoco by remember { mutableStateOf(false) }
    LaunchedEffect(itens.map { it.id }) {
        // Esperar pelo primeiro frame: o FocusRequester so responde depois do no colocado.
        withFrameNanos { }
        if (temFoco) return@LaunchedEffect
        val alvo = focusedId?.takeIf { id -> itens.any { it.id == id } }?.let { requesters[it] }
            ?: requesters[itens.firstOrNull()?.id] ?: separadores[seccao]
        runCatching { alvo?.requestFocus() }
    }

    PapelTheme {
    Box(Modifier.fillMaxSize()) {
    FundoDePapel()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { temFoco = it.hasFocus }
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
            .padding(bottom = Overscan.vertical),
        verticalArrangement = Arrangement.spacedBy(Espaco.medio),
    ) {
        // Depois da primeira cascata, marca-se: nas vezes seguintes o menu ja
        // esta composto quando a foto chega do leitor.
        LaunchedEffect(Unit) { onMostrado() }

        // A faixa da marca (2/10/2026): azul escuro a fundir para o roxo, de ponta a ponta do ecra e com a altura
        // da logomarca; os botoes por cima dela no tema escuro (bege em repouso, amarelo com o foco).
        CarameloTheme {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(FaixaDaMarca)
                    // Para baixo, de qualquer botao, vai ao separador aberto (4/10/2026): de Definicoes caia-se no
                    // mosaico que estivesse por baixo e saltava-se a linha dos separadores.
                    .focusProperties { down = separadores.getValue(seccao) }
                    .padding(start = Overscan.horizontal, end = Overscan.horizontal, top = Overscan.vertical, bottom = Espaco.largo),
            ) {
                Cabecalho(
                    temContinuar = catalog.lastWatched() != null,
                    onContinue = onContinue,
                    onSettings = onSettings,
                    onMontra = onMontra,
                    onCoa = onCoa,
                    onIdioma = onIdioma,
                )
            }
        }

        // Duas barras por baixo do cabecalho, com o par de cores da pagina aberta (Francisco, 29/9/2026:
        // "duas barras por baixo do header, de cores diferentes de pagina para pagina"). A cor de cima e'
        // a do separador; mudar de separador funde as cores, sem saltos.
        val (corCima, corBaixo) = parDe(seccao)
        val cima by animateColorAsState(corCima, tween(260), label = "barra-cima")
        val baixo by animateColorAsState(corBaixo, tween(260), label = "barra-baixo")
        // As cores leem-se so no desenho (drawBehind): a animacao nao recompoe o ecra inteiro a cada fotograma.
        Column(Modifier.fillMaxWidth().offset(y = -Espaco.medio), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(Modifier.fillMaxWidth().height(8.dp).drawBehind { drawRect(cima) })
            Box(Modifier.fillMaxWidth().height(4.dp).drawBehind { drawRect(baixo) })
        }

        // Os tres submenus a esquerda e a legenda dos pontos a direita, na mesma linha:
        // a grelha fica com a altura toda que sobra.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Overscan.horizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ao subir da grelha o foco entra no separador aberto, e nao no que calhar mais perto
            // (que mudava de secao sem querer).
            Row(Modifier.focusProperties { enter = { separadores.getValue(seccao) } }.focusGroup(), horizontalArrangement = Arrangement.spacedBy(Espaco.curto)) {
                ORDEM_SECCOES.forEach { s ->
                    Separador(
                        titulo = termo(s.label),
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
            // Sem margem lateral: a fila corre ate a borda do ecra e a margem de seguranca vai por dentro dela.
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { s ->
            val daSeccao = if (s == seccao) itens else catalog.section(s)
            Grelha(
                daSeccao, catalog, requesters, onFocusItem, onOpenItem, onEditItem, cascata = cascata, cor = corDaSeccao(s),
                separador = separadores.getValue(s),
                ultimo = focusedId?.takeIf { id -> daSeccao.any { it.id == id } }?.let { requesters[it] },
            )
        }

    }
    }
    }
}

/**
 * A fila lateral de uma seccao (1/10/2026, Francisco: "quero que a lista de produtos seja navegavel lateralmente"):
 * os mosaicos todos do mesmo tamanho, grandes, numa linha que o D-pad percorre. Row e nao LazyRow: todos compostos,
 * por isso o FocusRequester de cada um existe sempre e o foco volta ao sitio certo ao sair de uma ficha.
 *
 * As bordas (4/10/2026, Francisco: "pensa que ha limites, e ha coisas para alem das bordas"):
 *  - a fila tem a largura do ecra e a margem de seguranca vai por dentro (padding depois do scroll): os mosaicos
 *    deslizam ate a borda fisica em vez de serem cortados numa linha invisivel a 58 dp;
 *  - o mosaico cresce 9 % com o foco, por isso a altura deixa essa folga e o mosaico em foco nunca e' cortado;
 *  - o foco nunca assenta encostado a borda ([FocoComFolga]): fica sempre um bocado do vizinho a vista, que e' o que
 *    diz que ha mais para la;
 *  - a regua por baixo diz onde se esta ("04 / 15").
 * Para cima vai sempre ao separador aberto, de qualquer mosaico (antes so do primeiro: do Uranium nao se saia da
 * fila); ao descer do separador volta-se ao ultimo mosaico visto.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Grelha(
    itens: List<MenuItem>,
    catalog: Catalog,
    requesters: Map<String, FocusRequester>,
    onFocusItem: (MenuItem) -> Unit,
    onOpenItem: (MenuItem) -> Unit,
    onEditItem: (MenuItem) -> Unit,
    cascata: Boolean = false,
    cor: Color = Amarelo,
    separador: FocusRequester,
    ultimo: FocusRequester?,
) {
    var focado by remember { mutableIntStateOf(-1) }
    val densidade = LocalDensity.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Espaco.curto)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.CenterStart) {
            // A folga da escala do foco em cima e em baixo, mais um pouco para a orla amarela.
            val alto = maxHeight / Movimento.ESCALA_FOCO - Espaco.curto
            val largo = alto * 0.72f
            // Quanto do vizinho fica sempre a vista: pouco mais de um terco de mosaico, depois da margem de seguranca.
            val folga = Overscan.horizontal + largo * 0.38f
            val spec = remember(densidade, folga) { with(densidade) { FocoComFolga(folga.toPx()) } }
            CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Ao descer do separador entra-se no ultimo mosaico visto, nao no que calhar por baixo.
                        .focusProperties { enter = { ultimo ?: FocusRequester.Default } }
                        .focusGroup()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Overscan.horizontal),
                    horizontalArrangement = Arrangement.spacedBy(Espaco.largo),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    itens.forEachIndexed { i, item ->
                        // Na primeira vez que o menu aparece os mosaicos entram em cascata; nas seguintes ja estao parados,
                        // para a foto que volta da ficha encontrar o seu mosaico no sitio.
                        Entrada(ordem = i, modifier = Modifier.width(largo).height(alto), activa = cascata) {
                            ProductTile(
                                item = item,
                                posterUrl = catalog.imageUrlOf(item),
                                onClick = { onOpenItem(item) },
                                onLongClick = { onEditItem(item) },
                                onFocus = { focado = i; onFocusItem(item) },
                                focusRequester = requesters[item.id],
                                cor = cor,
                                modifier = Modifier.fillMaxSize().focusProperties { up = separador },
                            )
                        }
                    }
                }
            }
        }
        Posicao(focado, itens.size, cor)
    }
}

/**
 * Onde assenta o mosaico que recebe o foco: nunca a menos de [folga] px de uma borda. O horizontalScroll trazia-o so
 * ate ficar inteiro, encostado ao fim do ecra, e o vizinho desaparecia; assim ha sempre um pedaco dele a vista.
 * No inicio e no fim da fila o scroll para nos limites, e o primeiro e o ultimo assentam na margem de seguranca.
 */
@OptIn(ExperimentalFoundationApi::class)
private class FocoComFolga(private val folga: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val fim = offset + size
        val antes = folga.coerceAtMost((containerSize - size) / 2f)
        return when {
            offset < antes -> offset - antes
            fim > containerSize - antes -> fim - (containerSize - antes)
            else -> 0f
        }
    }
}

/** A regua da fila: a cor da seccao ate ao mosaico em foco, e "04 / 15" a direita. Le-se, nao se navega. */
@Composable
private fun Posicao(i: Int, n: Int, cor: Color) {
    val fracao by animateFloatAsState(if (n > 0 && i >= 0) (i + 1f) / n else 0f, tween(220), label = "regua")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Overscan.horizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A fracao le-se so no desenho: andar na fila nao recompoe a regua.
        Box(
            Modifier.weight(1f).height(3.dp).drawBehind {
                drawRect(Tinta.copy(alpha = 0.10f))
                drawRect(cor, size = size.copy(width = size.width * fracao))
            },
        )
        Text(
            text = if (i >= 0) "%02d / %02d".format(i + 1, n) else "$n " + tr("artigos", "items"),
            style = MaterialTheme.typography.labelSmall,
            color = TintaFraca,
            letterSpacing = 2.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = Espaco.medio),
        )
    }
}

/** Um separador do submenu: so o nome, sem contagens (1/10/2026). Muda ao ganhar foco. */
@Composable
private fun Separador(titulo: String, activo: Boolean, onFoco: () -> Unit, focusRequester: FocusRequester, cor: Color) {
    var focado by remember { mutableStateOf(false) }
    // Regra 3: o foco e' amarelo; regra 2: a seccao aberta leva a sua cor cheia; as outras ficam com um filete da cor.
    val fundo by animateColorAsState(
        when {
            focado -> Amarelo
            activo -> cor
            else -> cor.copy(alpha = 0.12f)
        },
        tween(160), label = "separador",
    )
    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { f -> focado = f.isFocused; if (f.isFocused) onFoco() }
            .focusable()
            .background(fundo, RoundedCornerShape(8.dp))
            .border(2.dp, Tinta, RoundedCornerShape(8.dp))
            .padding(horizontal = Espaco.largo, vertical = Espaco.curto),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = if (focado) Tinta else if (activo) Creme else Tinta,
            letterSpacing = 2.sp,
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
            // O nome da marca como o manual o compõe (30/9/2026): CARAMELO em Carbona ExtraBold bege sobre DO BRASIL
            // em amarelo espaçado, recortado da logomarca oficial (uma imagem, não a fonte embutida; a licença da
            // Carbona que existe é a web). Substitui o lettering em grafíti e o "DO BRASIL" em Poppins.
            Image(
                painter = painterResource(R.drawable.lettering_caramelo),
                contentDescription = "Caramelo do Brasil",
                modifier = Modifier.padding(start = Espaco.medio).height(46.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Espaco.curto)) {
            // Arranca a passagem automatica ja, em vez de esperar pelo tempo de
            // inactividade. E o botao que se carrega ao abrir a loja.
            TvButton(text = tr("Montra", "Showcase"), onClick = onMontra, primary = true)
            // O "Continuar" saiu (4/10/2026): abrir um produto ja retoma o video onde ficou, e o botao so baralhava o cabecalho.
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
