package pt.caramelo.tv.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.ui.animation.hero
import pt.caramelo.tv.ui.components.TvButton
import pt.caramelo.tv.ui.components.formatarTempo
import pt.caramelo.tv.ui.components.precoCurto
import pt.caramelo.tv.ui.i18n.termo
import pt.caramelo.tv.ui.i18n.tr
import pt.caramelo.tv.ui.inicio.Etiqueta
import pt.caramelo.tv.ui.inicio.Ficha
import pt.caramelo.tv.ui.inicio.FundoDaPagina
import pt.caramelo.tv.ui.inicio.Grelha
import pt.caramelo.tv.ui.inicio.GrelhaPagina
import pt.caramelo.tv.ui.inicio.LuzDasFlores
import pt.caramelo.tv.ui.inicio.MolduraJanela
import pt.caramelo.tv.ui.inicio.NichoDaJanela
import pt.caramelo.tv.ui.inicio.PeDaPagina
import pt.caramelo.tv.ui.inicio.TopoDaPagina
import pt.caramelo.tv.ui.inicio.fichaDaFlor
import pt.caramelo.tv.ui.theme.CarameloTheme
import pt.caramelo.tv.ui.theme.Carbona
import pt.caramelo.tv.ui.theme.Papel
import pt.caramelo.tv.ui.theme.Tinta
import pt.caramelo.tv.ui.theme.Vermelho
import pt.caramelo.tv.ui.theme.corDaSeccao

/**
 * A ficha de uma flor sem video (o OK numa flor que ainda nao tem clipe): a mesma pagina do leitor, com a foto da flor
 * na janela em vez do video.
 *
 * No mundo do ecra inicial (22/11/2026, Francisco, na TV da loja: "o player e a expansao das paginas ainda esta no tema
 * antigo, preciso que coloques tudo consistente"). Era o papel bege de 1/10. Agora a ficha de revista fica onde estava no
 * ecra inicial, com a genetica por baixo quando se sabe, e a janela da marca abre maior no lugar do frasco, com a foto
 * recortada da flor sobre o nicho azul e a etiqueta do preco no canto. A ficha diz o que se ve e se cheira; sem texto
 * corrido, que e' materia regulada.
 */
@Composable
fun DetailsScreen(
    item: MenuItem,
    posterUrl: String,
    resumePositionMs: Long,
    onPlay: (resume: Boolean) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val primeiroBotao = remember { FocusRequester() }
    LaunchedEffect(item.id, item.hasVideo, resumePositionMs > 0) {
        withFrameNanos { }
        runCatching { primeiroBotao.requestFocus() }
    }

    CarameloTheme {
        Box(Modifier.fillMaxSize().background(Tinta)) {
            FundoDaPagina(LuzDasFlores, vivo = true)
            NichoDaJanela()
            MolduraJanela(
                corDaSeccao(item.section),
                x = GrelhaPagina.janelaX, y = GrelhaPagina.janelaY, largura = GrelhaPagina.janela, altura = GrelhaPagina.janela,
            ) { FotoDaFlor(item, posterUrl) }
            precoCurto(item.preco).takeIf { it.isNotBlank() && !item.brevemente }?.let { Etiqueta(it, "", GrelhaPagina.etiquetaX, GrelhaPagina.etiquetaY) }

            val conteudo = fichaDaFlor(item)
            Ficha(conteudo, conteudo.olho, entrada = false) {
                // a genetica so quando nao e' ja o que esta na linha do tipo (flores sem tipo mostram-na la)
                val genetica = item.strain.genetics.takeIf { it.isNotBlank() && it != conteudo.tipo }
                if (genetica != null) Column(Modifier.padding(top = 14.dp).width(Grelha.dadosLargura)) {
                    Text(tr("GENÉTICA", "GENETICS"), color = Papel.copy(alpha = 0.6f), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontFamily = Carbona, maxLines = 1)
                    Text(termo(genetica), color = Papel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = Carbona, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                }
            }

            PeDaPagina {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val podeVer = item.hasVideo
                    if (podeVer && resumePositionMs > 0) {
                        TvButton(text = tr("Retomar em ", "Resume at ") + formatarTempo(resumePositionMs), onClick = { onPlay(true) }, primary = true, focusRequester = primeiroBotao)
                        TvButton(text = tr("Do início", "From the start"), onClick = { onPlay(false) })
                    } else if (podeVer) {
                        TvButton(text = tr("Ver o vídeo", "Play the video"), onClick = { onPlay(false) }, primary = true, focusRequester = primeiroBotao)
                    }
                    TvButton(text = tr("Voltar", "Back"), onClick = onBack, primary = !podeVer, focusRequester = if (podeVer) null else primeiroBotao)
                    TvButton(text = tr("Editar", "Edit"), onClick = onEdit, discreto = true)
                }
            }
            TopoDaPagina(listOf("OK" to tr("escolher", "select"), "↩" to tr("voltar", "back")))
        }
    }
}

/** A foto recortada da flor pousada no nicho da janela, inteira (Fit, nunca Crop); o carimbo quando nao ha para vender. */
@Composable
private fun FotoDaFlor(item: MenuItem, posterUrl: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (posterUrl.isNotBlank() && !item.brevemente) {
            AsyncImage(
                model = posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                colorFilter = if (item.esgotado) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.3f) }) else null,
                // a mesma chave da flor dentro do frasco: a foto voa do frasco ate a janela
                modifier = Modifier.fillMaxSize().padding(34.dp).hero(item.id).graphicsLayer { alpha = if (item.esgotado) 0.7f else 1f },
            )
        }
        val carimbo = when {
            item.brevemente || posterUrl.isBlank() -> tr("EM BREVE", "COMING SOON")
            item.esgotado -> tr("ESGOTADO", "SOLD OUT")
            else -> null
        }
        if (carimbo != null) {
            val cor = if (item.esgotado) Vermelho else Tinta
            val forma = RoundedCornerShape(6.dp)
            Text(
                carimbo, color = cor, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = Carbona, maxLines = 1, softWrap = false,
                modifier = Modifier.graphicsLayer { rotationZ = -8f }.background(Papel, forma).border(3.dp, cor, forma).padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}
