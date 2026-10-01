package pt.caramelo.tv.data.model

import kotlinx.serialization.Serializable

/**
 * As tres seccoes do ecra inicial. Sao as duas linhas comerciais mais o hash, que
 * ganha montra propria: sao produtos artesanais, visualmente distintos das cabecas
 * verdes, e no meio de oito flores passavam despercebidos.
 */
enum class Section(val label: String, val columns: Int, val slots: Int) {
    INFUSAO("Infusão", 4, 12),
    CBD("CBD", 4, 6),
    HASH("Hash", 4, 6),
}

/**
 * Formato do media. Os clipes da marca sao todos recortados a quadrado — e o que
 * apaga as marcas de agua dos canais e o que enquadra bem num ecra 16:9, deixando
 * uma coluna limpa para a ficha do produto.
 */
enum class MediaOrientation {
    SQUARE,
    PORTRAIT,
    LANDSCAPE,
    ;

    val aspect: Float
        get() = when (this) {
            SQUARE -> 1f
            PORTRAIT -> 9f / 16f
            LANDSCAPE -> 16f / 9f
        }
}

/** Em que pe esta o slot. Derivado, para nao haver um campo a dessincronizar. */
enum class ItemState { PRONTO, EM_BREVE, LIVRE }

@Serializable
data class MenuItem(
    val id: String,
    /** Posicao dentro da seccao, comecando em 0. */
    val slot: Int,
    val section: Section,
    val title: String = "",
    val videoUrl: String = "",
    val imageUrl: String = "",
    val orientation: MediaOrientation = MediaOrientation.SQUARE,
    val strain: Strain = Strain(),
    /** Sem stock no CRM (1/10/2026): o mosaico leva o carimbo SOLD OUT e vai para o fim da fila. */
    val esgotado: Boolean = false,
) {
    val hasVideo: Boolean get() = videoUrl.isNotBlank()

    val state: ItemState
        get() = when {
            hasVideo -> ItemState.PRONTO
            title.isNotBlank() -> ItemState.EM_BREVE
            else -> ItemState.LIVRE
        }

    /** Nome a mostrar, incluindo nos slots ainda por preencher. */
    val displayTitle: String get() = title.ifBlank { "Slot livre" }
}
