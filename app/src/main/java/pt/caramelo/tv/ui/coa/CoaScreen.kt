package pt.caramelo.tv.ui.coa

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.Analise
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.animation.heroBounds
import pt.caramelo.tv.ui.components.FundoDePapel
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.PapelTheme
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.TintaFraca
import pt.caramelo.tv.ui.theme.Verde
import pt.caramelo.tv.ui.theme.corDaSeccao

/**
 * Analises (4/10/2026, Francisco: "o separador Analises so tem algumas porque foi para o Nicoletti; faz umas tabelas no estilo
 * novo, seguindo o manual da marca, com as stats de cada strain, obtidas pelo COA"). Antes eram quatro boletins digitalizados,
 * escritos a mao no codigo. Agora sao os numeros do boletim publico de cada lote, que chegam do painel com o catalogo (os mesmos
 * da pagina /lote do site): uma flor nova com boletim aparece aqui sozinha.
 *
 *   +---------------------+-------------------------------------------+
 *   | as flores com       |  NOME                  [secao] Lote X     |
 *   | boletim, por        |  THC total  0,21 %  [====|.....] 0,3 %    |
 *   | seccao              |  tabela: CBD, CBG, D9-THC, THC total      |
 *   +---------------------+-------------------------------------------+
 *
 * Cima e baixo escolhem a flor; Voltar sai. Os numeros sao os do laboratorio, sem leitura nem comparacao (regra de copy).
 */
private const val LIMITE_THC = 0.3

@Composable
fun CoaScreen(
    itens: List<MenuItem>,
    onBack: () -> Unit,
) {
    val comBoletim = remember(itens) {
        val ordem = listOf(Section.CBD, Section.INFUSAO, Section.HASH)
        itens.filter { it.analise != null }.sortedWith(compareBy({ ordem.indexOf(it.section) }, { it.title }))
    }
    var escolhido by rememberSaveable { mutableIntStateOf(0) }
    val primeiro = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { primeiro.requestFocus() } }

    PapelTheme {
        Box(Modifier.fillMaxSize()) {
            FundoDePapel()
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical)
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyUp && e.key == Key.Back) { onBack(); true } else false
                    },
                verticalArrangement = Arrangement.spacedBy(Espaco.largo),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            tr("Análises", "Lab reports"),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Tinta,
                            modifier = Modifier.heroBounds("ecra-analises"),
                        )
                        Text(
                            tr("O boletim do laboratório de cada lote, o mesmo do QR do rótulo.", "Each batch's laboratory certificate, the same as the QR on the label."),
                            style = MaterialTheme.typography.labelMedium,
                            color = TintaFraca,
                        )
                    }
                    TvButton(text = tr("Voltar", "Back"), onClick = onBack)
                }

                if (comBoletim.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(tr("Ainda não há boletins publicados.", "No certificates published yet."), style = MaterialTheme.typography.titleMedium, color = TintaFraca)
                    }
                    return@Column
                }

                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Espaco.seccao)) {
                    Column(
                        // Folga em cima e em baixo: o cartao em foco cresce 2 % e a orla nao fica cortada no limite do scroll.
                        Modifier.weight(0.34f).fillMaxHeight().focusGroup().verticalScroll(rememberScrollState()).padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(Espaco.curto),
                    ) {
                        comBoletim.forEachIndexed { i, item ->
                            Linha(
                                item = item,
                                activa = i == escolhido,
                                onFoco = { escolhido = i },
                                modifier = if (i == 0) Modifier.focusRequester(primeiro) else Modifier,
                            )
                        }
                    }
                    AnimatedContent(
                        targetState = comBoletim.getOrNull(escolhido) ?: comBoletim.first(),
                        transitionSpec = { fadeIn(tween(200, delayMillis = 60)).togetherWith(fadeOut(tween(90))) },
                        contentKey = { it.id },
                        label = "boletim",
                        modifier = Modifier.weight(0.66f).fillMaxHeight(),
                    ) { item -> Ficha(item, item.analise!!) }
                }
            }
        }
    }
}

