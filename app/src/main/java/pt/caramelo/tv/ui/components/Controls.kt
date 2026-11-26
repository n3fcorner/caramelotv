package pt.caramelo.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import pt.caramelo.tv.ui.theme.noPapel
import androidx.compose.foundation.shape.RoundedCornerShape
import pt.caramelo.tv.ui.i18n.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import pt.caramelo.tv.ui.theme.Creme

/**
 * Botao unico para todo o projeto. Assenta em Surface em vez de Button para
 * manter uma so primitiva de foco, mais facil de afinar depois na TV real.
 *
 * No traco do ecra inicial (22/11/2026, "preciso que coloques tudo consistente"): cantos de 8 dp, o fundo quase
 * transparente com o contorno bege fino das caixas da ficha tecnica, e o foco sempre a amarelo cheio com a letra a tinta
 * (regra 3 das cores: so o foco acende a amarelo). O principal ja nasce amarelo. No papel (o tema antigo, que ja nenhum
 * ecra usa) fica como era.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    /** Sem preenchimento em repouso; so ao ganhar foco e que acende. */
    discreto: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val papel = noPapel()
    val shape = RoundedCornerShape(8.dp)
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(
            focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
        ),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                primary -> scheme.primary
                discreto -> Color.Transparent
                papel -> scheme.surfaceVariant
                else -> Creme.copy(alpha = 0.08f)
            },
            contentColor = when {
                primary -> scheme.onPrimary
                discreto -> scheme.onSurfaceVariant
                else -> scheme.onSurface
            },
            focusedContainerColor = scheme.primary,
            focusedContentColor = scheme.onPrimary,
            disabledContainerColor = if (papel) scheme.surface else Creme.copy(alpha = 0.04f),
            disabledContentColor = scheme.onSurfaceVariant,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = when {
                papel -> Border(BorderStroke(2.dp, scheme.onSurface), shape = shape)
                primary || discreto -> Border.None
                else -> Border(BorderStroke(1.5.dp, Creme.copy(alpha = 0.22f)), shape = shape)
            },
            focusedBorder = Border(BorderStroke(2.dp, if (papel) scheme.onSurface else Creme), shape = shape),
        ),
    ) {
        Box(
            // align dentro do BoxScope da Surface: sem isto o texto de um botao
            // que ocupa a largura toda encosta a esquerda.
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Linha focavel com um rotulo a esquerda e um valor a direita. Base das listas de definicoes. */
@Composable
fun SelectableRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    /** Um traco de cor a esquerda (a cor da seccao do produto). */
    marca: Color? = null,
) {
    val papel = noPapel()
    // Cartoes de cantos de 8 dp: no mundo azul, o fundo quase transparente e o contorno bege fino das caixas da ficha
    // tecnica (22/11/2026); o foco a amarelo cheio. No papel (o tema antigo) ficam brancos com o contorno a tinta.
    val shape = RoundedCornerShape(8.dp)
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (papel) Color.White.copy(alpha = 0.6f) else Creme.copy(alpha = 0.06f),
            contentColor = scheme.onSurface,
            focusedContainerColor = scheme.primary,
            focusedContentColor = scheme.onPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            border = if (papel) Border(BorderStroke(2.dp, scheme.onSurface), shape = shape) else Border(BorderStroke(1.5.dp, Creme.copy(alpha = 0.18f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, if (papel) scheme.onSurface else Creme), shape = shape),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            marca?.let { Box(Modifier.padding(end = 12.dp).width(4.dp).height(22.dp).background(it, RoundedCornerShape(2.dp))) }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

/** Linha de definicao ligado/desligado. */
@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    SelectableRow(
        label = label,
        value = if (checked) tr("Ligado", "On") else tr("Desligado", "Off"),
        onClick = { onToggle(!checked) },
        modifier = modifier,
        focusRequester = focusRequester,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
        modifier = modifier,
    )
}
