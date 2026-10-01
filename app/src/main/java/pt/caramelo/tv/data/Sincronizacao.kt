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
 * flor e' conhecida, com o que se escreveu na ficha do produto no painel por cima
 * (campo a campo); uma flor nova entra com a ficha que o painel tiver.
 *
 * Sem rede, ou com o CRM em baixo, fica o ultimo catalogo recebido; sem nenhum, o de
 * fabrica. Nunca um ecra vazio.
 */
object Sincronizacao {

    @Serializable
    data class LoteCrm(val codigo: String = "", val url: String = "")

    @Serializable
    data class TerpenoCrm(val nome: String = "", val pct: Double? = null)

    /** O que foi escrito na ficha do produto no painel do CRM: ganha a ficha de fabrica, campo a campo (29/9/2026). */
    @Serializable
    data class PainelCrm(
        val tipo: String = "",
        val cultivo: String = "",
        val genetica: String = "",
        val indica: Int? = null,
        val aromas: List<String> = emptyList(),
        val terpenos: List<TerpenoCrm> = emptyList(),
    )

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
        val genetica: String = "",
        val indica: Int? = null,
        val terpenos: List<TerpenoCrm> = emptyList(),
        val painel: PainelCrm? = null,
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

    private val SLUG_CARTAZ = Regex("""(?:posters/|img/produtos/)([a-z0-9-]+?)(?:-tv)?\.(?:jpg|webp)""")
    private val NAO_ALFANUM = Regex("[^a-z0-9]")

    /** O slug que o [DefaultCatalog] guarda no caminho do cartaz ("img/produtos/<slug>-tv.webp", antes "posters/<slug>.jpg"). */
    private fun slugDe(item: MenuItem): String? = SLUG_CARTAZ.find(item.imageUrl)?.groupValues?.get(1)

    private fun normal(s: String) = s.lowercase().replace(NAO_ALFANUM, "")

    /** "Estufa (greenhouse)" passa a "Greenhouse": o desenho do cultivo conhece estes quatro nomes. */
    private fun cultivoDe(s: String): String {
        val t = s.lowercase()
        return when {
            "glass" in t -> "Glasshouse"
            "indoor" in t || "interior" in t -> "Indoor"
            "greenhouse" in t || "estufa" in t -> "Greenhouse"
            "outdoor" in t || "exterior" in t -> "Outdoor"
            else -> ""
        }
    }

    private fun aromaDe(l: List<String>) = l.joinToString(", ").replaceFirstChar { it.uppercase() }

    /** "mirceno" passa a "Mirceno": o dicionario de traducoes da TV conhece os nomes com maiuscula. */
    private fun nomeTerpeno(s: String) = s.trim().replaceFirstChar { it.uppercase() }

    /**
     * As percentagens so contam quando todos os terpenos as tem; senao fica so a ordem de dominancia.
     * Mostram-se como parte do perfil (a soma e' 100), que e' o que o grafico diz ("% dos terpenos"):
     * assim tanto faz o painel ter recebido 35/20 ou os 0,8/0,4 da massa do boletim, e um 0,4 nao
     * arredonda para 0 e desfaz o grafico.
     */
    private fun pcts(l: List<TerpenoCrm>): List<Int> {
        if (l.isEmpty() || l.any { it.pct == null || it.pct <= 0.0 }) return emptyList()
        val soma = l.sumOf { it.pct!! }
        return l.map { maxOf(1, Math.round(it.pct!! / soma * 100).toInt()) }
    }

    /** A ficha de fabrica com o que o painel preencheu por cima. */
    private fun comPainel(s: Strain, p: PainelCrm?): Strain {
        if (p == null) return s
        return s.copy(
            type = p.tipo.ifBlank { s.type },
            cultivation = if (p.cultivo.isNotBlank()) cultivoDe(p.cultivo) else s.cultivation,
            genetics = p.genetica.ifBlank { s.genetics },
            // Um tipo novo no painel sem indica escrita deixa de valer o numero de fabrica (que era do
            // tipo antigo): passa a estimado pelo tipo, e o ecra diz que e' estimado.
            indica = p.indica?.takeIf { it in 0..100 } ?: if (p.tipo.isNotBlank()) -1 else s.indica,
            aroma = if (p.aromas.isNotEmpty()) aromaDe(p.aromas) else s.aroma,
            terpenes = if (p.terpenos.isNotEmpty()) p.terpenos.map { nomeTerpeno(it.nome) } else s.terpenes,
            terpenesPct = if (p.terpenos.isNotEmpty()) pcts(p.terpenos) else s.terpenesPct,
        )
    }

    /** Os itens da TV a partir do CRM, com a ficha de fabrica por cima quando a flor e' conhecida. */
    fun juntar(crm: CatalogoCrm, fabrica: List<MenuItem>): List<MenuItem> {
        val porSlug = fabrica.mapNotNull { f -> slugDe(f)?.let { it to f } }.toMap()
        val usados = mutableSetOf<String>()
        val porSeccao = mutableMapOf<Section, Int>()
        // Os esgotados vao para o fim de cada seccao, pela ordem do painel (1/10/2026): a frente da fila e' o que ha.
        return crm.itens.sortedBy { if (it.estado == "esgotado") 1 else 0 }.mapNotNull { c ->
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
                // O video do site primeiro (1/10/2026, "na TV nem tem fotos nem videos"): os ficheiros da propria TV
                // vivem na pasta interna da app, que se perde a cada desinstalacao (a troca de assinatura obriga a uma);
                // o do CRM esta sempre no site. O da TV fica so para a flor que o site ainda nao tem.
                videoUrl = c.video?.takeIf { it.isNotBlank() } ?: base?.videoUrl.orEmpty(),
                // O recorte do site primeiro (1/10/2026): a lista e a ficha sao agora papel bege, e o recorte
                // pousa nele como na loja; o cartaz da TV, com a margem roxa, so quando o CRM nao tem foto.
                imageUrl = c.imagem?.takeIf { it.isNotBlank() } ?: base?.imageUrl.orEmpty(),
                esgotado = c.estado == "esgotado",
                orientation = base?.orientation ?: MediaOrientation.SQUARE,
                // A linha (CBD ou infundida) vem sempre do CRM, nunca da ficha de fabrica: e' o que manda
                // na zona de venda, e uma etiqueta errada e' o erro mais caro (CLAUDE.md §1).
                strain = base?.strain?.let { comPainel(it, c.painel).copy(infused = seccao == Section.INFUSAO || c.infundido, line = if (seccao == Section.INFUSAO || c.infundido) "Infusão" else "CBD") } ?: Strain(
                    line = if (seccao == Section.INFUSAO || c.infundido) "Infusão" else "CBD",
                    format = if (seccao == Section.HASH) "Hash" else "Flor",
                    cultivation = cultivoDe(c.cultivo),
                    aroma = aromaDe(c.aromas),
                    infused = seccao == Section.INFUSAO || c.infundido,
                    genetics = c.genetica,
                    type = c.tipo,
                    indica = c.indica?.takeIf { it in 0..100 } ?: -1,
                    terpenes = c.terpenos.map { nomeTerpeno(it.nome) },
                    terpenesPct = pcts(c.terpenos),
                ),
            )
        }
    }
}
