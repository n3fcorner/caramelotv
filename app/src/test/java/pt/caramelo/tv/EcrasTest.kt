package pt.caramelo.tv

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.robolectric.RuntimeEnvironment
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.Coil
import coil.ImageLoader
import coil.compose.AsyncImage
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pt.caramelo.tv.data.model.Analise
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.Especificacao
import pt.caramelo.tv.data.model.FotoDeCor
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.data.model.Strain
import pt.caramelo.tv.ui.caramelo.Caramelo
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.inicio.InicioScreen
import pt.caramelo.tv.ui.animation.LocalNavAnimation
import pt.caramelo.tv.ui.animation.LocalSharedTransition
import pt.caramelo.tv.ui.coa.CoaScreen
import pt.caramelo.tv.ui.details.DetailsScreen
import pt.caramelo.tv.ui.settings.EditItemScreen
import pt.caramelo.tv.ui.settings.SettingsScreen
import pt.caramelo.tv.ui.inicio.LocalComVideo
import pt.caramelo.tv.ui.parafernalia.PaginaDaPeca
import pt.caramelo.tv.ui.player.Leitor
import pt.caramelo.tv.ui.player.PaginaDoLeitor
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.PapelTheme
import pt.caramelo.tv.ui.theme.Tinta
import java.io.File

