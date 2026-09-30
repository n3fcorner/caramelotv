package pt.caramelo.tv.ui.settings

import pt.caramelo.tv.ui.i18n.Idioma
import pt.caramelo.tv.ui.i18n.LocalIdioma
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.ItemState
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.PanelSettings
import pt.caramelo.tv.ui.animation.heroBounds
import pt.caramelo.tv.ui.components.SectionHeader
import pt.caramelo.tv.ui.components.SelectableRow
import pt.caramelo.tv.ui.components.ToggleRow
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.theme.Overscan

/**
 * Definicoes: lista dos 15 slots a esquerda, opcoes globais a direita.
 * Escolher um slot abre o formulario de edicao ([EditItemScreen]).
 */
@Composable
fun SettingsScreen(
    catalog: Catalog,
    onEditItem: (MenuItem) -> Unit,
    onSettingsChange: (PanelSettings) -> Unit,
    onClearProgress: () -> Unit,
    onResetCatalog: () -> Unit,
    onBack: () -> Unit,
) {
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstRow.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                tr("Definições", "Settings"),
                style = MaterialTheme.typography.headlineMedium,
                // A mesma caixa que o botao do menu: e' ele que cresce ate aqui.
                modifier = Modifier.heroBounds("ecra-definicoes"),
            )
            TvButton(text = tr("Voltar", "Back"), onClick = onBack)
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.55f).fillMaxHeight().focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader(tr("Produtos", "Products"))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(catalog.items, key = { it.id }) { item ->
                        SelectableRow(
                            label = "${termo(item.section.label)} · ${item.displayTitle}",
                            value = when (item.state) {
                                ItemState.PRONTO -> tr("com vídeo", "with video")
                                ItemState.EM_BREVE -> tr("em breve", "coming soon")
                                ItemState.LIVRE -> tr("livre", "empty")
                            },
                            onClick = { onEditItem(item) },
                            focusRequester = if (item.id == catalog.items.firstOrNull()?.id) firstRow else null,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.45f).focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader(tr("Comportamento", "Behaviour"))
                SelectableRow(
                    label = tr("Língua", "Language"),
                    value = tr("Português", "English"),
                    onClick = { onSettingsChange(catalog.settings.copy(idioma = if (catalog.settings.idioma == "en") "pt" else "en")) },
                )
                ToggleRow(
                    label = tr("Reprodução automática", "Autoplay"),
                    checked = catalog.settings.autoplay,
                    onToggle = { onSettingsChange(catalog.settings.copy(autoplay = it)) },
                )
                ToggleRow(
                    label = tr("Retomar vídeos", "Resume videos"),
                    checked = catalog.settings.resumePlayback,
                    onToggle = { onSettingsChange(catalog.settings.copy(resumePlayback = it)) },
                )
                SelectableRow(
                    label = tr("Montra automática", "Automatic showcase"),
                    value = rotuloDaEspera(catalog.settings.attractSeconds, LocalIdioma.current),
                    onClick = {
                        onSettingsChange(
                            catalog.settings.copy(
                                attractSeconds = proximaEspera(catalog.settings.attractSeconds),
                            ),
                        )
                    },
                )
                ToggleRow(
                    label = tr("Repetir o clipe", "Loop the clip"),
                    checked = catalog.settings.loopPlayback,
                    onToggle = { onSettingsChange(catalog.settings.copy(loopPlayback = it)) },
                )

                SectionHeader(text = tr("Servidor de media", "Media server"), modifier = Modifier.padding(top = 16.dp))
                Text(
                    text = catalog.settings.mediaBaseUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = tr("Prefixo dos caminhos relativos dos itens. Muda-se por ADB: ", "Prefix for the items' relative paths. Change it over ADB: ") +
                        "caramelotv://base?url=http://...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionHeader(text = tr("Manutenção", "Maintenance"), modifier = Modifier.padding(top = 16.dp))
                TvButton(
                    text = tr("Limpar progresso", "Clear progress"),
                    onClick = onClearProgress,
                    modifier = Modifier.fillMaxWidth(),
                )
                TvButton(
                    text = tr("Repor catálogo", "Reset catalogue"),
                    onClick = onResetCatalog,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = tr("Repor apaga todas as edições e volta aos produtos de origem.", "Reset removes every edit and goes back to the original products."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Segundos de comando parado ate a montra arrancar sozinha. Zero desliga. */
private val ESPERAS = listOf(0, 30, 60, 120, 300)

private fun proximaEspera(actual: Int): Int {
    val i = ESPERAS.indexOf(actual)
    return ESPERAS[if (i < 0) 0 else (i + 1) % ESPERAS.size]
}

private fun rotuloDaEspera(segundos: Int, idioma: Idioma): String {
    val en = idioma == Idioma.EN
    return when {
        segundos <= 0 -> if (en) "Off" else "Desligada"
        segundos < 60 -> if (en) "$segundos s idle" else "$segundos s parado"
        else -> if (en) "${segundos / 60} min idle" else "${segundos / 60} min parado"
    }
}
