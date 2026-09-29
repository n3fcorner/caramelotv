package pt.caramelo.tv.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Portugues e ingles (Francisco, 29/9/2026: "garante que tem versao pt e ingles").
 *
 * A lingua vive nas definicoes do catalogo e chega aos ecras por [LocalIdioma]. E' um
 * CompositionLocal estatico: muda uma vez por visita, e quando muda recompoe-se tudo
 * de uma vez, sem cada Text a observar um estado.
 *
 * Os textos da interface escrevem-se ao lado um do outro com [tr]; os do catalogo
 * (aromas, cultivos, terpenos, geneticas) sao dados e traduzem-se por [termo], com o
 * dicionario em [Termos]. O que o dicionario nao conhece fica em portugues: melhor
 * uma palavra por traduzir do que uma traducao inventada.
 */
enum class Idioma(val codigo: String) {
    PT("pt"), EN("en");

    companion object {
        fun de(codigo: String?): Idioma = entries.firstOrNull { it.codigo == codigo } ?: PT
    }
}

val LocalIdioma = staticCompositionLocalOf { Idioma.PT }

/** O texto da interface na lingua actual. */
@Composable
@ReadOnlyComposable
fun tr(pt: String, en: String): String = if (LocalIdioma.current == Idioma.EN) en else pt

/** Um texto do catalogo na lingua actual. */
@Composable
@ReadOnlyComposable
fun termo(pt: String): String = if (LocalIdioma.current == Idioma.EN) Termos.en(pt) else pt

object Termos {
    private val EN = mapOf(
        // linhas, formatos e tipos
        "Infusão" to "Infusion", "CBD" to "CBD", "Hash" to "Hash", "Flor" to "Flower", "Hash piattella" to "Piattella hash",
        "Índica" to "Indica", "Sativa" to "Sativa", "Híbrida" to "Hybrid",
        // cultivos (nomes de uso internacional)
        "Indoor" to "Indoor", "Greenhouse" to "Greenhouse", "Glasshouse" to "Glasshouse",
        // terpenos
        "Mirceno" to "Myrcene", "Cariofileno" to "Caryophyllene", "Limoneno" to "Limonene", "Pineno" to "Pinene",
        "Linalol" to "Linalool", "Humuleno" to "Humulene", "Ocimeno" to "Ocimene", "Terpineol" to "Terpineol", "Terpinoleno" to "Terpinolene",
        // aromas
        "Amendoim torrado, fundo doce" to "Roasted peanut, sweet base",
        "Cremoso e doce, baunilha e citrinos" to "Creamy and sweet, vanilla and citrus",
        "Cítrico e cremoso, fundo a diesel" to "Citrus and creamy, diesel base",
        "Cítrico e doce" to "Citrus and sweet",
        "Doce e cremoso, fundo terroso" to "Sweet and creamy, earthy base",
        "Doce e resinoso, fundo terroso" to "Sweet and resinous, earthy base",
        "Fresco e floral" to "Fresh and floral",
        "Frutos vermelhos e doce de forno" to "Red berries and baked sweets",
        "Frutos vermelhos sobre fundo terroso" to "Red berries over an earthy base",
        "Resinoso" to "Resinous",
        "Limão e casca fresca, fundo herbáceo" to "Lemon and fresh peel, herbal base",
        "Manga fresca" to "Fresh mango",
        "Manga madura e resina" to "Ripe mango and resin",
        "Maçã verde e ácido, fundo doce" to "Green apple and sour, sweet base",
        "Mirtilo e frutos silvestres" to "Blueberry and wild berries",
        "Papaia madura, fundo resinoso" to "Ripe papaya, resinous base",
        "Pêssego doce" to "Sweet peach",
        "Seco e especiado, fundo a pinho" to "Dry and spiced, pine base",
        "Terroso e a pinho, fundo resinoso" to "Earthy and piney, resinous base",
        "Terroso e café, fundo doce" to "Earthy and coffee, sweet base",
        "Uva e frutos escuros, fundo doce" to "Grape and dark fruit, sweet base",
        "Ácido e cítrico, fundo a diesel" to "Sour and citrus, diesel base",
        // geneticas (so as partes em portugues)
        "Blueberry de DJ Short" to "DJ Short's Blueberry",
        "Família Critical" to "Critical family",
        "Família OG (atribuída)" to "OG family (attributed)",
        "Descendente de Tangie (selecção Doweedo)" to "Tangie descendant (Doweedo selection)",
    )

    /** As partes que se repetem nas fichas curtas do CRM ("Híbrida, índica-dominante"). */
    private val PARTES = listOf(
        "(atribuída)" to "(attributed)", "índica-dominante" to "indica-dominant", "sativa-dominante" to "sativa-dominant",
        "Sativa-dominante" to "Sativa-dominant", "Índica-dominante" to "Indica-dominant", "Híbrida" to "Hybrid", "Índica" to "Indica",
    )

    fun en(pt: String): String = EN[pt] ?: PARTES.fold(pt) { t, (a, b) -> t.replace(a, b) }
}