/**
 * Os ecras da TV desenhados no JVM, a 1920 x 1080 (960 x 540 dp em xhdpi, como a TCL da loja), para se verem sem TV
 * (18/11/2026). As imagens ficam em app/build/ecras/. Correr:
 * ./gradlew :app:testDebugUnitTest --tests '*EcrasTest*' -Proborazzi.test.record=true
 *
 * As fotos vem de src/test/resources (recortes do site reduzidos), carregadas pelo Coil na linha principal para estarem
 * desenhadas quando a imagem se tira. Sem ExoPlayer (LocalComVideo): a cena das piteiras fica no cartaz.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class EcrasTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun coilNaLinhaPrincipal() {
        val ctx: Context = RuntimeEnvironment.getApplication()
        Coil.setImageLoader(ImageLoader.Builder(ctx).dispatcher(Dispatchers.Main.immediate).build())
    }

    private fun flor(nome: String) = "file://" + File("src/test/resources/flores/$nome.webp").absolutePath
    private fun peca(nome: String) = "file://" + File("src/test/resources/pecas/$nome").absolutePath

    private fun catalogo(): Catalog {
        fun item(s: Section, i: Int, nome: String, foto: String, estado: String = "venda", tipo: String = "Índica-dominante, com infusão aromática", indica: Int = 70,
                 aroma: String = "fruta tropical, uva fresca, frutado", terp: List<String> = listOf("cariofileno", "humuleno", "linalol"), lote: String = "") = MenuItem(
            id = "${s.name}-$i", slot = i, section = s, title = nome, imageUrl = flor(foto),
            videoUrl = "https://exemplo/video.mp4", estado = estado, esgotado = estado == "esgotado", preco = if (s == Section.HASH) 25.0 else 38.0,
            strain = Strain(type = tipo, aroma = aroma, indica = indica, terpenes = terp, cultivation = "Indoor"),
            analise = if (lote.isNotBlank()) Analise(lote = lote) else null,
        )
        val cbd = listOf(
            item(Section.CBD, 0, "Mars", "sky-tv", tipo = "Híbrida", indica = 50),
            item(Section.CBD, 1, "Solero Cream", "solero-cream-tv", tipo = "Híbrida", indica = 50, aroma = "fruta tropical, cítrico cremoso", terp = listOf("mirceno", "cariofileno", "pineno")),
        )
        val infusao = listOf("Skittles" to "skittles-tv-2", "Amnesia" to "amnesia-hy-pro-tv-2", "Sky" to "sky-tv", "White Banger" to "white-banger-tv", "No Milk" to "no-milk-tv", "Peanut Butter" to "peanut-butter-tv", "Prensadinho" to "")
            .mapIndexed { i, (n, f) -> item(Section.INFUSAO, i, n, f.ifBlank { "sky-tv" }, if (n == "Prensadinho") "brevemente" else if (n == "White Banger") "esgotado" else "venda", lote = if (i == 0) "SKT-TR-001" else "") }
        val hash = listOf(item(Section.HASH, 0, "Mango Melt", "peanut-butter-tv", tipo = "Hash piattella, com infusão aromática", indica = 50, aroma = "manga, tropical, creme", terp = listOf("mirceno", "ocimeno", "limoneno"), lote = "PT-H"))
        val pit = listOf(
            Parafernalia(slug = "piteira-vidro-fauna-brasileira-6mm", nome = "Piteira de vidro Fauna Brasileira 6mm", categoria = "piteiras", estado = "venda", preco = 15.0,
                imagem = peca("piteira-fauna.webp"), cena = "", cartaz = peca("cena-fauna.jpg"),
                ficha = listOf(Especificacao("Material", "Vidro"), Especificacao("Diâmetros", "5 / 6 / 7 mm"), Especificacao("Medidas", "55 mm de comprimento"), Especificacao("Acabamento", "Impressão a toda a volta"))),
            Parafernalia(slug = "kit-piteiras-fauna", nome = "Kit Piteiras de Vidro Fauna", categoria = "piteiras", estado = "venda", preco = 35.0, imagem = peca("kit-fauna.webp"), cartaz = peca("cena-fauna.jpg"),
                ficha = listOf(Especificacao("Material", "Vidro"), Especificacao("Peças", "3"), Especificacao("Artes", "Flora brasileira, Fauna brasileira, O Brasil é Indígena"))),
        )
        val par = listOf(
            Parafernalia(slug = "grinder-premium-aluminio", nome = "Grinder Premium alumínio anodizado", categoria = "grinders", estado = "venda", preco = 25.0, imagem = peca("grinder-azul.webp"),
                fotos = listOf(FotoDeCor("Azul metalizado", peca("grinder-azul.webp")), FotoDeCor("Vermelho metalizado", peca("grinder-vermelho.webp"))),
                ficha = listOf(Especificacao("Material", "Alumínio anodizado"), Especificacao("Medidas", "55 × 20 mm"), Especificacao("Dentes", "Em diamante"), Especificacao("Tampa", "Magnética, gravada com o mascote"))),
            Parafernalia(slug = "cuia-silicone", nome = "Cuia de silicone", categoria = "cuias", estado = "venda", preco = 4.0, imagem = peca("cuia-aurora.webp"),
                fotos = listOf(FotoDeCor("Aurora boreal", peca("cuia-aurora.webp"))), ficha = listOf(Especificacao("Material", "Silicone"), Especificacao("Base", "Antiderrapante"))),
            Parafernalia(slug = "cinzeiro-silicone", nome = "Cinzeiro de silicone", categoria = "cinzeiros", estado = "esgotado", preco = 7.0, imagem = peca("cinzeiro.webp")),
        )
        return Catalog(items = cbd + infusao + hash, parafernalia = pit + par)
    }

    private fun inicio(foco: String?, ficheiro: String) {
        val c = catalogo()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalComVideo provides false) {
                CarameloTheme {
                    InicioScreen(
                        catalog = c, focoGuardado = foco, ativo = true,
                        onFoco = {}, onFocoFlor = {}, onAbrirFlor = {}, onAbrirArtigo = {}, onEditar = {}, onAnalises = {}, onSettings = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(3200)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(800)
        compose.onRoot().captureRoboImage("build/ecras/$ficheiro")
    }

    @Test fun inicio_flores() = inicio("f:CBD-1", "01-inicio-flores.png")
    @Test fun inicio_hash() = inicio("f:HASH-0", "02-inicio-hash.png")
    @Test fun inicio_piteiras() = inicio("p:piteira-vidro-fauna-brasileira-6mm", "03-inicio-piteiras.png")
    @Test fun inicio_parafernalia() = inicio("p:grinder-premium-aluminio", "04-inicio-parafernalia.png")

    /** Uma pagina que abre a partir do ecra inicial, desenhada como o ecra inicial: sem video e com o relogio a mao. */
    private fun pagina(ficheiro: String, conteudo: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { CompositionLocalProvider(LocalComVideo provides false) { CarameloTheme { Box(Modifier.fillMaxSize().background(Tinta)) { conteudo() } } } }
        compose.mainClock.advanceTimeBy(3200)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(800)
        compose.onRoot().captureRoboImage("build/ecras/$ficheiro")
    }

    // As paginas que abrem (22/11/2026, "o player e a expansao das paginas ainda esta no tema antigo"): o leitor, a peca e a
    // ficha de uma flor sem video, no mundo do ecra inicial. O video nao se desenha aqui: fica o cartaz na janela.
    @Test
    fun leitor_de_uma_flor() {
        val c = catalogo(); val item = c.items.first { it.title == "Solero Cream" }
        pagina("05-leitor.png") {
            PaginaDoLeitor(
                item = item, atrator = false, aReproduzir = true, progresso = { 0.37f }, posicao = { 19_000L }, duracao = { 52_000L }, video = {},
            ) { Leitor(item = item, videoUrl = "", posterUrl = c.imageUrlOf(item), fotograma = null, mostrarVideo = false, erro = null, entrada = false) }
        }
    }

    @Test
    fun leitor_de_um_hash_na_montra_automatica() {
        val c = catalogo(); val item = c.items.first { it.section == Section.HASH }
        pagina("06-leitor-hash-montra.png") {
            PaginaDoLeitor(
                item = item, atrator = true, aReproduzir = true, progresso = { 0.8f }, posicao = { 42_000L }, duracao = { 52_000L }, video = {},
            ) { Leitor(item = item, videoUrl = "", posterUrl = c.imageUrlOf(item), fotograma = null, mostrarVideo = false, erro = null, entrada = true) }
        }
    }

    @Test
    fun peca_aberta_com_as_fotos_das_cores() {
        val c = catalogo(); val p = c.parafernalia.first { it.slug == "grinder-premium-aluminio" }
        pagina("07-peca-fotos.png") { PaginaDaPeca(item = p, fotos = c.fotosDe(p), indice = 0, video = null) }
    }

    @Test
    fun piteira_aberta_com_o_giro() {
        val c = catalogo(); val p = c.parafernalia.first { it.slug == "piteira-vidro-fauna-brasileira-6mm" }
        // no lugar do video do giro, a foto da piteira sobre o nicho (o giro tem o mesmo fundo azul)
        pagina("08-piteira-giro.png") {
            PaginaDaPeca(item = p, fotos = c.fotosDe(p), indice = 0) { AsyncImage(c.imagemDe(p), null, Modifier.fillMaxSize().padding(60.dp), contentScale = ContentScale.Fit) }
        }
    }

    @Test
    fun ficha_de_uma_flor_sem_video() {
        val c = catalogo(); val item = c.items.first { it.title == "No Milk" }.copy(videoUrl = "", strain = c.items.first { it.title == "No Milk" }.strain.copy(genetics = "Zkittlez × Gelato"))
        pagina("09-ficha-sem-video.png") { DetailsScreen(item = item, posterUrl = c.imageUrlOf(item), resumePositionMs = 0, onPlay = {}, onEdit = {}, onBack = {}) }
    }

    @Test
    fun analises() {
        val c = catalogo()
        val itens = c.items.mapIndexed { i, it -> if (i % 2 == 0) it.copy(analise = Analise(lote = "TR-00${i + 1}", laboratorio = "Christian Fuczik Chemisches Labor GmbH (Viena)", numero = "E58016${40 + i}", data = "2026-09-10", metodo = "HPLC-DAD", thc = 0.08 + i * 0.02, cbd = 3.61, d9thc = 0.05, cbg = 0.09)) else it }
        pagina("10-analises.png") { CoaScreen(itens = itens, onBack = {}) }
    }

    @Test
    fun definicoes() {
        val c = catalogo()
        pagina("11-definicoes.png") { SettingsScreen(catalog = c, onEditItem = {}, onSettingsChange = {}, onClearProgress = {}, onResetCatalog = {}, onBack = {}) }
    }

    @Test
    fun editar_um_produto() {
        val c = catalogo()
        pagina("12-editar.png") { EditItemScreen(item = c.items.first { it.title == "Skittles" }, onSave = {}, onCancel = {}) }
    }

    /**
     * A passagem do ecra inicial para a pagina de uma peca, a meio (22/11/2026): a mesma navegacao da MainActivity (o ecra
     * que sai esbate-se em 150 ms, o que entra aparece a partir dos 130 ms) com as pecas partilhadas: o logotipo e a ficha
     * ficam onde estao, e a janela da pagina nasce do tamanho da do ecra inicial e cresce. Tres imagens: aos 120, aos 260 e aos 900 ms.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun passagem_do_inicio_para_a_peca() {
        val c = catalogo(); val p = c.parafernalia.first { it.slug == "grinder-premium-aluminio" }
        var aberta by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalComVideo provides false) {
                CarameloTheme {
                    SharedTransitionLayout(Modifier.fillMaxSize().background(Tinta)) {
                        val partilha = this
                        AnimatedContent(
                            targetState = aberta,
                            transitionSpec = { (fadeIn(tween(260, delayMillis = 130)) + slideInHorizontally(tween(320, delayMillis = 130)) { it / 28 }).togetherWith(fadeOut(tween(150))) },
                            label = "navegacao",
                        ) { a ->
                            CompositionLocalProvider(LocalSharedTransition provides partilha, LocalNavAnimation provides this) {
                                if (!a) InicioScreen(catalog = c, focoGuardado = "p:" + p.slug, ativo = true, onFoco = {}, onFocoFlor = {}, onAbrirFlor = {}, onAbrirArtigo = {}, onEditar = {}, onAnalises = {}, onSettings = {})
                                else PaginaDaPeca(item = p, fotos = c.fotosDe(p), indice = 0, abre = true, video = null)
                            }
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(3200); compose.waitForIdle(); compose.mainClock.advanceTimeBy(800)
        aberta = true
        compose.mainClock.advanceTimeByFrame(); compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100); compose.onRoot().captureRoboImage("build/ecras/13-passagem-120ms.png")
        compose.mainClock.advanceTimeBy(140); compose.onRoot().captureRoboImage("build/ecras/14-passagem-260ms.png")
        compose.mainClock.advanceTimeBy(640); compose.onRoot().captureRoboImage("build/ecras/15-passagem-900ms.png")
    }

    @Test
    fun poses_do_caramelo() {
        compose.setContent {
            PapelTheme {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pose.entries.forEach { Caramelo(it, Modifier.size(width = 100.dp, height = 220.dp)) }
                    }
                    Text("Carbona: a coleção do Caramelo · 38,00 €", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 24.dp))
                }
            }
        }
        compose.onRoot().captureRoboImage("build/ecras/00-caramelo.png")
    }
}
