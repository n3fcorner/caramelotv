package pt.caramelo.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pt.caramelo.tv.ui.caramelo.Caramelo
import pt.caramelo.tv.ui.caramelo.Pose
import pt.caramelo.tv.ui.theme.PapelTheme

/**
 * Os ecras da TV desenhados no JVM, a 1920 x 1080 (960 x 540 dp em xhdpi, como a TCL da loja), para se verem sem TV
 * (18/11/2026). As imagens ficam em app/build/ecras/. Correr: ./gradlew :app:testDebugUnitTest --tests '*EcrasTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class EcrasTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun poses_do_caramelo() {
        compose.setContent {
            PapelTheme {
                Row(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pose.entries.forEach { Caramelo(it, Modifier.size(width = 100.dp, height = 220.dp)) }
                }
                Text("Carbona: a coleção do Caramelo · 38,00 €", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(24.dp))
            }
        }
        compose.onRoot().captureRoboImage("build/ecras/00-caramelo.png")
    }
}
