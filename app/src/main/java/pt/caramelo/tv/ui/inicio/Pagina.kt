package pt.caramelo.tv.ui.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import pt.caramelo.tv.R
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.formatarTempo
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.theme.Laranja
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta

/**
 * As paginas que abrem a partir do ecra inicial (22/11/2026, Francisco, depois de experimentar a app nova na TV da loja:
 * "o player e a expansao das paginas ainda esta no tema antigo, preciso que coloques tudo consistente"). O leitor de
 * video, a peca aberta e a ficha de uma flor estavam no papel bege de 1/10; passam para o mundo do ecra inicial, com as
 * mesmas pecas e nos mesmos sitios: o logotipo no canto, a ficha em tipografia de revista na primeira coluna (a mesma
 * [Ficha], que fica onde estava) e, no lugar do palco, a janela da marca maior, com o video ou a peca la dentro e a
 * etiqueta do preco a pender do canto. E' a janela que cresce; o resto nao muda de sitio.
 *
 * Medidas em dp, na mesma grelha do ecra inicial (960 x 540): a janela e' quadrada, porque os videos das flores (640 x 640)
 * e os giros das pecas (720 x 720) tambem sao, comeca onde comeca a do ecra inicial (456) e acaba a 114 dp da margem, para
 * a etiqueta caber sem sair do ecra.
 */
object GrelhaPagina {
    val janelaX = 456.dp
    val janelaY = 78.dp
    val janela = 390.dp
    /** O eixo da luz: o meio da janela. */
    val eixo = janelaX + janela / 2
    /** A etiqueta pende do canto de cima a direita da janela, como no ecra inicial. */
    val etiquetaX = janelaX + janela
    val etiquetaY = janelaY - 24.dp
    /** A barra do tempo, por baixo da janela. */
    val barraY = janelaY + janela + 16.dp
    /** O rodape da primeira coluna (o estado do video, os botoes, a assinatura) assenta na margem de baixo. */
    val margemDeBaixo = 34.dp
}

/** O logotipo e o nome, no canto de cima a esquerda: o mesmo desenho e o mesmo sitio em todos os ecras. */
@Composable
fun Marca(modifier: Modifier = Modifier) {
    // partilhado entre os ecras (hero): na passagem de um para outro o logotipo nao pisca, fica onde esta
    Row(modifier.hero("marca"), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo_caramelo), "Caramelo do Brasil", Modifier.height(28.dp))
        Image(painterResource(R.drawable.lettering_caramelo), null, Modifier.padding(start = 8.dp).height(21.dp))
    }
}

/**
 * O topo de uma pagina: o logotipo onde esta no ecra inicial e, a direita, onde no ecra inicial esta o menu, o titulo
 * da pagina (quando o tem) e as teclas do comando que ali valem.
 */
@Composable
fun TopoDaPagina(teclas: List<Pair<String, String>>, titulo: String? = null, cor: Color = Amarelo) {
    Row(
        Modifier.padding(start = Grelha.margem, end = Grelha.margem, top = 22.dp).fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Marca()
        Spacer(Modifier.weight(1f))
        if (titulo != null) {
            Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(titulo, color = Papel, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1)
                Box(Modifier.padding(top = 3.dp).height(2.5.dp).fillMaxWidth().background(cor, RoundedCornerShape(2.dp)))
            }
            Spacer(Modifier.width(24.dp))
        }
        Teclas(teclas)
    }
}

