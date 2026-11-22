package pt.caramelo.tv.ui.settings

import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme as M3Theme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.components.SectionHeader
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.theme.Amarelo
import pt.caramelo.tv.ui.theme.Creme
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.inicio.FundoDaPagina
import pt.caramelo.tv.ui.inicio.Grelha
import pt.caramelo.tv.ui.inicio.GrelhaPagina
import pt.caramelo.tv.ui.inicio.LuzDasFlores
import pt.caramelo.tv.ui.inicio.TopoDaPagina
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Formulario de um slot.
 *
 * Os campos de texto vem de compose-material3: a biblioteca de TV nao tem TextField,
 * e esta e a combinacao recomendada. Ao ganhar foco abrem o teclado do sistema da TV.
 * Para URLs longos e mais pratico usar o deep link por ADB descrito no README.
 */
@Composable
fun EditItemScreen(
    item: MenuItem,
    onSave: (MenuItem) -> Unit,
    onCancel: () -> Unit,
) {
    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var videoUrl by rememberSaveable(item.id) { mutableStateOf(item.videoUrl) }
    var imageUrl by rememberSaveable(item.id) { mutableStateOf(item.imageUrl) }

    // No mundo do ecra inicial (22/11/2026): o azul com a luz parada e o topo das outras paginas; o nome do produto em
    // grande, como na ficha, e os campos com o contorno bege fino.
    Box(Modifier.fillMaxSize().background(Tinta)) {
    FundoDaPagina(LuzDasFlores, eixo = 480.dp, vivo = false)
    TopoDaPagina(listOf("OK" to tr("escrever", "type"), "↩" to tr("cancelar", "cancel")), titulo = tr("Editar produto", "Edit product"))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Grelha.margem, end = Grelha.margem, top = Grelha.fichaY, bottom = GrelhaPagina.margemDeBaixo)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(termo(item.section.label))
        Text(item.displayTitle, color = Papel, fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp, fontFamily = Carbona, maxLines = 1)

        M3Theme(colorScheme = editorColors()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(tr("Título", "Title"), title) { title = it }
                Field(tr("URL do vídeo (mp4 ou m3u8)", "Video URL (mp4 or m3u8)"), videoUrl) { videoUrl = it }
                Field(tr("URL da imagem", "Image URL"), imageUrl) { imageUrl = it }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvButton(
                text = tr("Guardar", "Save"),
                primary = true,
                onClick = {
                    onSave(
                        item.copy(
                            title = title.trim(),
                            videoUrl = videoUrl.trim(),
                            imageUrl = imageUrl.trim(),
                        ),
                    )
                },
            )
            TvButton(text = tr("Cancelar", "Cancel"), onClick = onCancel)
        }
    }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { M3Text(label) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun editorColors() = remember {
    darkColorScheme(
        primary = Amarelo,
        surface = Tinta,
        onSurface = Creme,
        onSurfaceVariant = Creme.copy(alpha = 0.7f),
        outline = Creme.copy(alpha = 0.35f),
    )
}
