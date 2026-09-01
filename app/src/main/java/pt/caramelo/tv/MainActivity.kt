package pt.caramelo.tv

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.details.DetailsScreen
import pt.caramelo.tv.ui.home.HomeScreen
import pt.caramelo.tv.ui.player.PlayerScreen
import pt.caramelo.tv.ui.settings.EditItemScreen
import pt.caramelo.tv.ui.settings.SettingsScreen
import pt.caramelo.tv.ui.theme.CarameloTheme

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
        intent?.data?.let(viewModel::handleDeepLink)

        setContent {
            CarameloTheme {
                CarameloRoot(viewModel = viewModel)
            }
        }
    }

    /** A activity e singleTask: os deep links de configuracao chegam por aqui. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::handleDeepLink)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CarameloRoot(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val loaded = catalog ?: run {
        LoadingScreen()
        return
    }

    // Back recua na pilha e, na raiz, nao faz nada: numa montra ninguem deve sair
    // sem querer. Para sair mesmo, o botao Home do comando continua a funcionar.
    BackHandler { viewModel.pop() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            val partilha = this
            AnimatedContent(
                targetState = viewModel.current,
                transitionSpec = {
                    // Entra a crescer de leve, sai a afastar-se: o corte deixa de ser seco
                    // e a foto que voa entre ecras tem tempo de se ler.
                    (fadeIn(tween(280)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280)))
                        .togetherWith(
                            fadeOut(tween(200)) + scaleOut(targetScale = 1.03f, animationSpec = tween(200)),
                        )
                },
                label = "navegacao",
            ) { screen ->
                val animacao = this
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
            onInteracao = viewModel::registarInteracao,
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
                videoUrl = catalog.videoUrlOf(item),
                startPositionMs = screen.startPositionMs,
                loop = catalog.settings.loopPlayback,
                atrator = screen.atrator,
                onExit = { position, finished ->
                    viewModel.savePosition(item.id, position, finished)
                },
                onSkip = { delta -> viewModel.skip(item, delta) },
                onFimDoAtrator = { viewModel.proximoNoAtrator(item) },
                onControlo = viewModel::assumirControlo,
            )
        }

        Screen.Settings -> SettingsScreen(
            catalog = catalog,
            onEditItem = { viewModel.push(Screen.EditItem(it.id)) },
            onSettingsChange = { updated -> viewModel.updateSettings { updated } },
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
        Text("A carregar...", style = MaterialTheme.typography.titleMedium)
    }
}
