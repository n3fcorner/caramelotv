package pt.caramelo.tv

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.caramelo.tv.data.CatalogRepository
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
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

    fun onFocused(id: String) {
        focusedId = id
        registarInteracao()
    }

    // --- Modo atrator -------------------------------------------------------

    private var ultimaInteracao = System.currentTimeMillis()

    /** Qualquer toque no comando adia a montra automatica. */
    fun registarInteracao() {
        ultimaInteracao = System.currentTimeMillis()
    }

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val segundos = catalog.value?.settings?.attractSeconds ?: 0
                if (segundos <= 0) continue
                // Ja esta a andar sozinha: nada a fazer.
                val ecra = current
                if (ecra is Screen.Player && ecra.atrator) continue

                // **Ha ecras onde silencio nao e' inactividade.** Quem esta a ler
                // um boletim de analise ou a mexer nas definicoes fica minutos sem
                // carregar em nada, e a montra arrancava-lhe por cima. Nas
                // definicoes era pior do que incomodo: expulsava a pessoa antes de
                // ela chegar ao interruptor que desliga a propria montra.
                if (ecra is Screen.Coa || ecra is Screen.Settings || ecra is Screen.EditItem) continue
                if (System.currentTimeMillis() - ultimaInteracao <= segundos * 1_000L) continue

                // O comando esta parado. Onde quer que a app tenha ficado — num
                // detalhe, nas definicoes — volta ao menu e comeca a montra. Sem
                // isto, bastava alguem deixar a app num submenu para ela ficar
                // parada o resto do dia.
                if (ecra != Screen.Home) popTo(Screen.Home)
                iniciarAtrator()
            }
        }
    }

    /** Arranca a montra automatica ja, sem esperar pelo tempo de inactividade. */
    fun iniciarAtrator() {
        // popTo mexe na pilha mas nao conta como interaccao humana.
        val primeiro = catalog.value?.items?.firstOrNull { it.hasVideo } ?: return
        focusedId = primeiro.id
        push(Screen.Player(primeiro.id, 0L, atrator = true))
    }

    /**
     * No modo atrator o passo e sobre o catalogo inteiro, para a montra mostrar tudo.
     * Quando e alguem no comando, [skip] fica dentro da seccao.
     */
    fun proximoNoAtrator(actual: MenuItem) {
        val lista = catalog.value?.items?.filter { it.hasVideo }.orEmpty()
        if (lista.isEmpty()) return
        val i = lista.indexOfFirst { it.id == actual.id }
        val proximo = lista[(i + 1).mod(lista.size)]
        focusedId = proximo.id
        sentidoProduto = 1
        replaceTop(Screen.Player(proximo.id, 0L, atrator = true))
    }

    /** Alguem pegou no comando durante a montra automatica: passa a sessao normal. */
    fun assumirControlo() {
        registarInteracao()
        val ecra = current
        if (ecra is Screen.Player && ecra.atrator) {
            replaceTop(ecra.copy(atrator = false))
        }
    }

    // --- Sentido do movimento -----------------------------------------------
    //
    // As transicoes sao direccionais: entrar num ecra empurra para a esquerda,
    // voltar puxa para a direita; no leitor, o produto seguinte sobe e o
    // anterior desce. A raiz e o leitor leem estes dois valores para saber para
    // que lado animar. Sao +1 ou -1 e nada mais.

    /** +1 quando se entra num ecra (push), -1 quando se volta (pop). */
    var sentidoNav by mutableIntStateOf(1)
        private set

    /** +1 quando o leitor passa ao produto seguinte, -1 ao anterior. */
    var sentidoProduto by mutableIntStateOf(1)
        private set

    /**
     * O menu ja apareceu uma vez nesta sessao. A cascata dos mosaicos so corre na
     * primeira: ao voltar de um produto, a foto que vem a voar tem de encontrar o
     * mosaico parado no sitio, e nao a meio de uma entrada.
     */
    var menuJaMostrado by mutableStateOf(false)


    fun push(screen: Screen) {
        sentidoNav = 1
        stack.add(screen)
        registarInteracao()
    }

    /** Devolve false quando ja estamos na raiz e nao ha para onde recuar. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        sentidoNav = -1
        stack.removeAt(stack.lastIndex)
        registarInteracao()
        return true
    }

    fun popTo(screen: Screen) {
        if (stack.size > 1 && stack.last() != screen) sentidoNav = -1
        while (stack.size > 1 && stack.last() != screen) stack.removeAt(stack.lastIndex)
    }

    /** Guarda o instante do ultimo toque no comando, para o modo atrator. */

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
        sentidoProduto = if (delta >= 0) 1 else -1
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
     * caramelotv://set?id=thcp-3&title=...&url=...&image=...
     * caramelotv://play?id=cbd-0
     * caramelotv://base?url=http://172.20.10.6:8000
     * caramelotv://reset
     * caramelotv://coa
     *
     * `id` e o identificador do slot: thcp-0..11, cbd-0..5, hash-0..5.
     */
    fun handleDeepLink(uri: Uri) {
        when (uri.host) {
            "reset" -> resetCatalog()

            // Abre o boletim de analise sem ter de o procurar no cabecalho.
            "coa" -> {
                popTo(Screen.Home)
                push(Screen.Coa)
            }

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
