package pt.caramelo.tv.data.model

import kotlinx.serialization.Serializable

/**
 * Um artigo de parafernalia na app da TV (25/10/2026, Francisco: "integrar a parafernalia na app da tv,
 * especialmente as piteiras e a sua visualizacao 3d"). Nao e' flor: nao tem genetica, nem terpenos, nem
 * boletim. Vem do CRM (caramelodobrasil.eu/api/tv/catalogo, chave "parafernalia") com a foto, o preco, a
 * categoria e, nas piteiras e kits, o video da peca a dar uma volta inteira ([giro]) e o modelo 3D ([modelo3d]).
 *
 * A app mostra-os numa pagina propria, separada da flor; ao escolher um, a peca aparece grande a rodar em loop.
 */
@Serializable
data class Parafernalia(
    val slug: String = "",
    val nome: String = "",
    val categoria: String = "",
    /** "venda" ou "esgotado", como no CRM. */
    val estado: String = "",
    val preco: Double? = null,
    val imagem: String = "",
    /** O .glb, quando a peca tem modelo (as piteiras, os kits e os artigos modelados). Fica para o futuro; a TV mostra o [giro]. */
    val modelo3d: String? = null,
    /** O video da rotacao, que fecha em loop (public/3d/artigos/giro/<chave>.mp4 no site). So as piteiras e os kits o tem. */
    val giro: String? = null,
) {
    val esgotado: Boolean get() = estado == "esgotado"
    val temGiro: Boolean get() = !giro.isNullOrBlank()
}