/** Uma flor da lista: o traco da cor da seccao, o nome e o lote. O foco escolhe-a. */
@Composable
private fun Linha(item: MenuItem, activa: Boolean, onFoco: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(8.dp)
    Surface(
        onClick = {},
        modifier = modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFoco() },
        shape = ClickableSurfaceDefaults.shape(shape = forma),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (activa) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
            contentColor = Tinta,
            focusedContainerColor = Amarelo,
            focusedContentColor = Tinta,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(border = Border(BorderStroke(2.dp, Tinta), shape = forma), focusedBorder = Border(BorderStroke(3.dp, Tinta), shape = forma)),
    ) {
        Row(Modifier.padding(horizontal = Espaco.medio, vertical = Espaco.curto), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(30.dp).background(corDaSeccao(item.section), RoundedCornerShape(2.dp)))
            Column(Modifier.padding(start = Espaco.medio).weight(1f)) {
                Text(item.displayTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(termo(item.section.label), item.analise?.lote?.takeIf { it.isNotBlank() }?.let { "${tr("Lote", "Batch")} $it" }).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TintaFraca,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A ficha de analise de uma flor: o cabecalho, o THC contra o limite, e a tabela dos canabinoides medidos. */
@Composable
private fun Ficha(item: MenuItem, a: Analise) {
    val cor = corDaSeccao(item.section)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(2.dp, Tinta, RoundedCornerShape(8.dp))
            .padding(horizontal = Espaco.seccao, vertical = Espaco.largo),
        verticalArrangement = Arrangement.spacedBy(Espaco.medio),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.displayTitle, style = MaterialTheme.typography.displaySmall, color = Tinta, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(
                termo(item.section.label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                letterSpacing = 2.sp,
                modifier = Modifier.background(cor, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            listOfNotNull(
                a.lote.takeIf { it.isNotBlank() }?.let { "${tr("Lote", "Batch")} $it" },
                a.laboratorio.takeIf { it.isNotBlank() },
                a.numero.takeIf { it.isNotBlank() }?.let { tr("boletim n.º ", "certificate no. ") + it },
                a.data.takeIf { it.isNotBlank() },
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.labelMedium,
            color = TintaFraca,
            maxLines = 2,
        )

        a.thc?.let { ReguaThc(it) }

        Column(Modifier.padding(top = Espaco.curto)) {
            Cabeca()
            val linhas = listOfNotNull(
                a.thc?.let { tr("THC total", "Total THC") to it },
                a.d9thc?.let { "Δ9-THC" to it },
                a.cbd?.let { tr("CBD total", "Total CBD") to it },
                a.cbg?.let { tr("CBG total", "Total CBG") to it },
            )
            linhas.forEachIndexed { i, (nome, v) -> LinhaTabela(nome, v, i % 2 == 0) }
            if (linhas.isEmpty()) Text(tr("O boletim não traz valores transcritos.", "The certificate has no transcribed values."), color = TintaFraca, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.weight(1f))
        if (a.metodo.isNotBlank()) {
            Text("${tr("Método", "Method")}: ${a.metodo}", style = MaterialTheme.typography.labelSmall, color = TintaFraca, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** THC total contra o limite legal de 0,3 %: a barra enche-se ate ao valor e o traco marca o limite. */
@Composable
private fun ReguaThc(thc: Double) {
    val dentro = thc <= LIMITE_THC
    val fracao by animateFloatAsState((thc / (LIMITE_THC * 1.25)).toFloat().coerceIn(0f, 1f), tween(520), label = "thc")
    Column(Modifier.fillMaxWidth().padding(top = Espaco.curto), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(tr("THC total", "Total THC"), style = MaterialTheme.typography.labelMedium, color = TintaFraca, modifier = Modifier.weight(1f))
            Text(pct(thc), style = MaterialTheme.typography.headlineSmall, color = Tinta, fontWeight = FontWeight.Bold)
            Text(
                if (dentro) tr("  conforme · limite 0,3 %", "  compliant · limit 0.3 %") else tr("  acima do limite", "  above the limit"),
                style = MaterialTheme.typography.labelMedium,
                color = if (dentro) Verde else Color(0xFFE13831),
            )
        }
        Box(
            Modifier.fillMaxWidth().height(14.dp).drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Tinta.copy(alpha = 0.08f), cornerRadius = r)
                drawRoundRect(if (dentro) Verde else Color(0xFFE13831), size = Size(size.width * fracao, size.height), cornerRadius = r)
                val x = size.width / 1.25f
                drawLine(Tinta, Offset(x, -6f), Offset(x, size.height + 6f), strokeWidth = 3f)
            },
        )
    }
}

@Composable
private fun Cabeca() {
    Row(Modifier.fillMaxWidth().padding(horizontal = Espaco.medio, vertical = 6.dp)) {
        Text(tr("CANABINOIDE", "CANNABINOID"), style = MaterialTheme.typography.labelSmall, color = TintaFraca, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
        Text(tr("% EM MASSA", "% BY WEIGHT"), style = MaterialTheme.typography.labelSmall, color = TintaFraca, letterSpacing = 2.sp)
    }
}

@Composable
private fun LinhaTabela(nome: String, v: Double, par: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (par) Tinta.copy(alpha = 0.045f) else Color.Transparent, RoundedCornerShape(6.dp))
            .padding(horizontal = Espaco.medio, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(nome, style = MaterialTheme.typography.titleSmall, color = Tinta, modifier = Modifier.weight(1f))
        Text(pct(v), style = MaterialTheme.typography.titleSmall, color = Tinta, fontWeight = FontWeight.Bold)
    }
}

/** 0.28 -> "0,28 %": virgula decimal, ate duas casas, sem zeros a mais. */
private fun pct(v: Double): String =
    String.format(java.util.Locale.ROOT, "%.2f", v).trimEnd('0').trimEnd('.').replace('.', ',').let { if (it.isEmpty()) "0" else it } + " %"
