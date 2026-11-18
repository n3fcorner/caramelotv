package pt.caramelo.tv.ui.components

import java.util.Locale

/** m:ss, ou h:mm:ss quando passa da hora. Usado no leitor e nos detalhes. */
fun formatarTempo(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** O preco como se escreve em Portugal: "38,00 €". Vazio sem preco. */
fun precoTxt(preco: Double?): String =
    preco?.let { String.format(Locale("pt", "PT"), "%.2f €", it) } ?: ""

/** O preco curto do rotulo do frasco: "38 €" quando e' redondo, "14,90 €" quando nao. */
fun precoCurto(preco: Double?): String = when {
    preco == null -> ""
    preco % 1.0 == 0.0 -> "${preco.toLong()} €"
    else -> precoTxt(preco)
}
