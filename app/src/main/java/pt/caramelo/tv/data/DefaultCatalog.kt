package pt.caramelo.tv.data

import pt.caramelo.tv.data.model.Catalog
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.data.model.Strain

/**
 * Semente do catalogo: as duas linhas da marca mais o hash.
 *
 * Os caminhos sao relativos — o prefixo vem de [PanelSettings.mediaBaseUrl], para
 * mudar de alojamento ser uma edicao so em vez de dezasseis.
 *
 *   Infusão (4x3)  11 com video, 1 anunciada (a MAC)
 *   CBD     (2x3)  6 com video
 *   Hash    (2x3)  6 com video
 *
 * Desde setembro de 2026 nao ha slots livres: as dezoito posicoes estao todas
 * ocupadas. O construtor de slot vazio saiu daqui por deixar de ter chamador —
 * o estado [ItemState.LIVRE] e o cartaz posters/livre.jpg ficam, que um item sem
 * titulo continua a cair nele. A proxima flor tem de ir para algum lado, e e a
 * grelha que vai ter de crescer.
 *
 * A ficha diz o que se ve e se cheira — formato, cultivo, aroma. Nao ha texto
 * corrido nem nome de composto: e materia regulada e tem de sair da marca.
 */
object DefaultCatalog {

    private const val LINHA_CBD = "CBD"
    private const val LINHA_INFUSAO = "Infusão"

    fun build(): Catalog = Catalog(items = itens(), defaultsFingerprint = fingerprint())

    private fun itens(): List<MenuItem> = infusao() + cbd() + hash()

    /**
     * Impressao dos itens tal como estao no codigo. Muda sozinha sempre que se
     * mexe num produto — nao ha numero de versao para alguem se esquecer de subir.
     */
    fun fingerprint(): String =
        Json.encodeToString(ListSerializer(MenuItem.serializer()), itens()).hashCode().toUInt().toString(16)

    private fun infusao(): List<MenuItem> = listOf(
        produto(
            Section.INFUSAO, 0, "Blueberry Small", "blueberry-small",
            LINHA_INFUSAO, "Flor", "Indoor", "Mirtilo e frutos silvestres", infundido = true,
            genetica = "Blueberry de DJ Short", tipo = "Índica", indica = 80,
            terpenos = listOf("Mirceno", "Pineno", "Cariofileno"),
        ),
        produto(
            Section.INFUSAO, 1, "Bubba Kush", "bubba-kush",
            LINHA_INFUSAO, "Flor", "Greenhouse", "Terroso e café, fundo doce", infundido = true,
            genetica = "OG Kush × Northern Lights (atribuída)", tipo = "Índica", indica = 80,
            terpenos = listOf("Mirceno", "Cariofileno", "Limoneno"),
        ),
        produto(
            Section.INFUSAO, 2, "Critical Berry", "critical-berry",
            LINHA_INFUSAO, "Flor", "", "Frutos vermelhos sobre fundo terroso", infundido = true,
            genetica = "Família Critical", tipo = "Híbrida", indica = 65,
            terpenos = listOf("Mirceno", "Cariofileno", "Limoneno"),
        ),
        produto(
            Section.INFUSAO, 3, "Smash Cali", "smash-cali",
            LINHA_INFUSAO, "Flor", "Glasshouse", "Cítrico e doce", infundido = true,
            tipo = "Híbrida", indica = 50,
            terpenos = listOf("Limoneno", "Cariofileno", "Mirceno"),
        ),
        produto(
            Section.INFUSAO, 4, "Sour Lifter", "sour-lifter",
            LINHA_INFUSAO, "Flor", "Glasshouse", "Ácido e cítrico, fundo a diesel", infundido = true,
            genetica = "Suver Haze #50 × Gorilla Glue #4", tipo = "Sativa", indica = 30,
            terpenos = listOf("Mirceno", "Terpineol", "Limoneno"),
        ),
        emBreve(
            Section.INFUSAO, 5, "MAC", LINHA_INFUSAO, "Cítrico e cremoso, fundo a diesel", "mac",
            genetica = "Alien Cookies × Miracle 15", tipo = "Híbrida", indica = 50,
            terpenos = listOf("Limoneno", "Cariofileno", "Pineno"),
        ),
        produto(
            Section.INFUSAO, 7, "Berry Pie", "berry-pie",
            LINHA_INFUSAO, "Flor", "Glasshouse", "Frutos vermelhos e doce de forno", infundido = true,
            genetica = "Cookies × Blueberry (atribuída)", tipo = "Híbrida", indica = 60,
            terpenos = listOf("Cariofileno", "Mirceno", "Limoneno"),
        ),
        produto(
            Section.INFUSAO, 8, "White Banger", "white-banger",
            LINHA_INFUSAO, "Flor", "Indoor", "Terroso e a pinho, fundo resinoso", infundido = true,
            tipo = "Híbrida", indica = 50,
            terpenos = listOf("Pineno", "Mirceno", "Cariofileno"),
        ),
        produto(
            Section.INFUSAO, 9, "Pulsar", "pulsar",
            LINHA_INFUSAO, "Flor", "Indoor", "Doce e resinoso, fundo terroso", infundido = true,
            tipo = "Híbrida", indica = 55,
            terpenos = listOf("Mirceno", "Cariofileno", "Limoneno"),
        ),
        produto(
            Section.INFUSAO, 10, "Peanut Butter", "peanut-butter",
            LINHA_INFUSAO, "Flor", "Greenhouse", "Amendoim torrado, fundo doce", infundido = true,
            genetica = "Do-Si-Dos × Mendo Breath (atribuída)", tipo = "Índica", indica = 70,
            terpenos = listOf("Cariofileno", "Limoneno", "Mirceno"),
        ),
        produto(
            Section.INFUSAO, 11, "Sky", "sky",
            LINHA_INFUSAO, "Flor", "Indoor", "Fresco e floral", infundido = true,
            tipo = "Híbrida", indica = 45,
            terpenos = listOf("Terpinoleno", "Linalol", "Pineno"),
        ),
    )

