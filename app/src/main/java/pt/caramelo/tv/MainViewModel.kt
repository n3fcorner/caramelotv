package pt.caramelo.tv

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.caramelo.tv.data.CatalogRepository
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.PanelSettings
import pt.caramelo.tv.nav.Screen

class MainViewModel(private val repo: CatalogRepository) : ViewModel() {

    val catalog: StateFlow<Catalog?> =
        repo.catalog.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Pilha de ecras propria em vez de Navigation-Compose. Sao cinco ecras e assim
     * o foco do D-pad fica sob o nosso controlo quando se volta atras.
     */
    private val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()

    /** Item destacado na grelha. Alimenta o painel e a restauracao de foco. */
    var focusedId by mutableStateOf<String?>(null)
        private set

    var exitDialogVisible by mutableStateOf(false)
        private set

    fun onFocused(id: String) {
        focusedId = id
    }

    fun push(screen: Screen) {
        stack.add(screen)
    }

    /** Devolve false quando ja estamos na raiz e nao ha para onde recuar. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popTo(screen: Screen) {
        while (stack.size > 1 && stack.last() != screen) stack.removeAt(stack.lastIndex)
    }

    fun showExitDialog(visible: Boolean) {
        exitDialogVisible = visible
    }

    // --- Acoes de conteudo -------------------------------------------------

    fun openDetails(item: MenuItem) {
        focusedId = item.id
        push(Screen.Details(item.id))
    }

    fun play(item: MenuItem, resume: Boolean) {
        if (!item.hasVideo) return
        val start = if (resume) catalog.value?.positionOf(item.id) ?: 0L else 0L
        focusedId = item.id
        push(Screen.Player(item.id, start))
    }

    /**
     * Cima/baixo no leitor: passa ao produto anterior ou seguinte com video dentro da
     * mesma seccao, substituindo o topo da pilha para o Back continuar a dar na home.
     */
    fun skip(current: MenuItem, delta: Int) {
        val catalogo = catalog.value ?: return
        val lista = catalogo.playableIn(current.section)
        if (lista.size < 2) return
        val indice = lista.indexOfFirst { it.id == current.id }
        if (indice < 0) return
        val proximo = lista[(indice + delta + lista.size) % lista.size]
        focusedId = proximo.id
        replaceTop(Screen.Player(proximo.id, 0L))
    }

    private fun replaceTop(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun savePosition(id: String, positionMs: Long, finished: Boolean) {
        viewModelScope.launch {
            repo.update { catalog ->
                val progress = catalog.progress.toMutableMap()
                if (finished || positionMs < RESUME_THRESHOLD_MS) progress.remove(id)
                else progress[id] = positionMs
                catalog.copy(progress = progress, lastWatchedId = id)
            }
        }
    }

    fun saveItem(item: MenuItem) {
        viewModelScope.launch { repo.update { it.replace(item) } }
    }

    fun updateSettings(transform: (PanelSettings) -> PanelSettings) {
        viewModelScope.launch { repo.update { it.copy(settings = transform(it.settings)) } }
    }

    fun clearProgress() {
        viewModelScope.launch { repo.update { it.copy(progress = emptyMap(), lastWatchedId = null) } }
    }

    fun resetCatalog() {
        viewModelScope.launch {
            repo.reset()
            popTo(Screen.Home)
        }
    }

    // --- Deep links (configuracao por ADB) ---------------------------------

    /**
     * caramelotv://set?id=thcp-3&title=...&desc=...&url=...&image=...
     * caramelotv://play?id=cbd-0
     * caramelotv://base?url=http://172.20.10.6:8000
     * caramelotv://reset
     *
     * `id` e o identificador do slot: thcp-0..8, cbd-0..5, hash-0..2.
     */
    fun handleDeepLink(uri: Uri) {
        when (uri.host) {
            "reset" -> resetCatalog()

            // caramelotv://base?url=http://192.168.1.216:8000
            "base" -> {
                val url = uri.getQueryParameter("url") ?: return
                updateSettings { it.copy(mediaBaseUrl = url) }
            }

            "play" -> {
                val id = uri.getQueryParameter("id") ?: return
                viewModelScope.launch {
                    val item = catalog.value?.byId(id) ?: return@launch
                    popTo(Screen.Home)
                    play(item, resume = false)
                }
            }

            "set" -> {
                val id = uri.getQueryParameter("id") ?: return
                viewModelScope.launch {
                    repo.update { catalog ->
                        val item = catalog.byId(id) ?: return@update catalog
                        catalog.replace(
                            item.copy(
                                title = uri.getQueryParameter("title") ?: item.title,
                                description = uri.getQueryParameter("desc") ?: item.description,
                                videoUrl = uri.getQueryParameter("url") ?: item.videoUrl,
                                imageUrl = uri.getQueryParameter("image") ?: item.imageUrl,
                            ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** Abaixo disto nao vale a pena oferecer Retomar. */
        const val RESUME_THRESHOLD_MS = 10_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CarameloApp
                MainViewModel(app.repository)
            }
        }
    }
}
