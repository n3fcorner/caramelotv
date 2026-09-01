package pt.caramelo.tv.data

import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.data.model.Strain

/**
 * Semente do catalogo: as duas linhas da marca mais o hash.
 *
 * Os caminhos sao relativos — o prefixo vem de [PanelSettings.mediaBaseUrl], para
 * mudar de alojamento ser uma edicao so em vez de dezasseis.
 *
 *   Infusão (3x3)  5 com video, 3 anunciadas, 1 livre
 *   CBD     (3x2)  4 com video, 2 anunciadas
 *   Hash    (3x1)  2 com video, 1 livre
 *
 * A ficha diz o que se ve e se cheira — formato, cultivo, aroma. Nao ha texto
 * corrido nem nome de composto: e materia regulada e tem de sair da marca.
 */
object DefaultCatalog {

    private const val LINHA_CBD = "CBD"
    private const val LINHA_INFUSAO = "Infusão"

    fun build(): Catalog = Catalog(items = infusao() + cbd() + hash())

    private fun infusao(): List<MenuItem> = listOf(
        produto(
            Section.INFUSAO, 0, "Blueberry Small", "blueberry-small",
            LINHA_INFUSAO, "Flor", "Indoor", "Mirtilo e frutos silvestres", infundido = true,
        ),
        produto(
            Section.INFUSAO, 1, "Bubba Kush", "bubba-kush",
            LINHA_INFUSAO, "Flor", "Greenhouse", "Terroso e café, fundo doce", infundido = true,
        ),
        produto(
            Section.INFUSAO, 2, "Critical Berry", "critical-berry",
            LINHA_INFUSAO, "Flor", "", "Frutos vermelhos sobre fundo terroso", infundido = true,
        ),
        produto(
            Section.INFUSAO, 3, "Smash Cali", "smash-cali",
            LINHA_INFUSAO, "Flor", "Glasshouse", "Cítrico e doce", infundido = true,
        ),
        produto(
            Section.INFUSAO, 4, "Sour Lifter", "sour-lifter",
            LINHA_INFUSAO, "Flor", "Glasshouse", "Ácido e cítrico, fundo a diesel", infundido = true,
        ),
        emBreve(Section.INFUSAO, 5, "MAC", LINHA_INFUSAO, "Cítrico e cremoso, fundo a diesel", "mac"),
        emBreve(Section.INFUSAO, 6, "Gorilla Mandarini", LINHA_INFUSAO, "Tangerina sobre fundo resinoso", "gorilla-mandarini"),
        emBreve(Section.INFUSAO, 7, "Berry Pie", LINHA_INFUSAO, "Frutos vermelhos e doce de forno", "berry-pie"),
        livre(Section.INFUSAO, 8),
    )

    private fun cbd(): List<MenuItem> = listOf(
        produto(
            Section.CBD, 0, "Grape Cali", "grape-cali",
            LINHA_CBD, "Flor", "Glasshouse", "Uva e frutos escuros, fundo doce",
        ),
        produto(
            Section.CBD, 1, "Lemon Trip", "lemon-trip",
            LINHA_CBD, "Flor", "Greenhouse", "Limão e casca fresca, fundo herbáceo",
        ),
        produto(
            Section.CBD, 2, "Mela Sour", "mela-sour",
            LINHA_CBD, "Flor", "", "Maçã verde e ácido, fundo doce",
        ),
        produto(
            Section.CBD, 3, "Solero Cream", "solero-cream",
            LINHA_CBD, "Flor", "Indoor", "Cremoso e doce, baunilha e citrinos",
        ),
        emBreve(Section.CBD, 4, "Critical Berry", LINHA_CBD, "Frutos vermelhos sobre fundo terroso", "critical-berry"),
        emBreve(Section.CBD, 5, "Uranium", LINHA_CBD, "Intenso e resinoso", "uranium"),
    )

    private fun hash(): List<MenuItem> = listOf(
        produto(
            Section.HASH, 0, "Static", "static",
            LINHA_INFUSAO, "Hash", "", "Concentrado e terroso", infundido = true,
        ),
        produto(
            Section.HASH, 1, "Sticky Peach", "sticky-peach-piattella",
            LINHA_INFUSAO, "Hash piattella", "", "Pêssego doce", infundido = true,
        ),
        livre(Section.HASH, 2),
    )

    // --- construtores -------------------------------------------------------

    private fun produto(
        section: Section,
        slot: Int,
        titulo: String,
        slug: String,
        linha: String,
        formato: String,
        cultivo: String,
        aroma: String,
        infundido: Boolean = false,
    ) = MenuItem(
        id = id(section, slot),
        slot = slot,
        section = section,
        title = titulo,
        videoUrl = "media/$slug.mp4",
        imageUrl = "posters/$slug.jpg",
        strain = Strain(linha, formato, cultivo, aroma, infundido),
    )

    /**
     * Produto ja anunciado mas ainda sem video: aparece com a etiqueta Em breve.
     * Leva cartaz na mesma — a foto de estudio do fornecedor chega para o mostrar.
     */
    private fun emBreve(
        section: Section,
        slot: Int,
        titulo: String,
        linha: String,
        aroma: String,
        slug: String = "",
    ) = MenuItem(
        id = id(section, slot),
        slot = slot,
        section = section,
        title = titulo,
        imageUrl = if (slug.isBlank()) "" else "posters/$slug.jpg",
        strain = Strain(
            line = linha,
            format = "Flor",
            aroma = aroma,
            infused = linha == LINHA_INFUSAO,
        ),
    )

    private fun livre(section: Section, slot: Int) = MenuItem(
        id = id(section, slot),
        slot = slot,
        section = section,
    )

    private fun id(section: Section, slot: Int) = when (section) {
        Section.INFUSAO -> "thcp-$slot"
        Section.CBD -> "cbd-$slot"
        Section.HASH -> "hash-$slot"
    }
}
