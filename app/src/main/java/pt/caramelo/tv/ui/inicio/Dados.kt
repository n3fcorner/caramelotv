package pt.caramelo.tv.ui.inicio

import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.Especificacao
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.data.model.Section

/** As tres filas do ecra inicial, ao mesmo nivel (18/11/2026, Francisco: "tudo, tudo animado", com as piteiras e a parafernalia). */
enum class Seccao { FLORES, PITEIRAS, PARAFERNALIA }

/** O que esta em foco numa fila: uma flor (um frasco) ou um artigo (piteira, kit, peca). */
sealed interface Peca { val chave: String }
data class PecaFlor(val item: MenuItem) : Peca { override val chave = "f:" + item.id }
data class PecaArtigo(val artigo: Parafernalia) : Peca { override val chave = "p:" + artigo.slug }

/** Contas puras do ecra inicial (testadas no JVM, em InicioTest). */
object Inicio {
    /** A ordem dos grupos da fila das flores: o CBD (a linha de exportacao) primeiro, a infusao, o hash. */
    val ORDEM = listOf(Section.CBD, Section.INFUSAO, Section.HASH)

    fun flores(c: Catalog): List<MenuItem> = ORDEM.flatMap { c.section(it) }

    /** As piteiras de vidro e os kits (as que tem cena); as de papel ficam com a parafernalia. */
    fun ehPiteira(p: Parafernalia) = p.slug.startsWith("kit-piteiras-") || p.slug.startsWith("piteira-vidro-")

    fun piteiras(c: Catalog): List<Parafernalia> = c.parafernalia.filter(::ehPiteira)

    fun parafernalia(c: Catalog): List<Parafernalia> = c.parafernalia.filterNot(::ehPiteira)

    /** "Indica-dominante, com infusao aromatica" fica "Indica-dominante": o chip da seccao ja diz infusao. */
    fun tipoSemInfusao(t: String): String =
        t.replace(Regex(",?\\s*com infus[aã]o arom[aá]tica", RegexOption.IGNORE_CASE), "").trim()
            .replaceFirstChar { it.uppercaseChar() }

    /** O diametro de uma piteira solta, do fim do slug: "piteira-vidro-fauna-brasileira-6mm" da "6". */
    fun mmDe(slug: String): String? = Regex("(\\d+)mm$").find(slug)?.groupValues?.get(1)

    private val CATEGORIA = mapOf(
        "grinders" to "GRINDER", "cuias" to "CUIA", "cinzeiros" to "CINZEIRO", "isqueiros" to "ISQUEIRO",
        "bandejas" to "BANDEJA", "laminas" to "LÂMINA", "papel" to "PAPEL", "piteiras" to "PITEIRAS",
    )

    /** A linha do olho de um artigo: o que e' e, havendo mais de uma, quantas cores. */
    fun olhoDe(p: Parafernalia): String {
        val mm = mmDe(p.slug)
        return when {
            p.slug.startsWith("kit-piteiras-") -> "KIT DE 3 PITEIRAS"
            p.slug.startsWith("piteira-vidro-") -> "PITEIRA DE VIDRO" + (mm?.let { " · $it mm" } ?: "")
            else -> (CATEGORIA[p.categoria] ?: p.categoria.uppercase()) + if (p.fotos.size > 1) " · ${p.fotos.size} CORES" else ""
        }
    }

    /** A linha do olho com a cor que esta no pedestal: "GRINDER · AZUL METALIZADO". */
    fun olhoComCor(p: Parafernalia, cor: String): String =
        if (cor.isBlank() || p.fotos.size < 2) olhoDe(p) else olhoDe(p).substringBefore(" · ") + " · " + corLimpa(p, cor).uppercase()

    /** O que vai por baixo do preco na etiqueta. */
    fun etiquetaDe(p: Parafernalia): String = when {
        p.slug.startsWith("kit-piteiras-") -> "O KIT"
        p.slug.startsWith("piteira-vidro-") -> "UMA PITEIRA"
        p.fotos.size > 1 -> "${p.fotos.size} CORES"
        else -> ""
    }

