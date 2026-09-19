package pt.caramelo.tv.ui.home

import androidx.compose.animation.AnimatedContent
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

/**
 * Ecra inicial: as duas linhas comerciais mais o hash.
 *
 *   +--------------------------+-----------------+
 *   |  INFUSÃO                 |  CBD            |
 *   |  3x3                     |  3x2            |
 *   |                          +-----------------+
 *   |                          |  HASH  3x1      |
 *   +--------------------------+-----------------+
 *
 * Nada de painel de definicoes sempre a vista: a ficha do produto vive no submenu, e
 * as definicoes chegam-se pela roda dentada do cabecalho ou pela tecla MENU.
 */
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
    /** Verdadeiro na primeira vez que o menu aparece: os mosaicos entram em cascata. */
    cascata: Boolean = false,
    onMostrado: () -> Unit = {},
) {
    val infusao = catalog.section(Section.INFUSAO)
    val cbd = catalog.section(Section.CBD)
    val hash = catalog.section(Section.HASH)

    // Um FocusRequester por mosaico: e o que devolve o foco ao sitio certo ao voltar
    // dos detalhes ou do leitor.
    val requesters = remember(catalog.items.size) {
        catalog.items.associate { it.id to FocusRequester() }
    }

    LaunchedEffect(Unit) {
        // Esperar pelo primeiro frame: o FocusRequester so responde depois do no colocado.
        withFrameNanos { }
        val alvo = focusedId?.let { requesters[it] } ?: requesters[infusao.firstOrNull()?.id]
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
        )

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Espaco.largo),
        ) {
            // Tres seccoes lado a lado, todas a vista. A infusao (12) em 4x3, o
            // CBD e o hash (6) em 2x3: as tres com tres linhas, o mesmo mosaico e as
            // filas alinhadas. Os pesos saem da largura que cada uma precisa.
            Seccao(
                titulo = Section.INFUSAO.label,
                modifier = Modifier.weight(0.505f).fillMaxHeight(),
            ) {
                Grelha(infusao, catalog, requesters, onFocusItem, onOpenItem, onEditItem, colunas = 4, cascata = cascata)
            }
            Seccao(
                titulo = Section.CBD.label,
                modifier = Modifier.weight(0.2475f).fillMaxHeight(),
            ) {
                Grelha(cbd, catalog, requesters, onFocusItem, onOpenItem, onEditItem, colunas = 2, cascata = cascata)
            }
            Seccao(
                titulo = Section.HASH.label,
                modifier = Modifier.weight(0.2475f).fillMaxHeight(),
            ) {
                Grelha(hash, catalog, requesters, onFocusItem, onOpenItem, onEditItem, colunas = 2, cascata = cascata)
            }
        }

        // Os pontos de cultivo dos mosaicos, explicados no fundo da grelha.
        LegendaDeCultivo()
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
    colunas: Int,
    cascata: Boolean = false,
) {
    TileGrid(items = itens, columns = colunas, modifier = Modifier.fillMaxSize()) { item, mod ->
        // Na primeira vez que o menu aparece os mosaicos entram em cascata, por
        // ordem. Nas vezes seguintes — ao voltar de um produto — ja estao la: a
        // foto que vem a voar precisa de encontrar o mosaico parado no sitio.
        Entrada(ordem = itens.indexOf(item) % colunas + itens.indexOf(item) / colunas, modifier = mod, activa = cascata) {
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
            TvButton(text = "Montra", onClick = onMontra, primary = true)
            if (temContinuar) {
                TvButton(text = "Continuar", onClick = onContinue, discreto = true)
            }
            // Cada botao e' a mesma caixa que o titulo do ecra que abre: carregar em
            // Definicoes faz o botao crescer ate ser o cabecalho das Definicoes, e
            // voltar faz o cabecalho encolher outra vez ate ao botao.
            Box(Modifier.heroBounds("ecra-analises")) {
                TvButton(text = "Análises", onClick = onCoa, discreto = true)
            }
            Box(Modifier.heroBounds("ecra-definicoes")) {
                TvButton(text = "Definições", onClick = onSettings, discreto = true)
            }
        }
    }
}
