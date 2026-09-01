@file:OptIn(ExperimentalSharedTransitionApi::class)

package pt.caramelo.tv.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Os dois ambitos que uma transicao partilhada precisa. Ficam em CompositionLocals para
 * os mosaicos e o cartaz dos detalhes nao terem de os receber por parametro atraves de
 * meia duzia de composables.
 */
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Liga este elemento ao elemento com a mesma [key] no outro ecra: a foto do mosaico
 * voa e cresce ate a posicao do cartaz em vez de haver um corte seco.
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
            boundsTransform = { _, _ ->
                spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
            },
        )
    }
}
