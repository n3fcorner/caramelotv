@file:OptIn(ExperimentalSharedTransitionApi::class)

package pt.caramelo.tv.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Os dois ambitos que uma transicao partilhada precisa. Ficam em CompositionLocals para
 * os mosaicos e o cartaz dos detalhes nao terem de os receber por parametro atraves de
 * meia duzia de composables.
 */
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** A mola de tudo o que voa entre ecras: um so caracter de movimento na app inteira. */
private val VOO = spring<androidx.compose.ui.geometry.Rect>(
    dampingRatio = 0.9f,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * Liga este elemento ao elemento com a mesma [key] no outro ecra: a foto do mosaico
 * voa e cresce ate a posicao do cartaz, e dai ate ao quadrado do video, em vez de
 * haver um corte seco.
 *
 * Sem os ambitos (por exemplo numa preview) devolve o modificador intacto.
 */
@Composable
fun Modifier.hero(key: String): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val anim = LocalNavAnimation.current ?: return this
    return with(shared) {
        this@hero.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = anim,
            boundsTransform = { _, _ -> VOO },
        )
    }
}

/**
 * Como [hero], mas para elementos cujo conteudo muda de um ecra para o outro — um
 * titulo que num ecra e' pequeno e noutro e' grande, um botao que vira cabecalho.
 * A caixa voa e o conteudo velho esbate-se no novo pelo caminho.
 */
@Composable
fun Modifier.heroBounds(key: String): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val anim = LocalNavAnimation.current ?: return this
    return with(shared) {
        this@heroBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = anim,
            boundsTransform = { _, _ -> VOO },
        )
    }
}

/**
 * Faz o conteudo entrar em vez de aparecer: sobe um pouco e ganha opacidade, com um
 * atraso proporcional a [ordem] para uma coluna de cartoes entrar em cascata.
 *
 * Corre so na primeira composicao — ao regressar de outro ecra o conteudo ja esta
 * la, e uma cascata a repetir-se de cada vez que se volta atras seria ruido.
 */
@Composable
fun Entrada(
    ordem: Int = 0,
    modifier: Modifier = Modifier,
    /** Falso desliga a cascata e desenha o conteudo tal como esta. */
    activa: Boolean = true,
    conteudo: @Composable () -> Unit,
) {
    // Fixado na primeira composicao: quem manda a cascata pode mudar de ideias a
    // meio (o menu marca-se como "ja mostrado" mal aparece), e trocar de ramo com
    // a animacao a correr cortava-a ao meio.
    val cascata = remember { activa }
    if (!cascata) {
        androidx.compose.foundation.layout.Box(modifier) { conteudo() }
        return
    }
    val estado = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = estado,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 260, delayMillis = 60 * ordem)) +
            slideInVertically(tween(durationMillis = 320, delayMillis = 60 * ordem)) { it / 5 },
    ) {
        conteudo()
    }
}
