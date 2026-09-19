package pt.caramelo.tv.nav

sealed interface Screen {
    data object Home : Screen
    data class Details(val id: String) : Screen
    data class Player(
        val id: String,
        val startPositionMs: Long,
        /** Verdadeiro quando a montra se poe a andar sozinha, sem ninguem no comando. */
        val atrator: Boolean = false,
    ) : Screen
    /** O boletim de analise, em imagem: a TV nao abre PDF. */
    data object Coa : Screen
    data object Settings : Screen
    data class EditItem(val id: String) : Screen
}