    private fun cbd(): List<MenuItem> = listOf(
        produto(
            Section.CBD, 0, "Grape Cali", "grape-cali",
            LINHA_CBD, "Flor", "Glasshouse", "Uva e frutos escuros, fundo doce",
            tipo = "Híbrida", indica = 60,
            // Perfil publicado pela WeNeed: mirceno 30, limoneno 28, cariofileno 20, linalol 17, pineno 5.
            terpenos = listOf("Mirceno", "Limoneno", "Cariofileno", "Linalol", "Pineno"),
            terpenosPct = listOf(30, 28, 20, 17, 5),
        ),
        produto(
            Section.CBD, 1, "Lemon Trip", "lemon-trip",
            LINHA_CBD, "Flor", "Greenhouse", "Limão e casca fresca, fundo herbáceo",
            tipo = "Sativa", indica = 35,
            terpenos = listOf("Limoneno", "Terpinoleno", "Pineno"),
        ),
        produto(
            Section.CBD, 2, "Mela Sour", "mela-sour",
            LINHA_CBD, "Flor", "Indoor", "Maçã verde e ácido, fundo doce",
            // WeNeed: selecção Doweedo com mãe Tangie, cultivo RDWC indoor; perfil
            // publicado: mirceno 65, limoneno 14, pineno 11, linalol 5, cariofileno 5.
            genetica = "Descendente de Tangie (selecção Doweedo)", tipo = "Sativa", indica = 35,
            terpenos = listOf("Mirceno", "Limoneno", "Pineno", "Linalol", "Cariofileno"),
            terpenosPct = listOf(65, 14, 11, 5, 5),
        ),
        produto(
            Section.CBD, 3, "Solero Cream", "solero-cream",
            LINHA_CBD, "Flor", "Indoor", "Cremoso e doce, baunilha e citrinos",
            // Perfil publicado pela WeNeed: mirceno 39, cariofileno 24, pineno 17, linalol 10, limoneno 9.
            tipo = "Híbrida", indica = 50,
            terpenos = listOf("Mirceno", "Cariofileno", "Pineno", "Linalol", "Limoneno"),
            terpenosPct = listOf(39, 24, 17, 10, 9),
        ),
        produto(
            Section.CBD, 4, "Critical Berry", "critical-berry-cbd",
            LINHA_CBD, "Flor", "Greenhouse", "Frutos vermelhos sobre fundo terroso",
            genetica = "Família Critical", tipo = "Híbrida", indica = 65,
            terpenos = listOf("Mirceno", "Cariofileno", "Limoneno"),
        ),
        produto(
            Section.CBD, 5, "Uranium", "uranium",
            LINHA_CBD, "Flor", "", "Resinoso",
            tipo = "Híbrida", indica = 55,
            terpenos = listOf("Mirceno", "Cariofileno", "Humuleno"),
        ),
    )

