package pt.caramelo.tv.ui.components

/** m:ss, ou h:mm:ss quando passa da hora. Usado no leitor e nos detalhes. */
fun formatarTempo(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