    /**
     * O nome grande da ficha: sem o que ja esta noutro sitio ("Piteira de vidro" esta no olho, o diametro tambem, e o
     * material esta no tipo). "Grinder Premium aluminio anodizado" fica "Grinder Premium".
     */
    fun nomeDe(p: Parafernalia): String {
        var n = p.nome.trim()
            .replace(Regex("^Piteira de vidro\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^Kit Piteiras de Vidro\\s+", RegexOption.IGNORE_CASE), "Kit ")
            .replace(Regex("\\s+\\d+\\s*mm$", RegexOption.IGNORE_CASE), "")
        val tipo = tipoDe(p)
        if (tipo.isNotBlank() && n.endsWith(tipo, ignoreCase = true) && n.length > tipo.length + 3) {
            // sem o material e sem a preposicao que o ligava: "Cinzeiro de aluminio anodizado" fica "Cinzeiro", nao "Cinzeiro de"
            n = n.dropLast(tipo.length).trim().replace(Regex("\\s+(de|do|da|em|com)$", RegexOption.IGNORE_CASE), "")
        }
        return n
    }

    private fun chave(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("[^a-z0-9]"), "")

    /**
     * A cor de uma foto, sem o nome da peca la dentro (22/11/2026). O painel manda a cor como o nome da variante, e em
     * tres artigos esse nome repete o do produto: "Cinzeiro de aluminio Rosa", "Grinder de aluminio 4 partes Boladao Azul",
     * "Porta-Joint Bio cru". Tiram-se as palavras que ja estao no nome ou no slug da peca (e as preposicoes que sobram
     * a frente); se nao sobrar nada, fica a cor como veio.
     */
    fun corLimpa(p: Parafernalia, cor: String): String {
        val partes = (p.nome.split(Regex("[\\s-]+")) + p.slug.split('-')).map(::chave).filter { it.isNotBlank() }
        val conhecidas = (partes + partes.zipWithNext { a, b -> a + b }).toSet()
        val resto = cor.trim().split(Regex("\\s+")).filter { chave(it) !in conhecidas }
            .dropWhile { it.lowercase() in setOf("de", "do", "da", "em", "com") }
        return resto.joinToString(" ").ifBlank { cor.trim() }
    }

    /** O tipo de um artigo (a linha amarela por baixo do nome): o material, quando a ficha o tem. */
    fun tipoDe(p: Parafernalia): String =
        p.ficha.firstOrNull { it.rotulo.equals("Material", ignoreCase = true) }?.valor
            ?: p.ficha.firstOrNull()?.valor.orEmpty()

    /** As quatro caixas da ficha tecnica: o resto da ficha (sem o material, que ja esta no tipo) e, se faltar, as cores. */
    fun especificacoesDe(p: Parafernalia): List<Especificacao> {
        val tipo = tipoDe(p)
        val mm = if (p.slug.startsWith("piteira-vidro-")) mmDe(p.slug) else null
        val resto = p.ficha.filterNot { it.valor == tipo && it.rotulo.equals("Material", ignoreCase = true) }
            // a ficha da familia das soltas lista os tres diametros; a piteira em foco so tem o seu
            .map { if (mm != null && it.rotulo.startsWith("Diâmetro", ignoreCase = true)) Especificacao("Diâmetro", "$mm mm") else it }
            .toMutableList()
        if (resto.size < 4 && p.fotos.size > 1 && resto.none { it.rotulo.equals("Cores", ignoreCase = true) })
            resto += Especificacao("Cores", "${p.fotos.size} cores")
        return resto.take(4)
    }

    /** Os terpenos com o comprimento da barra: pelas percentagens quando as ha todas, senao 1, 0,72 e 0,5. */
    fun terpenosDe(nomes: List<String>, pct: List<Int>): List<Pair<String, Float>> {
        val n = nomes.take(3)
        if (pct.size >= n.size && n.isNotEmpty() && pct.take(n.size).all { it > 0 }) {
            val max = pct.take(n.size).max().toFloat()
            return n.mapIndexed { i, t -> t to (pct[i] / max) }
        }
        val fixos = listOf(1f, 0.72f, 0.5f)
        return n.mapIndexed { i, t -> t to fixos[i] }
    }

    /**
     * Quanto a fila desliza para o foco nunca passar da terceira peca a contar do fim, nem ficar antes da margem
     * (como na maqueta). [offset] e' onde a peca comeca na janela da fila, [contentor] a largura dela.
     */
    fun deslize(offset: Float, tamanho: Float, contentor: Float, margem: Float, passo: Float): Float {
        val limite = contentor - margem - 3 * passo
        return when {
            offset < margem -> offset - margem
            offset > limite -> offset - limite
            else -> 0f
        }.let { if (offset + tamanho > contentor && it == 0f) offset + tamanho - contentor else it }
    }
}
