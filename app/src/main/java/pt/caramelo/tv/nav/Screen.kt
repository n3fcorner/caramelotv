package pt.caramelo.tv.nav

sealed interface Screen {
    data object Home : Screen
    data class Details(val id: String) : Screen
    data class Player(val id: String, val startPositionMs: Long) : Screen
    data object Settings : Screen
    data class EditItem(val id: String) : Screen
}
