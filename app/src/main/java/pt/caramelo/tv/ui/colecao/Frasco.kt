package pt.caramelo.tv.ui.colecao

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import pt.caramelo.tv.R
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.precoCurto
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Vermelho
import pt.caramelo.tv.ui.theme.corDaSeccao
import kotlin.math.roundToInt

/**
 * O frasco de vidro de uma flor (18/11/2026): as camadas do frasco feito no Blender (scripts/frasco/frasco.py) empilhadas a
 * volta da foto recortada da flor, que vem do site. O vidro de tras fica por baixo da flor e o da frente por cima, com os
 * reflexos; a tampa tem a cor da seccao e e' uma camada a parte para poder levantar. O rotulo de papel, com o nome e o
 * preco, desenha-se aqui (Carbona), por isso uma flor nova no painel entra num frasco sozinha.
 *
 * Tudo o que mexe (o foco, a tampa, o brilho) passa por graphicsLayer e pelo desenho: o frasco nao se recompoe a cada
 * fotograma.
 */
object FrascoGeo {
    /** Largura / altura das camadas renderizadas. */
    const val PROPORCAO = 0.727f
    /**
     * Onde a flor assenta (fracoes da imagem): o interior do corpo, do fundo grosso ao ombro, um pouco para dentro do
     * vidro. Do geometria.json do render de 18/11/2026 (interior 0,130 a 0,870 por 0,234 a 0,859).
     */
    val interior = Fracao(0.165f, 0.245f, 0.835f, 0.850f)
    /** Onde vai o rotulo de papel: a parte de baixo do corpo, com a flor a ver-se por cima. */
    val rotulo = Fracao(0.21f, 0.655f, 0.79f, 0.805f)
}

data class Fracao(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

/** Coloca o filho no rectangulo [f] (fracoes) da caixa do pai. */
fun Modifier.naFracao(f: Fracao): Modifier = layout { m, c ->
    val w = (c.maxWidth * (f.x1 - f.x0)).roundToInt(); val h = (c.maxHeight * (f.y1 - f.y0)).roundToInt()
    val p = m.measure(Constraints.fixed(w.coerceAtLeast(0), h.coerceAtLeast(0)))
    layout(c.maxWidth, c.maxHeight) { p.place((c.maxWidth * f.x0).roundToInt(), (c.maxHeight * f.y0).roundToInt()) }
}

/**
 * As camadas do frasco, descodificadas uma vez para a app inteira (22/11/2026, Francisco: "ve se da para otimizar
 * performance"). Cada frasco sao tres imagens de 400 x 550 (o vidro de tras, o da frente e a tampa), iguais em todos, e
 * ha uns quinze no ecra inicial. Com o `painterResource`, cada frasco que entrava na composicao (ao correr a fila, ao
 * mudar o do palco) ia buscar as suas a's resources: o Android so as guarda por referencia fraca, por isso com pouca
 * memoria, que e' o caso da TCL, voltava a descodificar os WebP na linha principal, a meio da animacao. Aqui ficam
 * cinco bitmaps (4 MB) presos enquanto a app vive.
 */
private object CamadasDoFrasco {
    private val guardadas = HashMap<Int, ImageBitmap>()
    fun de(res: android.content.res.Resources, id: Int): ImageBitmap = guardadas.getOrPut(id) { ImageBitmap.imageResource(res, id) }
}

@Composable
private fun camada(id: Int): BitmapPainter {
    val res = LocalContext.current.resources
    return remember(id) { BitmapPainter(CamadasDoFrasco.de(res, id)) }
}

fun tampaDe(s: Section): Int = when (s) {
    Section.CBD -> R.drawable.frasco_tampa_verde
    Section.INFUSAO -> R.drawable.frasco_tampa_roxo
    Section.HASH -> R.drawable.frasco_tampa_laranja
}

@Composable
fun Frasco(
    item: MenuItem,
    fotoUrl: String,
    focado: Boolean,
    modifier: Modifier = Modifier,
    /**
     * 0 fechado, 1 com a tampa no ar (ao abrir o frasco). E' uma funcao, lida so no desenho: a tampa a flutuar no palco do
     * ecra inicial anima a cada fotograma sem recompor o frasco.
     */
    abertura: () -> Float = { 0f },
    /** A foto da flor voa para a ficha a partir daqui (hero partilhado com o leitor). */
    comHero: Boolean = true,
    /** Falso no palco do ecra inicial: ali o tamanho e' o do palco, e o foco so liga o brilho. */
    cresce: Boolean = true,
    /** O rotulo de papel; os frascos pequenos das filas nao o levam. */
    comRotulo: Boolean = true,
) {
    val foco by animateFloatAsState(if (focado && cresce) 1f else 0f, pt.caramelo.tv.ui.theme.Movimento.foco, label = "foco-frasco")
    Box(
        modifier
            .aspectRatio(FrascoGeo.PROPORCAO)
            .graphicsLayer {
                val e = 1f + 0.12f * foco
                scaleX = e; scaleY = e
                translationY = -10.dp.toPx() * foco
                // A base assenta na tabua: cresce para cima, nao para baixo.
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
    ) {
        Image(camada(R.drawable.frasco_tras), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        // A flor, dentro do vidro. O brevemente e' o frasco vazio, so com o rotulo.
        if (!item.brevemente && fotoUrl.isNotBlank()) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                colorFilter = if (item.esgotado) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.3f) }) else null,
                modifier = Modifier.naFracao(FrascoGeo.interior).let { if (comHero) it.hero(item.id) else it }
                    .graphicsLayer { alpha = if (item.esgotado) 0.7f else 1f },
            )
        }

        Image(
            camada(R.drawable.frasco_frente), null,
            Modifier.fillMaxSize().brilho(focado),
            contentScale = ContentScale.FillBounds,
        )
        Image(
            camada(tampaDe(item.section)), null,
            Modifier.fillMaxSize().graphicsLayer {
                val a = abertura()
                translationY = -size.height * 0.18f * a
                rotationZ = -10f * a
            },
            contentScale = ContentScale.FillBounds,
        )
        if (comRotulo) Rotulo(item, Modifier.naFracao(FrascoGeo.rotulo))
    }
}

