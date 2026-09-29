package pt.caramelo.tv.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pt.caramelo.tv.data.model.MediaOrientation
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.data.model.Strain
import java.net.HttpURLConnection
import java.net.URL

/**
 * O catalogo que vem do CRM (29/9/2026, Francisco: "da para ligar a app da TV ao CRM?
 * E sincronizar?").
 *
 * O CRM (caramelodobrasil.eu/api/tv/catalogo) manda em que flores existem, o nome, a
 * ordem, a foto, o video e o lote. A ficha rica — genetica, perfil indica/sativa,
 * terpenos, aroma em duas linguas — continua a ser a do [DefaultCatalog] quando a
 * flor e' conhecida; uma flor nova, criada no painel, entra com a ficha curta do CRM.
 *
 * Sem rede, ou com o CRM em baixo, fica o ultimo catalogo recebido; sem nenhum, o de
 * fabrica. Nunca um ecra vazio.
 */
object Sincronizacao {

    @Serializable
    data class LoteCrm(val codigo: String = "", val url: String = "")

    @Serializable
    data class ItemCrm(
        val slug: String,
        val nome: String,
        val seccao: String,
        val estado: String = "",
        val preco: Double? = null,
        val infundido: Boolean = false,
        val imagem: String? = null,
        val video: String? = null,
        val lote: LoteCrm? = null,
        val tipo: String = "",
        val cultivo: String = "",
        val aromas: List<String> = emptyList(),
    )

    @Serializable
    data class CatalogoCrm(val itens: List<ItemCrm> = emptyList())

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Vai buscar o catalogo e devolve-o ja canonico (so os itens, sem a hora a que foi
     * gerado): assim so se grava — e so se redesenha o ecra — quando alguma coisa muda
     * de facto. Numa TCL cada recomposicao inteira da app conta.
     */
    fun buscar(endereco: String): String? {
        if (endereco.isBlank()) return null
        val ligacao = (URL(endereco).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (ligacao.responseCode != 200) return null
            val texto = ligacao.inputStream.bufferedReader().use { it.readText() }
            if (texto.length > 1_000_000) return null
            val lido = json.decodeFromString(CatalogoCrm.serializer(), texto)
            if (lido.itens.isEmpty()) null else json.encodeToString(CatalogoCrm.serializer(), lido)
        } catch (e: Exception) {
            null
        } finally {
            ligacao.disconnect()
        }
    }

    fun ler(raw: String?): CatalogoCrm? =
        raw?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(CatalogoCrm.serializer(), it) }.getOrNull() }

    private fun seccaoDe(s: String): Section? = when (s) {
        "cbd" -> Section.CBD
        "infusao" -> Section.INFUSAO
        "hash" -> Section.HASH
        else -> null
    }

    private fun prefixo(s: Section) = when (s) {
        Section.INFUSAO -> "thcp"
        Section.CBD -> "cbd"
        Section.HASH -> "hash"
    }

    /** O slug que o [DefaultCatalog] guarda no caminho do cartaz ("posters/<slug>.jpg"). */
    private fun slugDe(item: MenuItem): String? =
        Regex("""posters/([a-z0-9-]+)\.""").find(item.imageUrl)?.groupValues?.get(1)

    private fun normal(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    /** "Estufa (greenhouse)" passa a "Greenhouse": o ponto de cultivo so conhece os tres nomes. */
    private fun cultivoDe(s: String): String {
        val t = s.lowercase()
        return when {
            "glass" in t -> "Glasshouse"
            "indoor" in t || "interior" in t -> "Indoor"
            "greenhouse" in t || "estufa" in t -> "Greenhouse"
            else -> ""
        }
    }

    /** Os itens da TV a partir do CRM, com a ficha de fabrica por cima quando a flor e' conhecida. */
    fun juntar(crm: CatalogoCrm, fabrica: List<MenuItem>): List<MenuItem> {
        val porSlug = fabrica.mapNotNull { f -> slugDe(f)?.let { it to f } }.toMap()
        val usados = mutableSetOf<String>()
        val porSeccao = mutableMapOf<Section, Int>()
        return crm.itens.mapNotNull { c ->
            val seccao = seccaoDe(c.seccao) ?: return@mapNotNull null
            val base = (porSlug[c.slug] ?: fabrica.firstOrNull { it.section == seccao && normal(it.title) == normal(c.nome) })
                ?.takeIf { it.id !in usados }
            base?.let { usados += it.id }
            val slot = porSeccao.getOrDefault(seccao, 0).also { porSeccao[seccao] = it + 1 }
            MenuItem(
                id = base?.id ?: "${prefixo(seccao)}-${c.slug}",
                slot = slot,
                section = seccao,
                title = c.nome,
                // O video e o cartaz da propria TV quando ela os tem (tocam sem rede);
                // senao os do CRM.
                videoUrl = base?.videoUrl?.takeIf { it.isNotBlank() } ?: c.video.orEmpty(),
                imageUrl = base?.imageUrl?.takeIf { it.isNotBlank() } ?: c.imagem.orEmpty(),
                orientation = base?.orientation ?: MediaOrientation.SQUARE,
                strain = base?.strain ?: Strain(
                    line = if (seccao == Section.INFUSAO || c.infundido) "Infusão" else "CBD",
                    format = if (seccao == Section.HASH) "Hash" else "Flor",
                    cultivation = cultivoDe(c.cultivo),
                    aroma = c.aromas.joinToString(", ").replaceFirstChar { it.uppercase() },
                    infused = seccao == Section.INFUSAO || c.infundido,
                    type = c.tipo,
                ),
            )
        }
    }
}
