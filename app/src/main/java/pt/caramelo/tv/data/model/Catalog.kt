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
) {
    fun section(section: Section): List<MenuItem> =
        items.filter { it.section == section }.sortedBy { it.slot }

    /** Itens da mesma seccao com video, pela ordem do ecra — alimenta o saltar no leitor. */
    fun playableIn(section: Section): List<MenuItem> = section(section).filter { it.hasVideo }

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
}
