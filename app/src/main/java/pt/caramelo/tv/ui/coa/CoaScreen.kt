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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.Analise
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.caramelo.Caramelo
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.inicio.FundoDaPagina
import pt.caramelo.tv.ui.inicio.Grelha
import pt.caramelo.tv.ui.inicio.GrelhaPagina
import pt.caramelo.tv.ui.inicio.LuzDasFlores
import pt.caramelo.tv.ui.inicio.TopoDaPagina
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Verde
import pt.caramelo.tv.ui.theme.VerdeClaro
import pt.caramelo.tv.ui.theme.Vermelho
import pt.caramelo.tv.ui.theme.corDaSeccao

/**
 * Analises (4/10/2026, Francisco: "o separador Analises so tem algumas porque foi para o Nicoletti; faz umas tabelas no estilo
 * novo, seguindo o manual da marca, com as stats de cada strain, obtidas pelo COA"). Antes eram quatro boletins digitalizados,
 * escritos a mao no codigo. Agora sao os numeros do boletim publico de cada lote, que chegam do painel com o catalogo (os mesmos
 * da pagina /lote do site): uma flor nova com boletim aparece aqui sozinha.
 *
 *   [logo]                                        Analises   ▲▼ escolher
 *   as flores com    +--------------------------------+
 *   boletim, por     | [secao] BOLETIM · LOTE X       |   o Caramelo
 *   seccao           | NOME                           |   com a lupa
 *                    | THC total 0,21 % [===|..] 0,3 %|
 *                    | tabela: THC, D9-THC, CBD, CBG  |
 *                    +--------------------------------+
 *
 * Cima e baixo escolhem a flor; Voltar sai. Os numeros sao os do laboratorio, sem leitura nem comparacao (regra de copy).
 */
private const val LIMITE_THC = 0.3

/** As medidas da pagina, na grelha do ecra inicial: a lista na primeira coluna, o boletim no lugar do palco e o Caramelo com a lupa na terceira. */
private val LISTA_LARGURA = 250.dp
private val BOLETIM_X = 326.dp
private val BOLETIM_LARGURA = 410.dp

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

    // No mundo do ecra inicial (22/11/2026, "preciso que coloques tudo consistente"): era o papel bege. O azul das flores com
    // a luz sobre o boletim, a lista na primeira coluna, o boletim numa caixa do traco da ficha tecnica e, na terceira
    // coluna, o Caramelo curador a ler com a lupa (a pose que ja existia para as analises).
    CarameloTheme {
        Box(
            Modifier.fillMaxSize().background(Tinta)
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.Back) { onBack(); true } else false },
        ) {
            // a luz parada: a poeira via-se por tras da caixa do boletim, que e' meio transparente, e parecia sujidade nos numeros
            FundoDaPagina(LuzDasFlores, eixo = BOLETIM_X + BOLETIM_LARGURA / 2, vivo = false)

            if (comBoletim.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("Ainda não há boletins publicados.", "No certificates published yet."), color = Papel.copy(alpha = 0.7f), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona)
                }
            } else {
                Column(
                    // Folga em cima e em baixo: a linha em foco cresce 2 % e a orla nao fica cortada no limite do scroll.
                    Modifier.offset(Grelha.fichaX - 4.dp, Grelha.fichaY - 6.dp).width(LISTA_LARGURA + 8.dp).height(540.dp - Grelha.fichaY - GrelhaPagina.margemDeBaixo + 12.dp)
                        .focusGroup().verticalScroll(rememberScrollState()).padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
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
                    modifier = Modifier.offset(BOLETIM_X, Grelha.fichaY).width(BOLETIM_LARGURA).height(540.dp - Grelha.fichaY - GrelhaPagina.margemDeBaixo),
                ) { item -> Boletim(item, item.analise!!) }
            }

            Caramelo(Pose.Lupa, Modifier.offset(Grelha.carameloX, Grelha.carameloY).size(Grelha.carameloLargura, Grelha.carameloAltura))
            Text(
                tr("O boletim do laboratório de cada lote, o mesmo do QR do rótulo.", "Each batch's laboratory certificate, the same as the QR on the label."),
                color = Papel.copy(alpha = 0.6f), fontSize = 9.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, textAlign = TextAlign.Center,
                modifier = Modifier.offset(Grelha.carameloX, Grelha.carameloY + Grelha.carameloAltura + 14.dp).width(Grelha.carameloLargura),
            )
            TopoDaPagina(
                listOf("▲ ▼" to tr("escolher", "choose"), "↩" to tr("voltar", "back")),
                titulo = tr("Análises", "Lab reports"),
            )
        }
    }
}

/** Uma flor da lista: o traco da cor da seccao, o nome e o lote. O foco escolhe-a. */
@Composable
private fun Linha(item: MenuItem, activa: Boolean, onFoco: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(8.dp)
    var focada by remember { mutableStateOf(false) }
    Surface(
        onClick = {},
        modifier = modifier.fillMaxWidth().onFocusChanged { focada = it.isFocused; if (it.isFocused) onFoco() },
        shape = ClickableSurfaceDefaults.shape(shape = forma),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Papel.copy(alpha = if (activa) 0.14f else 0.06f),
            contentColor = Papel,
            focusedContainerColor = Amarelo,
            focusedContentColor = Tinta,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(border = Border(BorderStroke(1.5.dp, Papel.copy(alpha = 0.18f)), shape = forma), focusedBorder = Border(BorderStroke(2.dp, Papel), shape = forma)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(26.dp).background(corDaSeccao(item.section), RoundedCornerShape(2.dp)))
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(item.displayTitle, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Carbona, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(termo(item.section.label), item.analise?.lote?.takeIf { it.isNotBlank() }?.let { "${tr("Lote", "Batch")} $it" }).joinToString(" · ").uppercase(),
                    color = (if (focada) Tinta else Papel).copy(alpha = 0.65f), fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona, maxLines = 1,
                )
            }
        }
    }
}

