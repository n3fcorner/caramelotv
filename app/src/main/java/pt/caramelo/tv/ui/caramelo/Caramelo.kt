package pt.caramelo.tv.ui.caramelo

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import pt.caramelo.tv.R

/**
 * O Caramelo, o anfitriao da app (18/11/2026, Francisco: "a aplicacao da TV para ser um Caramelo"). Cada ecra escolhe a
 * pose e o componente troca de loop com um esbatimento curto.
 *
 * Os loops vem do site (os loops com alfa das paginas) passados a WebP animado com transparencia, a 12 fotogramas por
 * segundo (um sim, um nao dos 24 do original) e 480 px de altura (`app/src/main/assets/caramelo/`). O Android nao le o alfa
 * dos WebM VP9 nem dos HEVC do site; o WebP animado le-se com o AnimatedImageDrawable (Android 9+), que descodifica no
 * processador e por isso nao disputa o descodificador de video das flores (a TCL nao aguenta dois videos ao mesmo tempo).
 *
 * Fica o cartaz parado da mesma pose (`res/drawable-nodpi/caramelo_<pose>.webp`) enquanto o loop nao carrega, abaixo do
 * Android 9, nos testes de imagem (que nao correm o descodificador) e quando o ecra pede [parado]: durante as transicoes
 * e enquanto toca o video de uma flor, para nao disputar o processador fraco da TV.
 *
 * As poses de hoje sao as do site; quando chegarem os loops do Caramelo curador (fase 5 do plano), troca-se so o
 * ficheiro de cada pose.
 */
enum class Pose(val ficheiro: String, @DrawableRes val cartaz: Int) {
    /** Parado, a respirar: o Caramelo do hidrante, o da marca. */
    Espera("caramelo", R.drawable.caramelo_caramelo),
    /** Recebe quem chega: a montra a comecar. */
    Acena("acenar", R.drawable.caramelo_acenar),
    /** Apresenta o que esta em foco. */
    Aponta("apontar", R.drawable.caramelo_apontar),
    /** Tem nas maos o que se abriu. */
    Segura("caixa", R.drawable.caramelo_caixa),
    /** Le o boletim (as analises). */
    Lupa("lupa", R.drawable.caramelo_lupa),
    /** Mostra uma peca (a vitrine). */
    Mostra("ups", R.drawable.caramelo_ups),
    /** Espera com paciencia (a carregar, sem rede). */
    Paciencia("espera", R.drawable.caramelo_espera),
    /** Traz uma mensagem (avisos). */
    Carta("carta", R.drawable.caramelo_carta),
}

/** Um ImageLoader so para o Caramelo: o de omissao do Coil nao sabe animar WebP, e as fotos dos produtos nao precisam disso. */
private object CarameloLoader {
    @Volatile private var loader: ImageLoader? = null
    fun de(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .components { add(ImageDecoderDecoder.Factory()) }
            // Os loops vivem no APK: nada de cache em disco, e na memoria so o que esta no ecra.
            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
            .build()
            .also { loader = it }
    }
}

@Composable
fun Caramelo(
    pose: Pose,
    modifier: Modifier = Modifier,
    /** Virado para o outro lado (a apontar para a esquerda, por exemplo). */
    espelhar: Boolean = false,
    alinhamento: Alignment = Alignment.BottomCenter,
    /** Fica no cartaz parado: durante uma transicao ou com um video a tocar. */
    parado: Boolean = false,
) {
    val context = LocalContext.current
    val semAnimacao = parado || LocalInspectionMode.current || Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    val loader = remember { CarameloLoader.de(context) }
    Crossfade(targetState = pose, animationSpec = tween(280), label = "caramelo", modifier = modifier) { p ->
        val espelho = Modifier.fillMaxSize().graphicsLayer { if (espelhar) scaleX = -1f }
        Box(Modifier.fillMaxSize(), contentAlignment = alinhamento) {
            if (semAnimacao) {
                Image(painterResource(p.cartaz), contentDescription = null, contentScale = ContentScale.Fit, alignment = alinhamento, modifier = espelho)
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context).data("file:///android_asset/caramelo/${p.ficheiro}.webp").build(),
                    imageLoader = loader,
                    contentDescription = null,
                    placeholder = painterResource(p.cartaz),
                    contentScale = ContentScale.Fit,
                    alignment = alinhamento,
                    modifier = espelho,
                )
            }
        }
    }
}
