package pt.caramelo.tv.data.model

import kotlinx.serialization.Serializable

/**
 * Ficha do produto. So descreve o que se ve e se cheira: linha, formato, cultivo e
 * aroma. Nao guarda efeitos nem alegacoes de saude, que sao materia regulada e teriam
 * de vir da propria marca.
 */
@Serializable
data class Strain(
    /** Linha de produto: "CBD" ou "Infusão". */
    val line: String = "",
    /** "Flor" ou "Hash". */
    val format: String = "",
    /** "Indoor", "Greenhouse", "Glasshouse" — vazio quando o video nao o indica. */
    val cultivation: String = "",
    val aroma: String = "",
    val infused: Boolean = false,
) {
    val isEmpty: Boolean get() = line.isBlank() && format.isBlank()
}
