package pt.caramelo.tv.ui.settings

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
import pt.caramelo.tv.ui.theme.Roxo
import pt.caramelo.tv.ui.theme.Overscan

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
    var description by rememberSaveable(item.id) { mutableStateOf(item.description) }
    var videoUrl by rememberSaveable(item.id) { mutableStateOf(item.videoUrl) }
    var imageUrl by rememberSaveable(item.id) { mutableStateOf(item.imageUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Overscan.horizontal, vertical = Overscan.vertical)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(item.section.label)
        Text("Editar slot", style = MaterialTheme.typography.headlineMedium)

        M3Theme(colorScheme = editorColors()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Título", title) { title = it }
                Field("Descrição", description, singleLine = false) { description = it }
                Field("URL do vídeo (mp4 ou m3u8)", videoUrl) { videoUrl = it }
                Field("URL da imagem", imageUrl) { imageUrl = it }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvButton(
                text = "Guardar",
                primary = true,
                onClick = {
                    onSave(
                        item.copy(
                            title = title.trim(),
                            description = description.trim(),
                            videoUrl = videoUrl.trim(),
                            imageUrl = imageUrl.trim(),
                        ),
                    )
                },
            )
            TvButton(text = "Cancelar", onClick = onCancel)
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
        surface = Roxo,
        onSurface = Creme,
        onSurfaceVariant = Creme,
        outline = Amarelo,
    )
}
