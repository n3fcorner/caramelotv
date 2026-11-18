package pt.caramelo.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.caramelo.tv.data.DefaultCatalog
import pt.caramelo.tv.data.Sincronizacao

/**
 * A juncao do catalogo do site com o de fabrica (18/11/2026): o preco, o estado e a pagina do lote passam a chegar ao
 * frasco (o rotulo leva o preco; o brevemente e' o frasco vazio). Com um excerto do /api/tv/catalogo real.
 */
class SincronizacaoTest {
    private val excerto = """
        {"versao":2,"itens":[
          {"slug":"mars-cbd","nome":"Mars","seccao":"cbd","estado":"venda","preco":38,"desde":false,"infundido":false,
           "imagem":"https://caramelodobrasil.eu/img/produtos/prensadinho.webp?v=3","video":null,
           "lote":{"codigo":"MARSCBD-001","url":"https://caramelodobrasil.eu/lote/MARSCBD-001","analise":{"laboratorio":"IFHA","numero":"E5801651","data":"2026-09-10","metodo":"HPLC-DAD","thc":0.12,"cbd":8.74,"d9thc":0.06,"cbg":0.16}},
           "tipo":"","cultivo":"","aromas":[],"genetica":"Dioica 88","indica":null,"terpenos":[]},
          {"slug":"skittles","nome":"Skittles","seccao":"infusao","estado":"esgotado","preco":38,"infundido":true,
           "imagem":"https://caramelodobrasil.eu/img/produtos/skittles-tv-2.webp","video":"https://caramelodobrasil.eu/video/skittles.mp4",
           "tipo":"Índica-dominante, com infusão aromática","aromas":["fruta tropical","uva fresca","frutado"],"indica":70,
           "terpenos":[{"nome":"cariofileno","pct":null},{"nome":"humuleno","pct":null},{"nome":"linalol","pct":null}]},
          {"slug":"novo","nome":"Novo","seccao":"hash","estado":"brevemente","preco":null,"imagem":null,"video":null}
        ],"parafernalia":[]}
    """.trimIndent()

    @Test
    fun o_preco_o_estado_e_o_lote_chegam_ao_item() {
        val crm = Sincronizacao.ler(excerto)
        assertNotNull(crm)
        val itens = Sincronizacao.juntar(crm!!, DefaultCatalog.build().items)
        val mars = itens.first { it.title == "Mars" }
        assertEquals(38.0, mars.preco!!, 0.001)
        assertEquals("venda", mars.estado)
        assertEquals("https://caramelodobrasil.eu/lote/MARSCBD-001", mars.loteUrl)

        val skittles = itens.first { it.title == "Skittles" }
        assertTrue(skittles.esgotado)
        assertEquals(70, skittles.strain.indica)

        val novo = itens.first { it.title == "Novo" }
        assertTrue(novo.brevemente)
        assertEquals(null, novo.preco)
    }
}
