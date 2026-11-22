package pt.caramelo.tv.ui.inicio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.Especificacao
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.components.precoCurto
import pt.caramelo.tv.ui.theme.corDaSeccao
import pt.caramelo.tv.ui.animation.Entrada
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.Aroma
import pt.caramelo.tv.ui.components.DesenhoDeAroma
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Verde
import kotlin.math.pow

/** O que a ficha mostra, ja tirado da flor ou do artigo (InicioScreen faz a conta). */
data class ConteudoFicha(
    val chave: String,
    val chip: String,
    val corChip: Color,
    val chipClaro: Boolean,
    val olho: String,
    val nome: String,
    val tipo: String,
    /** Flores: a percentagem de indica (null quando nao se sabe) e os terpenos com o comprimento da barra. */
    val indica: Int? = null,
    val terpenos: List<Pair<String, Float>> = emptyList(),
    val aromas: List<Aroma> = emptyList(),
    /** Pecas: as quatro caixas da ficha tecnica. */
    val especificacoes: List<Especificacao> = emptyList(),
)

/** O conteudo da ficha de uma flor: o grupo, o lote (ou o cultivo), o tipo, a barra, os terpenos e os aromas. */
@Composable
internal fun fichaDaFlor(item: MenuItem): ConteudoFicha {
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
internal fun fichaDoArtigo(p: Parafernalia, s: Seccao): ConteudoFicha {
    val piteira = s == Seccao.PITEIRAS
    return ConteudoFicha(
        chave = "p:" + p.slug,
        chip = if (piteira) tr("PITEIRAS", "TIPS") else tr("PARAFERNÁLIA", "ACCESSORIES"),
        corChip = if (piteira) Amarelo else Laranja, chipClaro = true,
        olho = Inicio.olhoDe(p), nome = Inicio.nomeDe(p), tipo = Inicio.tipoDe(p),
        especificacoes = Inicio.especificacoesDe(p).ifEmpty { listOf(pt.caramelo.tv.data.model.Especificacao(tr("Preço", "Price"), precoCurto(p.preco))) },
    )
}

/**
 * A ficha do produto em foco, em tipografia de revista, na primeira coluna (360 dp). O nome encolhe para caber numa
 * linha e entra letra a letra; os blocos entram em cascata. Trocar de produto refaz a entrada (key pela chave).
 * [olho] vem a parte porque na parafernalia muda com a cor que esta no pedestal, sem refazer a entrada.
 */
@Composable
fun Ficha(
    c: ConteudoFicha, olho: String, modifier: Modifier = Modifier,
    /**
     * Falso desenha a ficha ja assente, sem a entrada (as letras a cair, os blocos em cascata, as barras a crescer). E' o
     * que as paginas que abrem pedem na primeira vez: a ficha ja estava no ecra inicial, no mesmo sitio, e a pagina so
     * cresce a volta dela; se voltasse a entrar, parecia outro ecra.
     */
    entrada: Boolean = true,
    /** O que a pagina acrescenta por baixo (a genetica, os botoes): entra na mesma coluna, depois dos dados. */
    depois: @Composable () -> Unit = {},
) {
    // partilhada com a pagina que abre (hero, pela chave do produto): na passagem a ficha fica onde esta, sem piscar
    Box(modifier.offset(Grelha.fichaX, Grelha.fichaY).width(Grelha.fichaLargura).hero("ficha-" + c.chave)) {
        key(c.chave) {
            Column {
                Entrada(0, activa = entrada) { Olho(c, olho) }
                Spacer(Modifier.height(8.dp))
                NomeQueCabe(c.nome, entrada)
                Entrada(2, activa = entrada) {
                    Text(c.tipo, color = Amarelo, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 6.dp))
                }
                if (c.especificacoes.isNotEmpty()) {
                    Entrada(3, activa = entrada) { Especificacoes(c.especificacoes, Modifier.padding(top = 14.dp)) }
                } else {
                    c.indica?.let { Entrada(3, activa = entrada) { BarraIndica(it, Modifier.padding(top = 14.dp), entrada) } }
                    if (c.terpenos.isNotEmpty()) Entrada(4, activa = entrada) { Terpenos(c.terpenos, Modifier.padding(top = 12.dp), entrada) }
                    if (c.aromas.isNotEmpty()) Entrada(5, activa = entrada) { AromasDaFicha(c.aromas, Modifier.padding(top = 13.dp)) }
                }
                depois()
            }
        }
    }
}