/** O boletim de uma flor: o chip e o nome como na ficha do ecra inicial, o THC contra o limite, e a tabela dos canabinoides medidos. */
@Composable
private fun Boletim(item: MenuItem, a: Analise) {
    val cor = corDaSeccao(item.section)
    val caixa = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxSize()
            .background(Tinta.copy(alpha = 0.55f), caixa)
            .border(1.5.dp, Papel.copy(alpha = 0.22f), caixa)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val chip = RoundedCornerShape(4.dp)
            Text(
                when (item.section) { Section.CBD -> "CBD"; Section.INFUSAO -> tr("INFUSÃO", "INFUSED"); Section.HASH -> "HASH" },
                color = if (item.section == Section.HASH) Tinta else Papel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp, fontFamily = Carbona, maxLines = 1,
                modifier = Modifier.background(cor, chip).border(1.5.dp, Tinta, chip).padding(horizontal = 7.dp, vertical = 3.dp),
            )
            Text(
                listOfNotNull(tr("BOLETIM", "CERTIFICATE"), a.lote.takeIf { it.isNotBlank() }?.let { "${tr("LOTE", "BATCH")} $it" }).joinToString(" · "),
                color = Papel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp, fontFamily = Carbona, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Text(item.displayTitle, color = Papel, fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp, fontFamily = Carbona, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(
            listOfNotNull(
                a.laboratorio.takeIf { it.isNotBlank() },
                a.numero.takeIf { it.isNotBlank() }?.let { tr("boletim n.º ", "certificate no. ") + it },
                a.data.takeIf { it.isNotBlank() },
            ).joinToString("  ·  "),
            color = Papel.copy(alpha = 0.65f), fontSize = 10.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )

        a.thc?.let { ReguaThc(it) }

        Column(Modifier.padding(top = 16.dp)) {
            Cabeca()
            val linhas = listOfNotNull(
                a.thc?.let { tr("THC total", "Total THC") to it },
                a.d9thc?.let { "Δ9-THC" to it },
                a.cbd?.let { tr("CBD total", "Total CBD") to it },
                a.cbg?.let { tr("CBG total", "Total CBG") to it },
            )
            linhas.forEachIndexed { i, (nome, v) -> LinhaTabela(nome, v, i % 2 == 0) }
            if (linhas.isEmpty()) Text(tr("O boletim não traz valores transcritos.", "The certificate has no transcribed values."), color = Papel.copy(alpha = 0.65f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona)
        }
        Spacer(Modifier.weight(1f))
        if (a.metodo.isNotBlank()) {
            Text("${tr("MÉTODO", "METHOD")} · ${a.metodo}", color = Papel.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** THC total contra o limite legal de 0,3 %: a barra enche-se ate ao valor e o traco marca o limite. No traco das barras da ficha. */
@Composable
private fun ReguaThc(thc: Double) {
    val dentro = thc <= LIMITE_THC
    val fracao by animateFloatAsState((thc / (LIMITE_THC * 1.25)).toFloat().coerceIn(0f, 1f), tween(520), label = "thc")
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(tr("THC TOTAL", "TOTAL THC"), color = Papel, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
            Text(pct(thc), color = Papel, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = Carbona)
            Text(
                (if (dentro) tr("conforme · limite 0,3 %", "compliant · limit 0.3 %") else tr("acima do limite", "above the limit")).uppercase(),
                color = if (dentro) VerdeClaro else Vermelho, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )
        }
        Box(
            Modifier.padding(top = 5.dp).fillMaxWidth().height(9.dp).drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(1.5.dp.toPx(), 1.5.dp.toPx()), size, r)
                drawRoundRect(Color(0xFF12365A), Offset.Zero, size, r)
                drawRoundRect(if (dentro) Verde else Vermelho, Offset.Zero, Size((size.width * fracao).coerceAtLeast(size.height), size.height), r)
                drawRoundRect(Tinta, Offset.Zero, size, r, style = Stroke(1.5.dp.toPx()))
                val x = size.width / 1.25f
                drawLine(Papel, Offset(x, -4.dp.toPx()), Offset(x, size.height + 4.dp.toPx()), strokeWidth = 2.dp.toPx())
            },
        )
    }
}

@Composable
private fun Cabeca() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(tr("CANABINOIDE", "CANNABINOID"), color = Papel.copy(alpha = 0.6f), fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontFamily = Carbona, modifier = Modifier.weight(1f))
        Text(tr("% EM MASSA", "% BY WEIGHT"), color = Papel.copy(alpha = 0.6f), fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontFamily = Carbona)
    }
}

@Composable
private fun LinhaTabela(nome: String, v: Double, par: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (par) Papel.copy(alpha = 0.06f) else Color.Transparent, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(nome, color = Papel, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, modifier = Modifier.weight(1f))
        Text(pct(v), color = Papel, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Carbona)
    }
}

/** 0.28 -> "0,28 %": virgula decimal, ate duas casas, sem zeros a mais. */
private fun pct(v: Double): String =
    String.format(java.util.Locale.ROOT, "%.2f", v).trimEnd('0').trimEnd('.').replace('.', ',').let { if (it.isEmpty()) "0" else it } + " %"
