package pt.caramelo.tv

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pt.caramelo.tv.data.DefaultCatalog
import pt.caramelo.tv.data.Sincronizacao
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.ui.animation.LocalNavAnimation
import pt.caramelo.tv.ui.animation.LocalSharedTransition
import pt.caramelo.tv.ui.caramelo.LocalCarameloFalso
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.coa.CoaScreen
import pt.caramelo.tv.ui.inicio.InicioScreen
import pt.caramelo.tv.ui.inicio.LocalComVideo
import pt.caramelo.tv.ui.inicio.LocalVideoFalso
import pt.caramelo.tv.ui.parafernalia.PaginaDaPeca
import pt.caramelo.tv.ui.player.Leitor
import pt.caramelo.tv.ui.player.PaginaDoLeitor
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Tinta
import java.io.File
import kotlin.math.roundToInt

/**
 * O filme da app, feito no computador (22/11/2026, Francisco: "da-me um video para mostrar, nao estou na loja ja"). Nao ha
 * TV nem emulador na sessao da nuvem, por isso a app desenha-se aqui fotograma a fotograma, com o relogio a mao, e o
 * ffmpeg junta os fotogramas num MP4. E' a app a serio: os mesmos ecras, as mesmas animacoes, o catalogo que a loja tem
 * nesse momento (caramelodobrasil.eu/api/tv/catalogo) e as fotos do site.
 *
 * O que nao e' a serio, e como se resolve:
 *  - os videos. O Robolectric nao tem descodificador: de cada video tiram-se os fotogramas com o ffmpeg e desenham-se no
 *    lugar do leitor ([LocalVideoFalso] na cena das piteiras; o buraco `video` das paginas do leitor e da peca);
 *  - o Caramelo. O WebP animado nao anima aqui: os fotogramas do proprio loop dao-se um a um ([LocalCarameloFalso]);
 *  - o comando. As setas sao teclas a serio (o foco anda como na TV); o OK e o Voltar mudam de ecra a mao, com a
 *    mesma passagem da MainActivity.
 *
 * So corre a pedido, porque descarrega umas dezenas de MB e demora minutos:
 *   ./scripts/filme-tv.sh          (ou: FILME=1 ./gradlew :app:testDebugUnitTest --tests '*FilmeTest*' -Proborazzi.test.record=true)
 * Os fotogramas ficam em app/build/filme/quadros e o que se descarrega em app/build/filme/cache (fica para a vez seguinte).
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class FilmeTest {
    @get:Rule val compose = createComposeRule()

    private val pasta = File("build/filme").absoluteFile
    private val cache = File(pasta, "cache")
    private val quadros = File(pasta, "quadros")
    private val fps = 25

    /** O fotograma do filme em que se vai: e' o relogio dos videos falsos e do Caramelo. */
    private var n = 0
    private val quadro = mutableIntStateOf(0)

    @Before
    fun coilNaLinhaPrincipal() {
        Coil.setImageLoader(ImageLoader.Builder(RuntimeEnvironment.getApplication()).dispatcher(Dispatchers.Main.immediate).build())
    }

    // ---------------------------------------------------------------------------------------------------------------
    // O que vem de fora: o catalogo, as fotos e os videos, guardados em cache
    // ---------------------------------------------------------------------------------------------------------------

    private fun correr(vararg cmd: String): Int =
        ProcessBuilder(*cmd).redirectErrorStream(true).redirectOutput(File("/dev/null")).start().waitFor()

    private fun nomeDe(url: String) = url.substringBefore('?').substringAfterLast('/')
    private fun local(url: String) = File(cache, "f/" + Integer.toHexString(url.hashCode()) + "-" + nomeDe(url))

    private fun descarregar(urls: Collection<String>) {
        val falta = urls.filter { it.startsWith("http") && !local(it).exists() }.distinct()
        if (falta.isEmpty()) return
        File(cache, "f").mkdirs()
        val lista = File(cache, "curl.txt")
        lista.writeText(falta.joinToString("\n") { "url = \"$it\"\noutput = \"${local(it).path}\"" })
        correr("curl", "-sS", "-Z", "--parallel-max", "8", "--retry", "2", "-K", lista.path)
    }

    /** O catalogo que a loja tem agora, como a app o le, com as fotos trocadas pelos ficheiros descarregados. */
    private fun catalogo(): Catalog {
        cache.mkdirs()
        val json = File(cache, "catalogo.json")
        if (!json.exists()) correr("curl", "-sS", "-o", json.path, "https://caramelodobrasil.eu/api/tv/catalogo")
        val crm = Sincronizacao.ler(json.readText()) ?: error("O catalogo nao se leu: ${json.path}")
        val itens = Sincronizacao.juntar(crm, DefaultCatalog.build().items)
        val pecas = Sincronizacao.parafernalia(crm)
        descarregar(itens.map { it.imageUrl } + pecas.flatMap { p -> listOf(p.imagem) + p.fotos.map { it.imagem } + listOfNotNull(p.cartaz) })
        fun f(u: String?) = u?.takeIf { it.isNotBlank() }?.let { local(it) }?.takeIf { it.exists() && it.length() > 0 }?.let { "file://" + it.path }.orEmpty()
        return Catalog(
            items = itens.map { it.copy(imageUrl = f(it.imageUrl)) },
            parafernalia = pecas.map { p ->
                p.copy(imagem = f(p.imagem), fotos = p.fotos.map { it.copy(imagem = f(it.imagem)) }.filter { it.imagem.isNotBlank() }, cartaz = f(p.cartaz).ifBlank { null })
            },
        )
    }

    /** Os fotogramas de um video, tirados uma vez com o ffmpeg ao tamanho a que se desenham. */
    private inner class Fita(url: String, largura: Int, altura: Int, segundos: Int) {
        private val dir = File(cache, "fitas/" + Integer.toHexString("$url $largura".hashCode()) + "-" + nomeDe(url))
        private val ficheiros: List<File>
        val duracaoMs: Long

        init {
            val feito = File(dir, "ok")
            if (!feito.exists()) {
                dir.mkdirs(); descarregar(listOf(url))
                correr(
                    "ffmpeg", "-y", "-t", "$segundos", "-i", local(url).path,
                    "-vf", "fps=$fps,scale=$largura:$altura:force_original_aspect_ratio=increase,crop=$largura:$altura", "-q:v", "3", File(dir, "%04d.jpg").path,
                )
                val d = ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", local(url).path).start().inputStream.bufferedReader().readText().trim()
                feito.writeText(d)
            }
            ficheiros = dir.listFiles { x -> x.extension == "jpg" }.orEmpty().sorted()
            check(ficheiros.isNotEmpty()) { "Sem fotogramas para $url" }
            duracaoMs = ((feito.readText().toDoubleOrNull() ?: 10.0) * 1000).toLong()
        }

        fun quadro(i: Int): ImageBitmap = BitmapFactory.decodeFile(ficheiros[i.mod(ficheiros.size)].path).asImageBitmap()
    }

    private val fitas = HashMap<String, Fita>()
    private fun fita(url: String, largura: Int, altura: Int, segundos: Int) = fitas.getOrPut("$url $largura") { Fita(url, largura, altura, segundos) }

    @Composable
    private fun VideoFalso(f: Fita, desde: Int, modifier: Modifier) {
        val i = quadro.intValue - desde
        val imagem = remember(f, i) { f.quadro(i) }
        Image(imagem, null, modifier, contentScale = ContentScale.Crop)
    }

    /** Os fotogramas de um loop do Caramelo (WebP animado a 12 por segundo, nos assets da app), tirados com o Pillow. */
    private val caramelos = HashMap<String, List<File>>()
    private fun quadrosDoCaramelo(pose: Pose): List<File> = caramelos.getOrPut(pose.ficheiro) {
        val dir = File(cache, "caramelo/${pose.ficheiro}")
        if (!File(dir, "ok").exists()) {
            dir.mkdirs()
            correr(
                "python3", "-c",
                "import sys\nfrom PIL import Image, ImageSequence\nim = Image.open(sys.argv[1])\nfor i, q in enumerate(ImageSequence.Iterator(im)): q.convert('RGBA').save('%s/%03d.png' % (sys.argv[2], i))",
                File("src/main/assets/caramelo/${pose.ficheiro}.webp").absolutePath, dir.path,
            )
            File(dir, "ok").writeText("")
        }
        dir.listFiles { x -> x.extension == "png" }.orEmpty().sorted()
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Os ecras, com a passagem da MainActivity
    // ---------------------------------------------------------------------------------------------------------------

    private sealed interface Ecra {
        data object Inicio : Ecra
        data class Leitor(val item: MenuItem) : Ecra
        data class Peca(val peca: Parafernalia) : Ecra
        data object Analises : Ecra
    }

    private var ecra by mutableStateOf<Ecra>(Ecra.Inicio)
    private var sentido = 1
    private var foco: String? = null

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun App(c: Catalog) {
        SharedTransitionLayout(Modifier.fillMaxSize().background(Tinta)) {
            val partilha = this
            AnimatedContent(
                targetState = ecra,
                transitionSpec = { (fadeIn(tween(260, delayMillis = 130)) + slideInHorizontally(tween(320, delayMillis = 130)) { it * sentido / 28 }).togetherWith(fadeOut(tween(150))) },
                label = "navegacao",
            ) { alvo ->
                CompositionLocalProvider(LocalSharedTransition provides partilha, LocalNavAnimation provides this) {
                    when (alvo) {
                        Ecra.Inicio -> InicioScreen(
                            catalog = c, focoGuardado = foco, ativo = ecra == Ecra.Inicio,
                            onFoco = { foco = it }, onFocoFlor = {}, onAbrirFlor = {}, onAbrirArtigo = {}, onEditar = {}, onAnalises = {}, onSettings = {},
                        )
                        is Ecra.Leitor -> LeitorDoFilme(c, alvo.item)
                        is Ecra.Peca -> PecaDoFilme(c, alvo.peca)
                        Ecra.Analises -> CoaScreen(itens = c.items, onBack = {})
                    }
                }
            }
        }
    }

    /** O leitor sem ExoPlayer: a pagina e' a da app; o video sao os fotogramas e a barra anda com eles. */
    @Composable
    private fun LeitorDoFilme(c: Catalog, item: MenuItem) {
        val f = remember(item.id) { fita(item.videoUrl, 780, 780, 12) }
        val desde = remember(item.id) { quadro.intValue }
        val segundos = { (quadro.intValue - desde) / fps.toFloat() }
        // o cartaz levanta-se quando o "video" ja tem fotograma, como na app
        val aTocar by remember { derivedStateOf { segundos() > 0.8f } }
        PaginaDoLeitor(
            item = item, atrator = false, aReproduzir = true,
            progresso = { (segundos() * 1000f / f.duracaoMs).coerceIn(0f, 1f) }, posicao = { (segundos() * 1000).toLong() }, duracao = { f.duracaoMs },
            video = { VideoFalso(f, desde, Modifier.fillMaxSize()) },
        ) { Leitor(item = item, videoUrl = "", posterUrl = c.imageUrlOf(item), fotograma = null, mostrarVideo = aTocar, erro = null, entrada = false) }
    }

    @Composable
    private fun PecaDoFilme(c: Catalog, p: Parafernalia) {
        val giro = c.giroDe(p)
        val fotos = remember(p.slug) { c.fotosDe(p) }
        var indice by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { if (giro.isBlank() && fotos.size > 1) while (true) { delay(1500); indice++ } }
        if (giro.isNotBlank()) {
            val f = remember { fita(giro, 780, 780, 6) }
            val desde = remember { quadro.intValue }
            PaginaDaPeca(item = p, fotos = fotos, indice = indice, abre = true) { VideoFalso(f, desde, Modifier.fillMaxSize()) }
        } else {
            PaginaDaPeca(item = p, fotos = fotos, indice = indice, abre = true, video = null)
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // O guiao
    // ---------------------------------------------------------------------------------------------------------------

    /** Deixa passar [s] segundos de app, a gravar um fotograma de 40 em 40 ms. */
    private fun espera(s: Double) {
        repeat((s * fps).roundToInt()) {
            n++; quadro.intValue = n
            compose.mainClock.advanceTimeBy(1000L / fps)
            compose.onRoot().captureRoboImage(File(quadros, "%05d.png".format(n)).path)
        }
    }

    private fun tecla(k: Key, depois: Double) {
        compose.onRoot().performKeyInput { pressKey(k) }
        espera(depois)
    }

    private fun emFoco(c: Catalog): Any? = foco?.let { ch -> if (ch.startsWith("f:")) c.byId(ch.drop(2)) else c.parafernalia.firstOrNull { it.slug == ch.drop(2) } }

    private fun abrir(c: Catalog) {
        sentido = 1
        ecra = when (val x = emFoco(c)) { is MenuItem -> Ecra.Leitor(x); is Parafernalia -> Ecra.Peca(x); else -> ecra }
    }

    private fun voltar() { sentido = -1; ecra = Ecra.Inicio }

    @Test
    fun filme() {
        assumeTrue("So a pedido: FILME=1", System.getenv("FILME") == "1")
        quadros.deleteRecursively(); quadros.mkdirs()
        val c = catalogo()
        // so o comeco, para experimentar o guiao sem esperar pelo filme inteiro
        val curto = System.getenv("FILME_CURTO") == "1"

        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalComVideo provides false,
                LocalVideoFalso provides { url, modifier ->
                    val f = remember(url) { fita(url, 1920, 1080, 9) }
                    val desde = remember(url) { quadro.intValue }
                    VideoFalso(f, desde, modifier)
                },
                LocalCarameloFalso provides { pose, modifier ->
                    val fs = remember(pose) { quadrosDoCaramelo(pose) }
                    if (fs.isNotEmpty()) {
                        val i = (quadro.intValue * 12 / fps).mod(fs.size)
                        val imagem = remember(pose, i) { BitmapFactory.decodeFile(fs[i].path).asImageBitmap() }
                        Image(imagem, null, modifier, contentScale = ContentScale.Fit, alignment = Alignment.BottomCenter)
                    }
                },
            ) { CarameloTheme { App(c) } }
        }

        // 1. As flores: a app abre, tres frascos para a direita, e o OK abre o video da flor.
        espera(3.4)
        repeat(3) { tecla(Key.DirectionRight, 1.5) }
        var tentativas = 0
        while ((emFoco(c) as? MenuItem)?.hasVideo != true && tentativas++ < 6) tecla(Key.DirectionRight, 1.2)
        if (curto) { abrir(c); espera(2.5); return }
        abrir(c); espera(6.5)
        voltar(); espera(1.6)

        // 2. As piteiras: a arte animada atras da janela, e uma aberta, a rodar.
        tecla(Key.DirectionDown, 2.8)
        tecla(Key.DirectionRight, 2.6)
        abrir(c); espera(4.6)
        voltar(); espera(1.4)

        // 3. A parafernalia: as cores a trocar no pedestal, e uma peca aberta.
        tecla(Key.DirectionDown, 2.8)
        repeat(2) { tecla(Key.DirectionRight, 2.2) }
        abrir(c); espera(4.8)
        voltar(); espera(1.2)

        // 4. As analises.
        sentido = 1; ecra = Ecra.Analises; espera(2.4)
        repeat(2) { tecla(Key.DirectionDown, 1.4) }
        espera(1.0)
    }
}
