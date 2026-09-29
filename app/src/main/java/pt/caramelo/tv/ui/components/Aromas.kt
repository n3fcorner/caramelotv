package pt.caramelo.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.CremeFraco
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.LaranjaEscuro
import pt.caramelo.tv.ui.theme.Lilas
import pt.caramelo.tv.ui.theme.VerdeClaro

/**
 * Os aromas em pequenos desenhos de traco colorido (29/9/2026, pedido do Leo: "desenhos
 * pequenos de traçado colorido, a substituir aromas").
 *
 * Cada familia de aroma tem um desenho feito de linhas simples, na cor da paleta, com o
 * nome por baixo nas duas linguas. Desenha-se com o Canvas, sem imagens: nitido em
 * qualquer TV e sem custo de memoria. So objetos da fruta, da cozinha e da natureza,
 * nunca plantas que se confundam com a folha que a marca nao usa.
 *
 * O aroma escrito continua a ser a fonte: le-se por palavras-chave e desenham-se ate
 * quatro familias; sem nenhuma reconhecida, fica o texto.
 */
enum class Aroma(val pt: String, val en: String, val cor: Color, private val chaves: List<String>) {
    CITRINO("Cítrico", "Citrus", Amarelo, listOf("lim", "cítric", "citric", "citrin", "casca", "tanger")),
    FRUTOS_VERMELHOS("Frutos vermelhos", "Red berries", Color(0xFFE13831), listOf("frutos vermelhos", "silvestre", "mirtilo", "baga", "berry", "morango", "cereja")),
    UVA("Uva", "Grape", Lilas, listOf("uva", "frutos escuros")),
    MANGA("Manga", "Mango", Laranja, listOf("manga")),
    PESSEGO("Pêssego", "Peach", Laranja, listOf("pêssego", "pessego", "alperce")),
    MACA("Maçã", "Apple", VerdeClaro, listOf("maçã", "maca verde")),
    PAPAIA("Papaia", "Papaya", LaranjaEscuro, listOf("papaia", "papaya")),
    TERRA("Terroso", "Earthy", LaranjaEscuro, listOf("terros", "terra")),
    PINHO("Pinho", "Pine", VerdeClaro, listOf("pinho", "pine")),
    DOCE("Doce", "Sweet", Creme, listOf("doce", "forno", "açúcar", "mel")),
    CREME("Cremoso", "Creamy", Creme, listOf("cremos", "baunilha", "nata")),
    FLORAL("Floral", "Floral", Lilas, listOf("floral", "flores", "lavanda")),
    DIESEL("Diesel", "Diesel", Color(0xFF3374D8), listOf("diesel", "gasolina")),
    CAFE("Café", "Coffee", LaranjaEscuro, listOf("café", "cafe", "cacau", "chocolate")),
    AMENDOIM("Amendoim", "Peanut", Amarelo, listOf("amendoim", "noz", "avelã")),
    ESPECIARIA("Especiado", "Spiced", Laranja, listOf("especia", "pimenta", "canela")),
    RESINA("Resinoso", "Resinous", Amarelo, listOf("resin", "resina")),
    ERVAS("Herbáceo", "Herbal", VerdeClaro, listOf("herbác", "herbac", "ervas", "menta", "hortelã"));

    fun reconhece(texto: String) = chaves.any { it in texto }

    companion object {
        /** As familias de um aroma escrito, pela ordem em que aparecem, ate [maximo]. */
        fun de(texto: String, maximo: Int = 4): List<Aroma> {
            val t = texto.lowercase()
            return entries
                .mapNotNull { a -> a.chavesPos(t)?.let { a to it } }
                .sortedBy { it.second }
                .map { it.first }
                .take(maximo)
        }
    }

    private fun chavesPos(t: String): Int? = chaves.mapNotNull { k -> t.indexOf(k).takeIf { it >= 0 } }.minOrNull()
}

/** A fila de desenhos com o nome por baixo. Sem nenhuma familia reconhecida, o texto tal como esta. */
@Composable
fun AromasIlustrados(texto: String, textoTraduzido: String, modifier: Modifier = Modifier, tamanho: Dp = 44.dp) {
    val familias = remember(texto) { Aroma.de(texto) }
    Column(modifier) {
        Text(tr("AROMA", "AROMA"), style = MaterialTheme.typography.labelSmall, color = Amarelo, letterSpacing = 2.sp)
        if (familias.isEmpty()) {
            Text(textoTraduzido, style = MaterialTheme.typography.bodyMedium, color = Creme, modifier = Modifier.padding(top = Espaco.minimo))
            return@Column
        }
        Row(Modifier.padding(top = Espaco.curto), horizontalArrangement = Arrangement.spacedBy(Espaco.medio)) {
            familias.forEach { a ->
                Column(Modifier.width(tamanho + 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    DesenhoDeAroma(a, Modifier.size(tamanho))
                    Text(
                        text = tr(a.pt, a.en),
                        style = MaterialTheme.typography.labelSmall,
                        color = CremeFraco,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Espaco.minimo),
                    )
                }
            }
        }
    }
}