/** As teclas do comando, no traco da dica do ecra inicial: a tecla numa caixa e o que faz ao lado. */
@Composable
fun Teclas(teclas: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        teclas.forEachIndexed { i, (t, d) ->
            Text(
                t, color = Papel.copy(alpha = 0.6f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(start = if (i == 0) 0.dp else 9.dp).border(1.dp, Papel.copy(alpha = 0.6f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp),
            )
            Text(d, color = Papel.copy(alpha = 0.6f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 3.dp))
        }
    }
}

/**
 * O fundo de uma pagina: o azul do mundo de onde ela abre, com o foco de luz sobre a janela. [vivo] falso nas paginas com
 * video (a luz fica parada e sem poeira, para so o video acordar o ecra); verdadeiro nas outras, igual ao ecra inicial.
 */
@Composable
fun FundoDaPagina(luz: Triple<Color, Color, Color> = LuzDasFlores, eixo: Dp = GrelhaPagina.eixo, vivo: Boolean = false) {
    Box(Modifier.fillMaxSize().background(Tinta)) { FundoComLuz(luz.first, luz.second, luz.third, eixo, vivo) }
}

/** O interior da janela antes de haver imagem: o nicho azul do giro das piteiras, com uma luz ao meio. Desenha-se uma vez. */
@Composable
fun NichoDaJanela(modifier: Modifier = Modifier, x: Dp = GrelhaPagina.janelaX, y: Dp = GrelhaPagina.janelaY, lado: Dp = GrelhaPagina.janela) {
    Box(
        modifier.offset(x, y).size(lado).drawWithCache {
            val luz = Brush.radialGradient(listOf(Color(0xFF1D4D7C), Nicho), center = Offset(size.width / 2, size.height * 0.45f), radius = size.width * 0.62f)
            onDrawBehind { drawRoundRect(luz, cornerRadius = CornerRadius(Grelha.janelaCanto.toPx())) }
        },
    )
}

/**
 * A barra do tempo de um video, por baixo da janela, no traco das barras da ficha (o contorno tinta, a sombra dura, o
 * amarelo). [progresso] le-se so no desenho: a barra avanca quatro vezes por segundo sem recompor nada (antes, a posicao
 * era estado lido na composicao e o leitor inteiro recompunha de 250 em 250 ms). Os minutos sao dois textos pequenos, que
 * so mudam de segundo a segundo.
 */
@Composable
fun BarraDeTempo(
    progresso: () -> Float, posicao: () -> Long, duracao: () -> Long, modifier: Modifier = Modifier,
    x: Dp = GrelhaPagina.janelaX, y: Dp = GrelhaPagina.barraY, largura: Dp = GrelhaPagina.janela,
) {
    Row(modifier.offset(x, y).width(largura).height(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Tempo(posicao)
        Box(
            Modifier.weight(1f).padding(horizontal = 9.dp).height(7.dp).drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(1.5.dp.toPx(), 1.5.dp.toPx()), size, r)
                drawRoundRect(Color(0xFF12365A), Offset.Zero, size, r)
                val p = progresso().coerceIn(0f, 1f)
                if (p > 0f) drawRoundRect(Amarelo, Offset.Zero, size.copy(width = (size.width * p).coerceAtLeast(size.height)), r)
                drawRoundRect(Tinta, Offset.Zero, size, r, style = Stroke(1.5.dp.toPx()))
            },
        )
        Tempo(duracao)
    }
}

@Composable
private fun Tempo(ms: () -> Long) {
    // o estado le-se aqui dentro: so este texto recompoe, e so quando o segundo muda
    val segundos = ms() / 1000
    Text(formatarTempo(segundos * 1000), color = Papel.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Carbona, maxLines = 1, softWrap = false)
}

/**
 * A assinatura da marca, copiada do estendarte da loja: aparece em todas as paginas que abrem, no fundo da primeira
 * coluna. O "Caramelos" vai a laranja como no estendarte (e' a unica palavra que la muda de cor).
 */
@Composable
fun Assinatura(modifier: Modifier = Modifier) {
    val frase = buildAnnotatedString {
        withStyle(SpanStyle(color = Papel)) { append(tr("Afinal, somos todos ", "After all, we're all ")) }
        withStyle(SpanStyle(color = Laranja)) { append("Caramelos.") }
    }
    Column(modifier) {
        Box(Modifier.width(28.dp).height(2.5.dp).background(Laranja, RoundedCornerShape(2.dp)))
        Text(frase, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontFamily = Carbona, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Uma linha pequena de estado (a reproduzir, em pausa, esgotado): um ponto na cor e o texto em maiusculas espacadas. */
@Composable
fun Estado(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(cor, RoundedCornerShape(1.5.dp)))
        Text(texto.uppercase(), color = Papel.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontFamily = Carbona, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 7.dp))
    }
}

/** O rodape da primeira coluna de uma pagina: o que a pagina tem para dizer e, por baixo, a assinatura. */
@Composable
fun PeDaPagina(assinatura: Boolean = true, conteudo: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxSize().padding(start = Grelha.fichaX, bottom = GrelhaPagina.margemDeBaixo)) {
        Column(Modifier.align(Alignment.BottomStart).width(Grelha.fichaLargura)) {
            conteudo()
            if (assinatura) Assinatura(Modifier.padding(top = 12.dp))
        }
    }
}
