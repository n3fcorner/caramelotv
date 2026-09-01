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
) {
    val isEmpty: Boolean get() = line.isBlank() && format.isBlank()

    val temPerfil: Boolean get() = indica in 0..100
}