@Composable
private fun Olho(c: ConteudoFicha, olho: String) {
    Row(Modifier.height(21.dp), verticalAlignment = Alignment.CenterVertically) {
        val forma = RoundedCornerShape(4.dp)
        Box(
            Modifier.drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(2.dp.toPx(), 2.dp.toPx()), size, CornerRadius(4.dp.toPx())) }
                .background(c.corChip, forma).border(1.5.dp, Tinta, forma).padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(c.chip, color = if (c.chipClaro) Tinta else Papel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp, fontFamily = Carbona, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.width(7.dp))
        LinhaQueCabe(olho, 280.dp, 10f, 7.5f) { tam ->
            TextStyle(color = Papel, fontSize = tam, fontWeight = FontWeight.ExtraBold, letterSpacing = tam * 0.13f, fontFamily = Carbona)
        }
    }
}

/** Um texto numa linha que encolhe ate caber em [largura] (de [de] a [ate] sp, medido antes de desenhar). */
@Composable
private fun LinhaQueCabe(texto: String, largura: Dp, de: Float, ate: Float, estilo: (TextUnit) -> TextStyle) {
    val medidor = rememberTextMeasurer()
    val densidade = androidx.compose.ui.platform.LocalDensity.current
    val max = with(densidade) { largura.roundToPx() }
    val (tam, final) = remember(texto, max) {
        fun cabe(t: String, s: Float) = medidor.measure(t, estilo(s.sp), maxLines = 1, softWrap = false).size.width <= max
        var s = de
        while (s > ate && !cabe(texto, s)) s -= 0.5f
        // nem na letra mais pequena: fica o que vem antes da virgula ("Magnetica, gravada com o mascote" da "Magnetica")
        if (!cabe(texto, s) && texto.contains(", ")) {
            val curto = texto.substringBefore(", "); var s2 = de
            while (s2 > ate && !cabe(curto, s2)) s2 -= 0.5f
            s2 to curto
        } else s to texto
    }
    Text(final, style = estilo(tam.sp), maxLines = 1, softWrap = false)
}

/**
 * O nome: Carbona Black, de 62 sp ate 35 sp ate caber nos 360 dp, a entrar letra a letra (uma animacao so, lida no
 * desenho de cada letra: sem recompor a cada fotograma).
 */