/** O brilho que corre pelo vidro do frasco em foco: uma faixa clara na diagonal, so no desenho. */
@Composable
private fun Modifier.brilho(activo: Boolean): Modifier {
    if (!activo) return this
    val t = rememberInfiniteTransition(label = "brilho")
    val x by t.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "brilho-x")
    return drawWithContent {
        drawContent()
        val i = FrascoGeo.interior
        val l = size.width * i.x0; val r = size.width * i.x1; val top = size.height * 0.16f; val bot = size.height * i.y1
        val cx = l + (r - l) * x; val w = (r - l) * 0.22f
        drawContext.canvas.save()
        drawContext.canvas.clipRect(l, top, r, bot)
        drawRect(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0f)),
                start = Offset(cx - w, top), end = Offset(cx + w, top + w * 0.6f),
            ),
            topLeft = Offset(l, top), size = androidx.compose.ui.geometry.Size(r - l, bot - top),
        )
        drawContext.canvas.restore()
    }
}

/**
 * O rotulo de papel colado no frasco: o nome em Carbona e o preco na cor da seccao; ESGOTADO a vermelho por cima quando
 * nao ha. A letra segue a largura do rotulo (18/11/2026): no frasco grande do palco o nome encolhe para caber numa linha.
 */
@Composable
private fun Rotulo(item: MenuItem, modifier: Modifier) {
    val forma = RoundedCornerShape(3.dp)
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val densidade = androidx.compose.ui.platform.LocalDensity.current
        val largura = with(densidade) { maxWidth.toPx() }
        val nome = item.title.uppercase()
        // Carbona Black: ~0,66 da letra por caracter; o nome ocupa no maximo 84 % do papel
        val letraPx = minOf(largura * 0.12f, largura * 0.84f / (maxOf(6, nome.length) * 0.66f))
        val letra = with(densidade) { letraPx.toSp() }
        val letraPreco = with(densidade) { (largura * 0.16f).toSp() }
        Column(
            Modifier.fillMaxWidth().background(Papel, forma).border(1.5.dp, Tinta, forma).padding(horizontal = 3.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                nome,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = letra, lineHeight = letra * 1.1f, letterSpacing = 0.3.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
                color = Tinta, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
            )
            val p = when {
                item.brevemente -> tr("EM BREVE", "SOON")
                else -> precoCurto(item.preco)
            }
            if (p.isNotBlank()) Text(
                p,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = letraPreco, lineHeight = letraPreco * 1.1f, fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
                color = corDaSeccao(item.section).let { if (item.section == Section.HASH) Tinta else it },
            )
        }
        if (item.esgotado) Text(
            tr("ESGOTADO", "SOLD OUT"),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp),
            color = Vermelho,
            modifier = Modifier.graphicsLayer { rotationZ = -12f }
                .background(Papel.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
                .border(2.dp, Vermelho, RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}
