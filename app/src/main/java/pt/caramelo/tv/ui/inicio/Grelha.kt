package pt.caramelo.tv.ui.inicio

import androidx.compose.ui.unit.dp

/**
 * As medidas do ecra inicial (18/11/2026), tiradas da maqueta 4 que o Francisco aprovou (arte/maqueta/tv.html), em dp:
 * a maqueta e' a 1920 x 1080 px e a TV desenha 960 x 540 dp, por isso tudo e' metade dos pixeis da maqueta.
 *
 * "Atencao a espacamentos e simetrias": margens iguais dos dois lados; tres colunas no topo com 20 dp entre elas (a
 * ficha, o palco e o Caramelo); o palco tem o eixo no meio da coluna e tudo o que la esta (o frasco, a janela, o balcao,
 * o foco de luz) assenta nesse eixo; a ficha e a janela comecam a mesma altura; as filas comecam aos 362.
 */
object Grelha {
    val margem = 56.dp

    val fichaX = 56.dp
    val fichaY = 80.dp
    val fichaLargura = 360.dp
    /** As barras, os terpenos e a ficha tecnica: um pouco mais estreitos que a coluna. */
    val dadosLargura = 300.dp

    /** O eixo do palco (o meio da coluna do meio, de 436 a 736). */
    val eixo = 586.dp

    val carameloX = 756.dp
    val carameloY = 75.dp
    val carameloLargura = 148.dp
    val carameloAltura = 270.dp

    /** A janela da marca (piteiras) e o estudio (parafernalia): o mesmo rectangulo, centrado no eixo. */
    val janelaX = 456.dp
    val janelaY = 80.dp
    val janelaLargura = 260.dp
    val janelaAltura = 250.dp
    val janelaBorda = 2.5.dp
    val janelaCanto = 14.dp
    /** A sombra dura da janela, na cor da seccao. */
    val janelaSombra = 8.dp
    /** A etiqueta do preco pende do canto de cima a direita da janela. */
    val etiquetaX = 716.dp
    val etiquetaY = 56.dp

    /** O balcao das flores, centrado no eixo, e o frasco em foco (165 dp) com os vizinhos (70 dp) a 137,5 dp. */
    val balcaoX = 411.dp
    val balcaoY = 305.dp
    val balcaoLargura = 350.dp
    val frascoBase = 307.dp
    val frascoCentro = 165.dp
    val frascoLado = 70.dp
    val frascoAfastamento = 137.5.dp

    /** As filas: cada uma com 120 dp; a activa sobe ao topo da faixa, que comeca aos 362. */
    val filasY = 362.dp
    val filaAltura = 120.dp
    val cartaoLargura = 70.dp
    val cartaoAltura = 73.dp
    val cartaoEspaco = 12.dp
    val passo = 82.dp
    /** O marcador de grupo da fila das flores (CBD, Infusao, Hash). */
    val separadorLargura = 20.dp
}
