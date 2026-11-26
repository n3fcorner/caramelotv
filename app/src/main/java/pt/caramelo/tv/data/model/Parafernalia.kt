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
    /**
     * As fotos reais de cada cor (18/11/2026, Francisco: "nao quero usar modelos 3D, apenas as fotos reais dos artigos de
     * parafernalia"): o ecra inicial troca-as no pedestal, uma a uma, com o nome da cor.
     */
    val fotos: List<FotoDeCor> = emptyList(),
    /** A ficha tecnica da loja (artigos.json), em portugues: material, medidas, dentes... */
    val ficha: List<Especificacao> = emptyList(),
    /**
     * Nas piteiras e nos kits: a arte animada da piteira com o giro ja dentro da janela, num video so (a TV so aguenta um
     * video de cada vez). Feito por scripts/cena-tv.py; [cartaz] e' o primeiro fotograma.
     */
    val cena: String? = null,
    val cartaz: String? = null,
) {
    val esgotado: Boolean get() = estado == "esgotado"
    val temGiro: Boolean get() = !giro.isNullOrBlank()
    val temCena: Boolean get() = !cena.isNullOrBlank()
}

/** Uma cor de um artigo, com a foto real dela. [cor] vazio quando o artigo so tem uma. */
@Serializable
data class FotoDeCor(val cor: String = "", val imagem: String = "", val estado: String = "venda")

/** Uma linha da ficha tecnica: "Material" / "Aluminio anodizado". */
@Serializable
data class Especificacao(val rotulo: String = "", val valor: String = "")
