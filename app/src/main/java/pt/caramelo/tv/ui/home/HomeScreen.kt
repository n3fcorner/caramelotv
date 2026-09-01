package pt.caramelo.tv.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import pt.caramelo.tv.ui.components.FundoDaMarca
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan

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
                if (evento.type == KeyEventType.KeyUp && evento.key == Key.Menu) {
                    onSettings()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
        verticalArrangement = Arrangement.spacedBy(Espaco.largo),
    ) {
        Cabecalho(
            temContinuar = catalog.lastWatched() != null,
            onContinue = onContinue,
            onSettings = onSettings,
        )

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Espaco.seccao),
        ) {
            Seccao(
                titulo = Section.INFUSAO.label,
                modifier = Modifier.weight(0.50f).fillMaxHeight(),
            ) {
                Grelha(infusao, catalog, requesters, onFocusItem, onOpenItem, onEditItem)
            }

            Column(
                modifier = Modifier.weight(0.50f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Espaco.largo),
            ) {
                Seccao(
                    titulo = Section.CBD.label,
                    modifier = Modifier.weight(0.66f).fillMaxWidth(),
                ) {
                    Grelha(cbd, catalog, requesters, onFocusItem, onOpenItem, onEditItem)
                }
                Seccao(
                    titulo = Section.HASH.label,
                    modifier = Modifier.weight(0.34f).fillMaxWidth(),
                ) {
                    Grelha(hash, catalog, requesters, onFocusItem, onOpenItem, onEditItem)
                }
            }
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
) {
    TileGrid(items = itens, columns = 3, modifier = Modifier.fillMaxSize()) { item, mod ->
        ProductTile(
            item = item,
            posterUrl = catalog.imageUrlOf(item),
            onClick = { onOpenItem(item) },
            onLongClick = { onEditItem(item) },
            onFocus = { onFocusItem(item) },
            focusRequester = requesters[item.id],
            modifier = mod,
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Espaco.curto)) {
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo_caramelo),
                contentDescription = "Caramelo do Brasil",
                modifier = Modifier.height(44.dp),
            )
            Column(Modifier.padding(start = Espaco.medio)) {
                // O lettering original da marca em vez do nome composto em Poppins:
                // e desenho proprio e diz mais da marca do que qualquer tipo de letra.
                Image(
                    painter = painterResource(R.drawable.lettering_caramelo),
                    contentDescription = "Caramelo",
                    modifier = Modifier.height(42.dp),
                )
                Text(
                    text = "DO BRASIL",
                    style = MaterialTheme.typography.labelMedium,
                    color = Amarelo,
                    letterSpacing = 5.sp,
                    modifier = Modifier.padding(start = Espaco.minimo),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Espaco.curto)) {
            if (temContinuar) {
                TvButton(text = "Continuar", onClick = onContinue, discreto = true)
            }
            TvButton(text = "Definições", onClick = onSettings, discreto = true)
        }
    }
}
