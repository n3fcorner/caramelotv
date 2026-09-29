package pt.caramelo.tv.ui.coa

import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import pt.caramelo.tv.data.model.Boletim
import pt.caramelo.tv.ui.animation.heroBounds
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Espaco
import pt.caramelo.tv.ui.theme.Overscan
import pt.caramelo.tv.ui.theme.RoxoProfundo

/**
 * O boletim de analise, pagina a pagina, com lupa.
 *
 * A TV nao abre PDF — nenhuma das aplicacoes instaladas se oferece para o fazer, os
 * dois browsers incluidos. As paginas vem ja convertidas em imagem, ao lado dos
 * cartazes, a 2400px de largura: a pagina inteira num ecra de 1080 mostra a tabela
 * mas as notas de rodape ficam ilegiveis, e e' preciso haver pixeis para ampliar.
 *
 * Comando:
 *   OK            1x -> 1,5x -> 2x -> 3x -> 1x
 *   setas         a 1x mudam de boletim; ampliado, deslocam a folha — e deslocam
 *                 de seguida enquanto a seta ficar carregada (repeticao do KeyDown)
 *   Voltar        ampliado volta a 1x; a 1x sai
 *
 * O deslocamento trata-se no KeyDown para apanhar a repeticao automatica do
 * comando: carregar e manter arrasta a folha, em vez de a empurrar aos saltos.
 * Mudar de boletim e ampliar tratam-se no KeyUp, para nao disparar em cadeia.
 */
/** Os degraus da lupa. O 1,5x e' o que chega para a tabela sem perder a folha. */
private val NIVEIS = listOf(1f, 1.5f, 2f, 3f)

@Composable
fun CoaScreen(
    boletins: List<Boletim>,
    onBack: () -> Unit,
) {
    var pagina by remember { mutableIntStateOf(0) }
    var ampliacao by remember { mutableFloatStateOf(1f) }
    var deslocX by remember { mutableFloatStateOf(0f) }
    var deslocY by remember { mutableFloatStateOf(0f) }
    val foco = remember { FocusRequester() }
    val contexto = LocalContext.current
    LaunchedEffect(Unit) { foco.requestFocus() }

    val escala by animateFloatAsState(ampliacao, tween(160), label = "ampliacao")
    val x by animateFloatAsState(deslocX, tween(120), label = "deslocX")
    val y by animateFloatAsState(deslocY, tween(120), label = "deslocY")

    /** Nao deixa a folha sair do ecra: o limite cresce com a ampliacao. */
    fun limita() {
        val folga = 520f * (ampliacao - 1f)
        deslocX = deslocX.coerceIn(-folga, folga)
        deslocY = deslocY.coerceIn(-folga * 1.4f, folga * 1.4f)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(RoxoProfundo)
            .clipToBounds()
            .focusRequester(foco)
            .focusable()
            .onKeyEvent { evento ->
                if (evento.type != KeyEventType.KeyUp) return@onKeyEvent false
                val passo = 180f
                when (evento.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        ampliacao = if (ampliacao >= 3f) 1f else ampliacao + 1f
                        if (ampliacao == 1f) { deslocX = 0f; deslocY = 0f } else limita()
                        true
                    }
                    Key.DirectionRight -> {
                        if (ampliacao > 1f) { deslocX -= passo; limita() }
                        else if (pagina < boletins.lastIndex) pagina++
                        true
                    }
                    Key.DirectionLeft -> {
                        if (ampliacao > 1f) { deslocX += passo; limita() }
                        else if (pagina > 0) pagina--
                        true
                    }
                    Key.DirectionDown -> {
                        if (ampliacao > 1f) { deslocY -= passo; limita() }
                        else if (pagina < boletins.lastIndex) pagina++
                        true
                    }
                    Key.DirectionUp -> {
                        if (ampliacao > 1f) { deslocY += passo; limita() }
                        else if (pagina > 0) pagina--
                        true
                    }
                    Key.Back -> {
                        if (ampliacao > 1f) { ampliacao = 1f; deslocX = 0f; deslocY = 0f }
                        else onBack()
                        true
                    }
                    else -> false
                }
            },
    ) {
        if (boletins.isNotEmpty()) {
            // Mudar de boletim e' uma folha a dar lugar a outra, nao uma troca a seco:
            // a nova entra a deslizar de leve no sentido da seta enquanto a velha se
            // esbate.
            AnimatedContent(
                targetState = pagina,
                transitionSpec = {
                    val sentido = if (targetState >= initialState) 1 else -1
                    (fadeIn(tween(240)) + slideInHorizontally(tween(320)) { it / 12 * sentido })
                        .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(320)) { -it / 12 * sentido })
                },
                label = "boletim",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        translationX = x
                        translationY = y
                    },
            ) { indice ->
                AsyncImage(
                    // Size.ORIGINAL: sem isto a Coil reduz a folha ao tamanho do ecra e
                    // ampliar so aumentava pixeis ja deitados fora.
                    model = ImageRequest.Builder(contexto)
                        .data(boletins[indice].url)
                        .size(Size.ORIGINAL)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Boletim de análise · ${boletins[indice].titulo}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Overscan.vertical, bottom = Espaco.seccao),
                )
            }
        }

        // O rotulo do ecra, no canto: e' a mesma caixa que o botao "Analises" do
        // menu, que cresce ate aqui.
        Text(
            text = tr("ANÁLISES", "LAB REPORTS"),
            style = MaterialTheme.typography.labelMedium,
            color = Amarelo,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = Overscan.horizontal, vertical = Espaco.medio)
                .heroBounds("ecra-analises")
                .background(RoxoProfundo.copy(alpha = 0.7f))
                .padding(horizontal = Espaco.curto, vertical = Espaco.minimo),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(RoxoProfundo.copy(alpha = 0.86f))
                .padding(horizontal = Overscan.horizontal, vertical = Espaco.curto),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaco.minimo),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.curto)) {
                boletins.indices.forEach { i ->
                    Box(
                        Modifier
                            .background(if (i == pagina) Amarelo else Creme.copy(alpha = 0.30f))
                            .padding(horizontal = if (i == pagina) 14.dp else 8.dp, vertical = 2.dp),
                    ) {}
                }
            }
            Text(
                text = boletins[pagina].titulo.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Creme,
                letterSpacing = 2.sp,
            )
            Text(
                text = if (ampliacao > 1f) {
                    "${boletins[pagina].laboratorio}  ·  ${"%.1f".format(ampliacao).trimEnd('0').trimEnd('.')}×  ·  setas deslocam  ·  OK amplia  ·  Voltar reduz"
                } else {
                    "${boletins[pagina].laboratorio}  ·  ${pagina + 1} de ${boletins.size}  ·  ◀ ▶ muda de boletim  ·  OK amplia"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Creme.copy(alpha = 0.72f),
                letterSpacing = 1.sp,
            )
        }
    }
}
