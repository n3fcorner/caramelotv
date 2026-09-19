package pt.caramelo.tv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    val items: List<MenuItem> = emptyList(),
    val settings: PanelSettings = PanelSettings(),
    /** Posicao em milissegundos por id de item, para o Retomar. */
    val progress: Map<String, Long> = emptyMap(),
    /** Ultimo item reproduzido, usado pelo botao Continuar a ver. */
    val lastWatchedId: String? = null,
    /**
     * Impressao dos itens de fabrica com que este catalogo foi guardado. Quando o
     * codigo traz itens diferentes (terpenos novos, um aroma corrigido), a
     * impressao ja nao bate certo e o repositorio troca os itens pelos novos,
     * guardando o resto (definicoes, posicoes, ultimo visto).
     */
    val defaultsFingerprint: String = "",
) {
    fun section(section: Section): List<MenuItem> =
        items.filter { it.section == section }.sortedBy { it.slot }

    /** Itens da mesma seccao com video, pela ordem do ecra — alimenta o saltar no leitor. */
    fun playableIn(section: Section): List<MenuItem> = section(section).filter { it.hasVideo }

    /**
     * Para onde o leitor pode ir a seguir a partir de [item]: o seguinte e o anterior
     * na seccao (setas), ou so o seguinte de todo o catalogo na montra automatica.
     * A mesma ordem que MainViewModel.skip e proximoNoAtrator usam.
     */
    fun vizinhosDe(item: MenuItem, atrator: Boolean): List<MenuItem> {
        val lista = if (atrator) items.filter { it.hasVideo } else playableIn(item.section)
        val i = lista.indexOfFirst { it.id == item.id }
        if (i < 0 || lista.size < 2) return emptyList()
        val seguinte = lista[(i + 1).mod(lista.size)]
        return if (atrator) listOf(seguinte) else listOf(seguinte, lista[(i - 1).mod(lista.size)]).distinct()
    }

    fun byId(id: String): MenuItem? = items.firstOrNull { it.id == id }

    fun replace(item: MenuItem): Catalog =
        copy(items = items.map { if (it.id == item.id) item else it })

    fun positionOf(id: String): Long = if (settings.resumePlayback) progress[id] ?: 0L else 0L

    fun lastWatched(): MenuItem? = lastWatchedId?.let { byId(it) }

    fun videoUrlOf(item: MenuItem): String = absolute(item.videoUrl)

    fun imageUrlOf(item: MenuItem): String = absolute(item.imageUrl)

    /** Deixa passar URLs completos; a tudo o resto prefixa a base configurada. */
    private fun absolute(path: String): String = when {
        path.isBlank() -> ""
        path.startsWith("http://") || path.startsWith("https://") -> path
        path.startsWith("file://") || path.startsWith("content://") -> path
        else -> settings.mediaBaseUrl.trimEnd('/') + "/" + path.trimStart('/')
    }

    /** Os boletins de analise, com o caminho ja resolvido contra a base. */
    fun boletins(): List<Boletim> = BOLETINS.map { it.copy(url = absolute(it.url)) }
}

/**
 * Um boletim de analise.
 *
 * Sao **certificados distintos**, um por variedade, e nao paginas de um documento
 * unico — o PDF que o Leo mandou traz os quatro digitalizados em sequencia. Por
 * isso cada um leva o seu nome: na TV, "pagina 3 de 4" nao diz nada a ninguem,
 * "Sour Lifter" diz.
 */
data class Boletim(
    val url: String,
    val titulo: String,
    val laboratorio: String,
)

/**
 * A lista fica escrita e nao sai de uma listagem de pasta: a app le de `file://`
 * na propria TV e nao tem como listar uma pasta remota quando a base e' HTTP. E o
 * nome de cada um so esta dentro da imagem, portanto tinha de ser transcrito na
 * mesma. Os ficheiros vivem em `videos/web/coa` e vao para a TV com os cartazes.
 */
private val BOLETINS = listOf(
    Boletim("coa/coa-1.jpg", "Lemon Kush Pop", "IFHA · Viena"),
    Boletim("coa/coa-2.jpg", "Uranium", "IFHA · Viena"),
    Boletim("coa/coa-3.jpg", "Sour Lifter", "IFHA · Viena"),
    Boletim("coa/coa-4.jpg", "Whyper Pod · Gelato", "Abexan"),
)