/** Um desenho de aroma, a traco. A grelha e' de 24 unidades, como os icones da interface. */
@Composable
fun DesenhoDeAroma(aroma: Aroma, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val traco = Stroke(width = 1.9f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        desenhar(aroma, u, traco, aroma.cor)
    }
}

private fun DrawScope.p(u: Float, x: Float, y: Float) = Offset(x * u, y * u)

private fun DrawScope.desenhar(a: Aroma, u: Float, s: Stroke, c: Color) {
    fun circulo(x: Float, y: Float, r: Float) = drawCircle(c, r * u, p(u, x, y), style = s)
    fun linha(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, p(u, x1, y1), p(u, x2, y2), s.width, StrokeCap.Round)
    fun caminho(bloco: Path.() -> Unit) = drawPath(Path().apply(bloco), c, style = s)
    fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
    fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
    fun Path.q(cx: Float, cy: Float, x: Float, y: Float) = quadraticTo(cx * u, cy * u, x * u, y * u)
    when (a) {
        // Rodela de limao: o circulo, a casca por dentro e os gomos.
        Aroma.CITRINO -> {
            circulo(12f, 12f, 9f); circulo(12f, 12f, 6.5f)
            for (i in 0 until 6) {
                val ang = Math.toRadians(i * 60.0)
                linha(12f, 12f, 12f + 6.5f * Math.cos(ang).toFloat(), 12f + 6.5f * Math.sin(ang).toFloat())
            }
        }
        // Tres bagas e o pe.
        Aroma.FRUTOS_VERMELHOS -> {
            circulo(8.5f, 14.5f, 4f); circulo(15.5f, 14.5f, 4f); circulo(12f, 8.5f, 3.6f)
            caminho { m(12f, 5f); q(13f, 2.5f, 16f, 2.5f) }
        }
        // Cacho de uva: tres, dois, um, e o pe.
        Aroma.UVA -> {
            listOf(7.5f to 8f, 12f to 8f, 16.5f to 8f, 9.8f to 12.5f, 14.2f to 12.5f, 12f to 17f).forEach { (x, y) -> circulo(x, y, 2.4f) }
            caminho { m(12f, 5.6f); q(12f, 3f, 14.5f, 2.2f) }
        }
        // Manga: o feijao inclinado e o pe com uma folha pequena.
        Aroma.MANGA -> {
            caminho { m(6f, 15f); q(4.5f, 6f, 13f, 5f); q(20.5f, 4.5f, 19f, 13f); q(17.5f, 21f, 10f, 20f); q(6.5f, 19.5f, 6f, 15f) }
            linha(13f, 5f, 12.5f, 2.5f)
        }
        // Pessego: o circulo com o sulco e o pe.
        Aroma.PESSEGO -> {
            circulo(12f, 13.5f, 8f)
            caminho { m(12f, 5.5f); q(9f, 12f, 12f, 21.5f) }
            linha(12f, 5.5f, 13.5f, 2.5f)
        }
        // Maca: dois lobos em cima, o pe e a folhinha redonda.
        Aroma.MACA -> {
            caminho { m(12f, 7.5f); q(4f, 3.5f, 4.5f, 12.5f); q(5.5f, 21f, 12f, 20f); q(18.5f, 21f, 19.5f, 12.5f); q(20f, 3.5f, 12f, 7.5f) }
            linha(12f, 7.5f, 12.5f, 3f)
            caminho { m(12.5f, 4.5f); q(15.5f, 2f, 17.5f, 4f); q(15f, 6f, 12.5f, 4.5f) }
        }
        // Meia papaia: a forma de pera com as sementes.
        Aroma.PAPAIA -> {
            caminho { m(12f, 3f); q(19f, 6f, 18.5f, 14f); q(18f, 21f, 12f, 21f); q(6f, 21f, 5.5f, 14f); q(5f, 6f, 12f, 3f) }
            listOf(12f to 12f, 10.5f to 15f, 13.5f to 15f, 12f to 18f).forEach { (x, y) -> drawCircle(c, 1.1f * u, p(u, x, y)) }
        }
        // Terra: tres camadas onduladas e duas pedras.
        Aroma.TERRA -> {
            caminho { m(3f, 10f); q(7.5f, 7f, 12f, 10f); q(16.5f, 13f, 21f, 10f) }
            caminho { m(3f, 15f); q(7.5f, 12f, 12f, 15f); q(16.5f, 18f, 21f, 15f) }
            linha(3f, 20f, 21f, 20f)
            circulo(8f, 4.8f, 1.6f); circulo(15f, 5.4f, 1.1f)
        }
        // Pinha: o oval com as escamas cruzadas.
        Aroma.PINHO -> {
            caminho { m(12f, 3f); q(19f, 8f, 17f, 16f); q(15f, 21.5f, 12f, 21.5f); q(9f, 21.5f, 7f, 16f); q(5f, 8f, 12f, 3f) }
            linha(8f, 10f, 16f, 16f); linha(16f, 10f, 8f, 16f); linha(7.5f, 14f, 12f, 19f); linha(16.5f, 14f, 12f, 19f)
        }
        // Doce: o rebuçado embrulhado.
        Aroma.DOCE -> {
            circulo(12f, 12f, 5f)
            caminho { m(7.2f, 10.5f); l(3f, 7.5f); l(3f, 16.5f); l(7.2f, 13.5f) }
            caminho { m(16.8f, 10.5f); l(21f, 7.5f); l(21f, 16.5f); l(16.8f, 13.5f) }
        }
        // Cremoso: a bola de gelado no cone.
        Aroma.CREME -> {
            caminho { m(6f, 11f); q(6f, 3.5f, 12f, 3.5f); q(18f, 3.5f, 18f, 11f); l(6f, 11f) }
            caminho { m(6.5f, 11f); l(12f, 21.5f); l(17.5f, 11f) }
            linha(9f, 14f, 15f, 14f)
        }
        // Floral: cinco petalas a volta do miolo.
        Aroma.FLORAL -> {
            circulo(12f, 12f, 2.4f)
            for (i in 0 until 5) {
                val ang = Math.toRadians(-90.0 + i * 72.0)
                circulo(12f + 5.6f * Math.cos(ang).toFloat(), 12f + 5.6f * Math.sin(ang).toFloat(), 3.2f)
            }
        }
        // Diesel: a gota de combustivel com o reflexo.
        Aroma.DIESEL -> {
            caminho { m(12f, 3f); q(19f, 11f, 18f, 15f); q(17f, 21f, 12f, 21f); q(7f, 21f, 6f, 15f); q(5f, 11f, 12f, 3f) }
            caminho { m(9f, 15.5f); q(9.5f, 18f, 12f, 18.3f) }
        }
        // Cafe: a chavena, a asa e o vapor.
        Aroma.CAFE -> {
            caminho { m(4f, 11f); l(16f, 11f); l(15f, 18.5f); q(14.5f, 20.5f, 12f, 20.5f); l(8f, 20.5f); q(5.5f, 20.5f, 5f, 18.5f); l(4f, 11f) }
            caminho { m(16f, 12.5f); q(20.5f, 12.5f, 19.5f, 15.5f); q(18.5f, 17.5f, 15.4f, 17f) }
            caminho { m(8f, 8.5f); q(6.5f, 6f, 8f, 3.5f) }
            caminho { m(12f, 8.5f); q(10.5f, 6f, 12f, 3.5f) }
        }
        // Amendoim: a casca em oito com o riscado.
        Aroma.AMENDOIM -> {
            caminho { m(12f, 3f); q(17f, 3f, 16.5f, 8.5f); q(15.5f, 11.5f, 16.5f, 14.5f); q(17.5f, 21f, 12f, 21f); q(6.5f, 21f, 7.5f, 14.5f); q(8.5f, 11.5f, 7.5f, 8.5f); q(7f, 3f, 12f, 3f) }
            linha(10f, 6.5f, 14f, 6.5f); linha(10f, 17.5f, 14f, 17.5f)
        }
        // Especiado: a estrela de anis.
        Aroma.ESPECIARIA -> {
            caminho {
                for (i in 0 until 8) {
                    val a1 = Math.toRadians(i * 45.0); val a2 = Math.toRadians(i * 45.0 + 22.5)
                    val x1 = 12f + 9f * Math.cos(a1).toFloat(); val y1 = 12f + 9f * Math.sin(a1).toFloat()
                    val x2 = 12f + 4f * Math.cos(a2).toFloat(); val y2 = 12f + 4f * Math.sin(a2).toFloat()
                    if (i == 0) m(x1, y1) else l(x1, y1)
                    l(x2, y2)
                }
                close()
            }
            circulo(12f, 12f, 1.4f)
        }
        // Resinoso: duas gotas de ambar a escorrer.
        Aroma.RESINA -> {
            caminho { m(8f, 3f); q(12.5f, 9f, 12f, 12f); q(11.5f, 15f, 8f, 15f); q(4.5f, 15f, 4f, 12f); q(3.5f, 9f, 8f, 3f) }
            caminho { m(16.5f, 10f); q(20.5f, 15f, 20f, 17.5f); q(19.5f, 20.5f, 16.5f, 20.5f); q(13.5f, 20.5f, 13f, 17.5f); q(12.5f, 15f, 16.5f, 10f) }
        }
        // Herbaceo: um raminho de alecrim, caule direito e folhas finas aos pares.
        Aroma.ERVAS -> {
            linha(12f, 21f, 12f, 3f)
            listOf(6f, 10f, 14f, 18f).forEach { y ->
                linha(12f, y, 7f, y - 2.5f); linha(12f, y, 17f, y - 2.5f)
            }
        }
    }
}

