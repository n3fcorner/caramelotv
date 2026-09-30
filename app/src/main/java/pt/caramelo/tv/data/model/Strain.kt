package pt.caramelo.tv.data.model

import kotlinx.serialization.Serializable

/**
 * Ficha do produto: o que se ve, o que se cheira e o que se sabe da genetica.
 *
 * Nao guarda percentagens de canabinoides nem efeitos. Os primeiros so viriam de
 * boletins de analise do lote, os segundos sao materia regulada — e nenhum dos
 * dois se inventa.
 */
@Serializable
data class Strain(
    /** Linha de produto: "CBD" ou "Infusão". */
    val line: String = "",
    /** "Flor" ou "Hash". */
    val format: String = "",
    /** "Indoor", "Greenhouse", "Glasshouse" — vazio quando nao esta documentado. */
    val cultivation: String = "",
    val aroma: String = "",
    val infused: Boolean = false,
    /** Cruzamento conhecido, quando existe. Os nomes comerciais raramente o publicam. */
    val genetics: String = "",
    /** "Índica", "Sativa" ou "Híbrida". Vazio quando nao se sabe. */
    val type: String = "",
    /**
     * Peso da indica de 0 a 100, para a barra do ecra de detalhes.
     * Negativo quer dizer desconhecido — e melhor nao mostrar barra nenhuma do que
     * mostrar uma inventada.
     */
    val indica: Int = -1,
    /** Terpenos dominantes descritos para a genetica. */
    val terpenes: List<String> = emptyList(),
    /**
     * Percentagem de cada terpeno no perfil publicado pelo produtor, na mesma ordem de
     * [terpenes]. Vazio quando so se conhece a ordem de dominancia (29/9/2026).
     */
    val terpenesPct: List<Int> = emptyList(),
) {
    val isEmpty: Boolean get() = line.isBlank() && format.isBlank()

    /**
     * O peso da indica a desenhar (29/9/2026, pedido do Leo: "grafico sativa/indica em
     * todas"). O documentado quando existe; senao estimado pelo tipo, e o ecra diz que
     * e' estimado. Sem tipo nenhum continua a nao haver barra.
     */
    val indicaEfectiva: Int get() = if (indica in 0..100) indica else indicaDoTipo(type)

    val perfilEstimado: Boolean get() = indica !in 0..100 && indicaEfectiva in 0..100

    val temPerfil: Boolean get() = indicaEfectiva in 0..100

    companion object {
        fun indicaDoTipo(tipo: String): Int {
            val t = tipo.lowercase()
            return when {
                "índica-dominante" in t || "indica-dominante" in t || "indica dominant" in t -> 65
                "sativa-dominante" in t || "sativa dominant" in t -> 35
                "híbrida" in t || "hibrida" in t || "hybrid" in t -> 50
                "índica" in t || "indica" in t -> 75
                "sativa" in t -> 25
                else -> -1
            }
        }
    }
}
