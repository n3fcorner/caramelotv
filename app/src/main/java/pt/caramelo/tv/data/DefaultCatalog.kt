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
 *   Infusão     (3x3)  5 com video, 3 anunciadas, 1 livre
 *   CBD          (3x2)  4 com video, 2 anunciadas
 *   Hash         (3x1)  2 com video, 1 livre
 *
 * As descricoes falam de formato, cultivo e aroma. Nao afirmam efeitos: isso e
 * materia regulada e tem de sair da propria marca.
 */
object DefaultCatalog {

    private const val LINHA_CBD = "CBD"
    private const val LINHA_INFUSAO = "Infusão"

    fun build(): Catalog = Catalog(items = infusao() + cbd() + hash())

    // --- Infusão -------------------------------------------------------

    private fun infusao(): List<MenuItem> = listOf(
        produto(
            Section.INFUSAO, 0, "Blueberry Small", "blueberry-small",
            linha = LINHA_INFUSAO, formato = "Flor", cultivo = "Indoor", infundido = true,
            aroma = "Mirtilo e frutos silvestres",
            descricao = "Flor de interior infundida, em formato small buds. " +
                "Cabeças pequenas e uniformes, de tom escuro e bem cobertas.",
        ),
        produto(
            Section.INFUSAO, 1, "Bubba Kush", "bubba-kush",
            linha = LINHA_INFUSAO, formato = "Flor", cultivo = "Greenhouse", infundido = true,
            aroma = "Terroso e café, fundo doce",
            descricao = "Flor de estufa infundida. Genética índica clássica, " +
                "de cabeça densa e cor profunda.",
        ),
        produto(
            Section.INFUSAO, 2, "Critical Berry", "critical-berry",
            linha = LINHA_INFUSAO, formato = "Flor", cultivo = "", infundido = true,
            aroma = "Frutos vermelhos sobre fundo terroso",
            descricao = "Flor infundida, da família Critical. Cabeça compacta " +
                "e clara, com pistilos alaranjados.",
        ),
        produto(
            Section.INFUSAO, 3, "Smash Cali", "smash-cali",
            linha = LINHA_INFUSAO, formato = "Flor", cultivo = "Glasshouse", infundido = true,
            aroma = "Cítrico e doce",
            descricao = "Flor de glasshouse infundida. Tom claro e pistilos " +
                "muito vivos, de aspeto californiano.",
        ),
        produto(
            Section.INFUSAO, 4, "Sour Lifter", "sour-lifter",
            linha = LINHA_INFUSAO, formato = "Flor", cultivo = "Glasshouse", infundido = true,
            aroma = "Ácido e cítrico, fundo a diesel",
            descricao = "Flor de glasshouse infundida. Cabeças alongadas, com " +
                "mistura de verdes e castanhos.",
        ),
        emBreve(
            Section.INFUSAO, 5, "MAC",
            linha = LINHA_INFUSAO, aroma = "Cítrico e cremoso, fundo a diesel",
            descricao = "Miracle Alien Cookies infundida. Entra em breve na linha.",
        ),
        emBreve(
            Section.INFUSAO, 6, "Gorilla Mandarini",
            linha = LINHA_INFUSAO, aroma = "Tangerina sobre fundo resinoso",
            descricao = "Cruzamento da família Gorilla com tangerina, infundido. " +
                "Entra em breve na linha.",
        ),
        emBreve(
            Section.INFUSAO, 7, "Berry Pie",
            linha = LINHA_INFUSAO, aroma = "Frutos vermelhos e doce de forno",
            descricao = "Flor infundida, de perfil doce e frutado. " +
                "Entra em breve na linha.",
        ),
        livre(Section.INFUSAO, 8),
    )

    // --- CBD ----------------------------------------------------------------

    private fun cbd(): List<MenuItem> = listOf(
        produto(
            Section.CBD, 0, "Grape Cali", "grape-cali",
            linha = LINHA_CBD, formato = "Flor", cultivo = "Glasshouse",
            aroma = "Uva e frutos escuros, fundo doce",
            descricao = "Flor de CBD cultivada em glasshouse. Cabeças compactas de tom " +
                "escuro com pistilos alaranjados e boa cobertura de tricoma.",
        ),
        produto(
            Section.CBD, 1, "Lemon Trip", "lemon-trip",
            linha = LINHA_CBD, formato = "Flor", cultivo = "Greenhouse",
            aroma = "Limão e casca fresca, fundo herbáceo",
            descricao = "Flor de CBD de estufa. Cabeças alongadas de verde vivo, com " +
                "aroma cítrico marcado assim que se abre o frasco.",
        ),
        produto(
            Section.CBD, 2, "Mela Sour", "mela-sour",
            linha = LINHA_CBD, formato = "Flor", cultivo = "",
            aroma = "Maçã verde e ácido, fundo doce",
            descricao = "Flor de CBD de cabeça grande e bem resinada, com tons " +
                "acastanhados e muito pistilo à vista.",
        ),
        produto(
            Section.CBD, 3, "Solero Cream", "solero-cream",
            linha = LINHA_CBD, formato = "Flor", cultivo = "Indoor",
            aroma = "Cremoso e doce, baunilha e citrinos",
            descricao = "Flor de CBD de interior. Das mais resinadas da linha, de tom " +
                "claro e tricoma denso.",
        ),
        emBreve(
            Section.CBD, 4, "Critical Berry",
            linha = LINHA_CBD, aroma = "Frutos vermelhos sobre fundo terroso",
            descricao = "A Critical Berry em versão CBD, sem infusão. Entra em breve na linha.",
        ),
        emBreve(
            Section.CBD, 5, "Uranium",
            linha = LINHA_CBD, aroma = "Intenso e resinoso",
            descricao = "Flor de CBD a entrar em breve na linha.",
        ),
    )

    // --- Hash ---------------------------------------------------------------

    private fun hash(): List<MenuItem> = listOf(
        produto(
            Section.HASH, 0, "Static", "static",
            linha = LINHA_INFUSAO, formato = "Hash", cultivo = "", infundido = true,
            aroma = "Concentrado e terroso",
            descricao = "Hash infundido, obtido por peneiração estática a seco. " +
                "O vídeo mostra a placa a ser aberta dentro da câmara.",
        ),
        produto(
            Section.HASH, 1, "Sticky Peach", "sticky-peach-piattella",
            linha = LINHA_INFUSAO, formato = "Hash piattella", cultivo = "", infundido = true,
            aroma = "Pêssego doce",
            descricao = "Hash em formato piattella infundido. Pasta clara e " +
                "maleável, cortada à faca.",
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
        descricao: String,
        infundido: Boolean = false,
    ) = MenuItem(
        id = id(section, slot),
        slot = slot,
        section = section,
        title = titulo,
        description = descricao,
        videoUrl = "media/$slug.mp4",
        imageUrl = "posters/$slug.jpg",
        strain = Strain(linha, formato, cultivo, aroma, infundido),
    )

    /** Produto ja anunciado mas ainda sem video: aparece com a etiqueta Em breve. */
    private fun emBreve(
        section: Section,
        slot: Int,
        titulo: String,
        linha: String,
        aroma: String,
        descricao: String,
    ) = MenuItem(
        id = id(section, slot),
        slot = slot,
        section = section,
        title = titulo,
        description = descricao,
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
