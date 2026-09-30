package pt.caramelo.tv

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.nav.Screen
import pt.caramelo.tv.ui.animation.LocalNavAnimation
import pt.caramelo.tv.ui.animation.LocalSharedTransition
import pt.caramelo.tv.ui.coa.CoaScreen
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.details.DetailsScreen
import pt.caramelo.tv.ui.home.HomeScreen
import pt.caramelo.tv.ui.player.PlayerScreen
import pt.caramelo.tv.ui.settings.EditItemScreen
import pt.caramelo.tv.ui.settings.SettingsScreen
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.i18n.Idioma
import pt.caramelo.tv.ui.i18n.LocalIdioma
import pt.caramelo.tv.ui.i18n.tr

/**
 * Activity unica. Toda a app vive em Compose; a navegacao e a pilha de ecras
 * da [MainViewModel], o que mantem o controlo do foco do D-pad do nosso lado.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Montra: o painel nao pode adormecer enquanto a app estiver a frente.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // So na criacao a serio. A activity e' singleTask, e o Android volta a
        // entregar o intent que criou a tarefa sempre que a recria — um
        // `caramelotv://coa` mandado uma vez por adb ficava a abrir o boletim em
        // cada arranque, por cima do menu, ate a tarefa morrer. Tratado uma vez,
        // o link e' consumido para nao voltar a contar.
        if (savedInstanceState == null) consumirDeepLink(intent)

        setContent {
            CarameloTheme {
                CarameloRoot(viewModel = viewModel)
            }
        }
    }

    /** A activity e singleTask: os deep links de configuracao chegam por aqui. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumirDeepLink(intent)
    }

    /**
     * Trata o deep link e apaga-o do intent, para que uma recriacao da activity
     * nao o volte a executar. O que fica guardado como intent da tarefa e' a
     * abertura normal, sem dados.
     */
    private fun consumirDeepLink(intent: Intent?) {
        val dados = intent?.data ?: return
        viewModel.handleDeepLink(dados)
        setIntent(Intent(intent).setData(null))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CarameloRoot(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()

    // Back recua na pilha e, na raiz, nao faz nada: numa montra ninguem deve sair
    // sem querer. Para sair mesmo, o botao Home do comando continua a funcionar.
    BackHandler { viewModel.pop() }

    // O arranque tambem e' uma transicao: o ecra de carregamento esbate-se no menu
    // em vez de ser trocado a seco quando o catalogo chega.
    //
    // O estado do Crossfade e' SO "ja carregou?", nunca o catalogo em si. Com o
    // catalogo como estado, cada gravacao (posicao do clipe, ultimo visto, uma
    // definicao) esbatia a app INTEIRA numa copia nova: dois leitores a descodificar
    // ao mesmo tempo em cada mudanca de erva, e a arvore velha ficava retida pelo
    // observador das transicoes partilhadas. Era isso que punha a TV de rastos.
    // A lingua desce por um CompositionLocal: todos os ecras a leem sem a receber por parametro.
    CompositionLocalProvider(LocalIdioma provides Idioma.de(catalog?.settings?.idioma)) {
    Crossfade(
        targetState = catalog != null,
        animationSpec = tween(400),
        label = "arranque",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { carregado ->
        val actual = catalog
        if (!carregado || actual == null) {
            LoadingScreen()
        } else {
            Navegacao(viewModel = viewModel, catalog = actual)
        }
    }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Navegacao(viewModel: MainViewModel, catalog: Catalog) {
    val loaded = catalog
    // Dois leitores com produtos diferentes sao o MESMO ecra para a raiz: a mudanca
    // de erva anima-se la dentro, com o quadrado do video e a ficha a deslizar, e
    // nao com o ecra inteiro a piscar. Por isso a raiz nem ve o produto: recebe um
    // marcador fixo, e o leitor le o produto actual por si. (Um contentKey igual
    // nao chegava: o AnimatedContent continuava a tratar a mudanca como transicao
    // e recompunha o leitor de raiz duas vezes por erva, com ExoPlayer e vista de
    // video novos de cada vez — e o que saia ficava retido.)
    val ecraActual = viewModel.current
    var ultimoLeitor by remember { mutableStateOf(ecraActual as? Screen.Player) }
    if (ecraActual is Screen.Player) ultimoLeitor = ecraActual
    val rota: Any = if (ecraActual is Screen.Player) Rota.Leitor else ecraActual
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            val partilha = this
            AnimatedContent(
                targetState = rota,
                transitionSpec = {
                    // Direccional: entrar empurra o ecra velho para a esquerda e traz
                    // o novo da direita; voltar faz o inverso. A foto partilhada voa
                    // por cima disto tudo. Sem escala: o hero ja da a sensacao de
                    // profundidade, e escala mais deslize era movimento a mais.
                    val sentido = viewModel.sentidoNav
                    (fadeIn(tween(320)) + slideInHorizontally(tween(360)) { largura -> sentido * largura / 14 })
                        .togetherWith(
                            fadeOut(tween(220)) + slideOutHorizontally(tween(360)) { largura -> -sentido * largura / 14 },
                        )
                },
                label = "navegacao",
            ) { alvo ->
                val animacao = this
                // O leitor a sair (a raiz ja esta no menu) continua a mostrar o
                // ultimo produto ate a transicao acabar.
                val screen = if (alvo === Rota.Leitor) ultimoLeitor ?: Screen.Home else alvo as Screen
                CompositionLocalProvider(
                    LocalSharedTransition provides partilha,
                    LocalNavAnimation provides animacao,
                ) {
                    ScreenContent(screen = screen, catalog = loaded, viewModel = viewModel)
                }
            }
        }
    }
}

/** O que a raiz anima: cada ecra por si, e um so marcador para todos os leitores. */
private object Rota {
    object Leitor
}

@Composable
private fun ScreenContent(screen: Screen, catalog: Catalog, viewModel: MainViewModel) {
    when (screen) {
        Screen.Home -> HomeScreen(
            catalog = catalog,
            focusedId = viewModel.focusedId,
            onFocusItem = { viewModel.onFocused(it.id) },
            onOpenItem = { item ->
                if (catalog.settings.autoplay && item.hasVideo) viewModel.play(item, resume = true)
                else viewModel.openDetails(item)
            },
            onEditItem = { viewModel.push(Screen.EditItem(it.id)) },
            onContinue = { catalog.lastWatched()?.let { viewModel.play(it, resume = true) } },
            onSettings = { viewModel.push(Screen.Settings) },
            onMontra = viewModel::iniciarAtrator,
            onCoa = { viewModel.push(Screen.Coa) },
            onInteracao = viewModel::registarInteracao,
            onIdioma = { viewModel.updateSettings { it.copy(idioma = if (it.idioma == "en") "pt" else "en") } },
            cascata = !viewModel.menuJaMostrado,
            onMostrado = { viewModel.menuJaMostrado = true },
        )

        Screen.Coa -> CoaScreen(
            boletins = catalog.boletins(),
            onBack = { viewModel.pop() },
        )

        is Screen.Details -> WithItem(catalog, screen.id, viewModel) { item ->
            DetailsScreen(
                item = item,
                posterUrl = catalog.imageUrlOf(item),
                resumePositionMs = catalog.positionOf(item.id),
                onPlay = { resume -> viewModel.play(item, resume) },
                onEdit = { viewModel.push(Screen.EditItem(item.id)) },
                onBack = { viewModel.pop() },
            )
        }

        is Screen.Player -> WithItem(catalog, screen.id, viewModel) { item ->
            PlayerScreen(
                item = item,
                videoUrlOf = { catalog.videoUrlOf(it) },
                posterUrlOf = { catalog.imageUrlOf(it) },
                startPositionMs = screen.startPositionMs,
                loop = catalog.settings.loopPlayback,
                atrator = screen.atrator,
                sentido = viewModel.sentidoProduto,
                onExit = { id, position, finished ->
                    viewModel.savePosition(id, position, finished)
                },
                onSkip = { delta -> viewModel.skip(item, delta) },
                onFimDoAtrator = { viewModel.proximoNoAtrator(item) },
                onControlo = viewModel::assumirControlo,
                cartazesVizinhos = catalog.vizinhosDe(item, screen.atrator).map { catalog.imageUrlOf(it) },
            )
        }

        Screen.Settings -> SettingsScreen(
            catalog = catalog,
            onEditItem = { viewModel.push(Screen.EditItem(it.id)) },
            // A base dos ficheiros que o ecra mostra e' a resolvida (a pasta da propria TV quando esta
            // vazia); guarda-se a que estava gravada, para "vazio" continuar a querer dizer "da TV".
            onSettingsChange = { updated -> viewModel.updateSettings { atual -> updated.copy(mediaBaseUrl = if (updated.mediaBaseUrl == viewModel.catalog.value?.settings?.mediaBaseUrl) atual.mediaBaseUrl else updated.mediaBaseUrl) } },
            onClearProgress = viewModel::clearProgress,
            onResetCatalog = viewModel::resetCatalog,
            onBack = { viewModel.pop() },
        )

        is Screen.EditItem -> WithItem(catalog, screen.id, viewModel) { item ->
            EditItemScreen(
                item = item,
                onSave = {
                    viewModel.saveItem(it)
                    viewModel.pop()
                },
                onCancel = { viewModel.pop() },
            )
        }
    }
}

/** Resolve o item pelo id e recua se ele tiver desaparecido (ex.: depois de repor o catalogo). */
@Composable
private fun WithItem(
    catalog: Catalog,
    id: String,
    viewModel: MainViewModel,
    content: @Composable (MenuItem) -> Unit,
) {
    val item = catalog.byId(id)
    if (item == null) {
        LaunchedEffect(id) { viewModel.pop() }
        LoadingScreen()
    } else {
        content(item)
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(tr("A carregar...", "Loading..."), style = MaterialTheme.typography.titleMedium)
    }
}