@Composable
private fun NomeQueCabe(nome: String, entrada: Boolean = true) {
    val medidor = rememberTextMeasurer()
    val densidade = androidx.compose.ui.platform.LocalDensity.current
    val max = with(densidade) { Grelha.fichaLargura.roundToPx() }
    val estilo = { s: Float -> TextStyle(fontSize = s.sp, fontWeight = FontWeight.Black, fontFamily = Carbona, letterSpacing = (-1).sp, color = Papel) }
    val tam = remember(nome, max) {
        var s = 62f
        while (s > 35f && medidor.measure(nome, estilo(s), maxLines = 1, softWrap = false).size.width > max) s -= 2f
        s
    }
    val passo = 28; val dura = 700
    val total = (nome.length * passo + dura).toFloat()
    val t = remember(nome) { Animatable(if (entrada) 0f else total) }
    LaunchedEffect(nome) { if (entrada) t.animateTo(total, tween(total.toInt(), easing = LinearEasing)) }
    Row(Modifier.height(61.dp), verticalAlignment = Alignment.Bottom) {
        nome.forEachIndexed { i, ch ->
            Text(
                ch.toString(), style = estilo(tam), maxLines = 1, softWrap = false,
                modifier = Modifier.graphicsLayer {
                    val p = ((t.value - i * passo) / dura).coerceIn(0f, 1f)
                    // a mola da maqueta (cubic-bezier .34, 1.4, .5, 1): passa um nada e assenta
                    val e = 1f + 2.4f * (p - 1f).pow(3) + 1.4f * (p - 1f).pow(2)
                    alpha = p.coerceAtMost(1f); translationY = (1f - e) * 23.dp.toPx(); rotationZ = (1f - e) * 5f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
            )
        }
    }
}

/** A barra indica / sativa: amarelo a indica, verde a sativa, com o contorno tinta da marca; cresce ao entrar. */
@Composable
private fun BarraIndica(indica: Int, modifier: Modifier = Modifier, entrada: Boolean = true) {
    val alvo = remember { Animatable(if (entrada) 0.5f else indica / 100f) }
    LaunchedEffect(indica) { if (entrada) kotlinx.coroutines.delay(420); alvo.animateTo(indica / 100f, spring(0.6f, 200f)) }
    Column(modifier.width(Grelha.dadosLargura)) {
        Row(Modifier.fillMaxWidth()) {
            Text(tr("ÍNDICA ", "INDICA ") + indica, color = Papel, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona)
            Spacer(Modifier.weight(1f))
            Text(tr("SATIVA ", "SATIVA ") + (100 - indica), color = Papel, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona)
        }
        Box(
            Modifier.padding(top = 4.5.dp).fillMaxWidth().height(8.dp)
                .drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(1.5.dp.toPx(), 1.5.dp.toPx()), size, r)
                    drawRoundRect(Verde, Offset.Zero, size, r)
                    drawRoundRect(Amarelo, Offset.Zero, size.copy(width = size.width * alvo.value), r)
                    drawRoundRect(Tinta, Offset.Zero, size, r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                },
        )
    }
}

@Composable
private fun Terpenos(terpenos: List<Pair<String, Float>>, modifier: Modifier = Modifier, entrada: Boolean = true) {
    val cresce = remember { Animatable(if (entrada) 0f else 1f) }
    LaunchedEffect(Unit) { if (entrada) { kotlinx.coroutines.delay(420); cresce.animateTo(1f, spring(0.6f, 200f)) } }
    Column(modifier.width(Grelha.dadosLargura), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        terpenos.forEach { (nome, f) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nome.replaceFirstChar { it.uppercaseChar() }, color = Papel.copy(alpha = 0.92f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, modifier = Modifier.width(94.dp), maxLines = 1)
                Box(
                    Modifier.weight(1f).height(5.dp).drawBehind {
                        drawRoundRect(Brush.horizontalGradient(listOf(Amarelo, Laranja)), Offset.Zero, size.copy(width = size.width * f * cresce.value), CornerRadius(size.height / 2))
                    },
                )
            }
        }
    }
}

@Composable
private fun AromasDaFicha(aromas: List<Aroma>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        aromas.take(3).forEach { a ->
            Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DesenhoDeAroma(a, Modifier.size(25.dp))
                Text(tr(a.pt, a.en), color = Papel.copy(alpha = 0.92f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 3.5.dp))
            }
        }
    }
}

/** A ficha tecnica em 2 x 2 caixas iguais, como a maqueta. */
@Composable
private fun Especificacoes(e: List<Especificacao>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        e.chunked(2).forEach { par ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                par.forEach { x ->
                    val forma = RoundedCornerShape(7.dp)
                    Column(
                        Modifier.size(146.5.dp, 39.dp).background(Tinta.copy(alpha = 0.55f), forma).border(1.5.dp, Papel.copy(alpha = 0.22f), forma)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(x.rotulo.uppercase(), color = Papel.copy(alpha = 0.6f), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontFamily = Carbona, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        LinhaQueCabe(x.valor, 130.dp, 11.5f, 8.5f) { tam -> TextStyle(color = Papel, fontSize = tam, fontWeight = FontWeight.Bold, fontFamily = Carbona) }
                    }
                }
            }
        }
    }
}
