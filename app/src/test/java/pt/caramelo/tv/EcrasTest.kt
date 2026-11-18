package pt.caramelo.tv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.data.model.Strain
import pt.caramelo.tv.ui.caramelo.Caramelo
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.colecao.ColecaoScreen
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.PapelTheme
import java.io.File

/**
 * Os ecras da TV desenhados no JVM, a 1920 x 1080 (960 x 540 dp em xhdpi, como a TCL da loja), para se verem sem TV
 * (18/11/2026). As imagens ficam em app/build/ecras/. Correr: ./gradlew :app:testDebugUnitTest --tests '*EcrasTest*'
 *
 * As fotos das flores vem de src/test/resources/flores (recortes do site reduzidos), carregadas pelo Coil na linha
 * principal para estarem desenhadas quando a imagem se tira.
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

    private fun catalogo(): Catalog {
        val fotos = listOf("skittles-tv-2", "amnesia-hy-pro-tv-2", "sky-tv", "white-banger-tv", "no-milk-tv", "peanut-butter-tv", "solero-cream-tv")
        fun item(s: Section, i: Int, nome: String, estado: String = "venda", aroma: String = "fruta tropical, uva fresca, frutado") = MenuItem(
            id = "${s.name}-$i", slot = i, section = s, title = nome, imageUrl = flor(fotos[i % fotos.size]),
            videoUrl = "https://exemplo/video.mp4", estado = estado, esgotado = estado == "esgotado", preco = if (s == Section.HASH) 25.0 else 38.0,
            strain = Strain(type = "Índica-dominante, com infusão aromática", aroma = aroma),
        )
        val infusao = listOf("Skittles", "Amnesia Hy-Pro", "Sky", "White Banger", "No Milk", "Peanut Butter", "Solero Cream", "Berry Pie", "Pulsar", "Forbidden Fruit", "Sour Lifter")
            .mapIndexed { i, n -> item(Section.INFUSAO, i, n, if (i == 3) "esgotado" else "venda") }
        val cbd = listOf("Mars", "Critical Berry").mapIndexed { i, n -> item(Section.CBD, i, n) }
        val hash = listOf("Static", "Mango Melt", "Novo").mapIndexed { i, n -> item(Section.HASH, i, n, if (i == 2) "brevemente" else "venda") }
        return Catalog(items = infusao + cbd + hash)
    }

    @Test
    fun colecao() {
        val c = catalogo()
        compose.setContent {
            ColecaoScreen(
                catalog = c, focusedId = c.items.first().id, onFocusItem = {}, onOpenItem = {}, onEditItem = {},
                onSettings = {}, onMontra = {}, onCoa = {}, onParafernalia = {},
            )
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/ecras/01-colecao.png")
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