    private fun hash(): List<MenuItem> = listOf(
        produto(
            Section.HASH, 0, "Static", "static",
            // Na WeNeed chama-se Static Mango: o aroma e' manga, nao terra.
            LINHA_INFUSAO, "Hash", "", "Manga fresca", infundido = true,
            tipo = "Híbrida", indica = 50,
            terpenos = listOf("Mirceno", "Ocimeno", "Limoneno"),
        ),
        produto(
            Section.HASH, 1, "Sticky Peach", "sticky-peach-piattella",
            LINHA_INFUSAO, "Hash piattella", "", "Pêssego doce", infundido = true,
            tipo = "Híbrida", indica = 55,
            terpenos = listOf("Limoneno", "Linalol", "Cariofileno"),
        ),
        produto(
            Section.HASH, 2, "Mango Melt", "mango-melt",
            LINHA_INFUSAO, "Hash piattella", "", "Manga madura e resina", infundido = true,
            tipo = "Híbrida", indica = 50,
            terpenos = listOf("Mirceno", "Ocimeno", "Limoneno"),
        ),
        produto(
            Section.HASH, 3, "Papaya OG", "papaya-og",
            LINHA_INFUSAO, "Hash", "", "Papaia madura, fundo resinoso", infundido = true,
            genetica = "Papaya × OG Kush (atribuída)", tipo = "Índica", indica = 70,
            terpenos = listOf("Mirceno", "Cariofileno", "Limoneno"),
        ),
        produto(
            Section.HASH, 4, "La Mousse", "la-mousse",
            LINHA_INFUSAO, "Hash", "", "Doce e cremoso, fundo terroso", infundido = true,
            tipo = "Híbrida", indica = 60,
            terpenos = listOf("Cariofileno", "Linalol", "Mirceno"),
        ),
        produto(
            Section.HASH, 5, "OG Ivory Dry", "og-ivory-dry",
            LINHA_INFUSAO, "Hash", "", "Seco e especiado, fundo a pinho", infundido = true,
            genetica = "Família OG (atribuída)", tipo = "Índica", indica = 70,
            terpenos = listOf("Mirceno", "Limoneno", "Cariofileno"),
        ),
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
        genetica: String = "",
        tipo: String = "",
        indica: Int = -1,
        terpenos: List<String> = emptyList(),
        terpenosPct: List<Int> = emptyList(),
    ) = MenuItem(
        id = id(section, slot),
        slot = slot,
        section = section,
        title = titulo,
        videoUrl = if (temVideo(slug)) "media/$slug.mp4" else "",
        imageUrl = "posters/$slug.jpg",
        strain = Strain(linha, formato, cultivo, aroma, infundido, genetica, tipo, indica, terpenos, terpenosPct),
    )

    /** Nem todos os produtos com foto ja tem clipe filmado. */
    private fun temVideo(slug: String) = slug !in SEM_VIDEO

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
        genetica: String = "",
        tipo: String = "",
        indica: Int = -1,
        terpenos: List<String> = emptyList(),
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
            genetics = genetica,
            type = tipo,
            indica = indica,
            terpenes = terpenos,
        ),
    )

    /** Todos tem clipe menos a MAC, que ainda nao foi filmada. */
    private val SEM_VIDEO = emptySet<String>()

    private fun id(section: Section, slot: Int) = when (section) {
        Section.INFUSAO -> "thcp-$slot"
        Section.CBD -> "cbd-$slot"
        Section.HASH -> "hash-$slot"
    }
}
