package pt.caramelo.tv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.robolectric.RuntimeEnvironment
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.Coil
import coil.ImageLoader
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
import pt.caramelo.tv.ui.inicio.LocalComVideo
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.PapelTheme
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
