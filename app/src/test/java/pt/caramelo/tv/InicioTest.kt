package pt.caramelo.tv

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.caramelo.tv.data.Sincronizacao
import pt.caramelo.tv.data.model.Catalog
import pt.caramelo.tv.data.model.Especificacao
import pt.caramelo.tv.data.model.FotoDeCor
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Parafernalia
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.components.Aroma
import pt.caramelo.tv.ui.inicio.Inicio

/** As contas do ecra inicial novo (18/11/2026), sem desenhar nada. */
class InicioTest {
    @Test
    fun as_flores_seguem_cbd_infusao_hash() {
        val c = Catalog(items = listOf(
            MenuItem(id = "h", slot = 0, section = Section.HASH, title = "Mango Melt"),
            MenuItem(id = "i", slot = 0, section = Section.INFUSAO, title = "Skittles"),
            MenuItem(id = "c", slot = 0, section = Section.CBD, title = "Solero"),
        ))
        assertEquals(listOf("c", "i", "h"), Inicio.flores(c).map { it.id })
    }

    @Test
    fun piteiras_de_vidro_e_kits_tem_fila_propria() {
        val c = Catalog(parafernalia = listOf(
            Parafernalia(slug = "kit-piteiras-fauna"), Parafernalia(slug = "piteira-vidro-fauna-brasileira-6mm"),
            Parafernalia(slug = "piteiras-papel-longa"), Parafernalia(slug = "grinder-premium-aluminio"),
        ))
        assertEquals(listOf("kit-piteiras-fauna", "piteira-vidro-fauna-brasileira-6mm"), Inicio.piteiras(c).map { it.slug })
        assertEquals(listOf("piteiras-papel-longa", "grinder-premium-aluminio"), Inicio.parafernalia(c).map { it.slug })
    }

    @Test
    fun o_tipo_sem_a_infusao_e_o_nome_sem_o_que_ja_esta_noutro_sitio() {
        assertEquals("Índica-dominante", Inicio.tipoSemInfusao("Índica-dominante, com infusão aromática"))
        assertEquals("Hash piattella", Inicio.tipoSemInfusao("Hash piattella, com infusão aromática"))
        val grinder = Parafernalia(slug = "grinder-premium-aluminio", nome = "Grinder Premium alumínio anodizado", categoria = "grinders",
            ficha = listOf(Especificacao("Material", "Alumínio anodizado"), Especificacao("Medidas", "55 × 20 mm")),
            fotos = listOf(FotoDeCor("Azul metalizado"), FotoDeCor("Dourado")))
        assertEquals("Grinder Premium", Inicio.nomeDe(grinder))
        assertEquals("Alumínio anodizado", Inicio.tipoDe(grinder))
        assertEquals("GRINDER · 2 CORES", Inicio.olhoDe(grinder))
        assertEquals("GRINDER · AZUL METALIZADO", Inicio.olhoComCor(grinder, "Azul metalizado"))
        assertEquals(listOf("Medidas", "Cores"), Inicio.especificacoesDe(grinder).map { it.rotulo })
        val pit = Parafernalia(slug = "piteira-vidro-fauna-brasileira-6mm", nome = "Piteira de vidro Fauna Brasileira 6mm")
        assertEquals("Fauna Brasileira", Inicio.nomeDe(pit))
        assertEquals("PITEIRA DE VIDRO · 6 mm", Inicio.olhoDe(pit))
        assertEquals("Kit Fauna", Inicio.nomeDe(Parafernalia(slug = "kit-piteiras-fauna", nome = "Kit Piteiras de Vidro Fauna")))
    }

    @Test
    fun a_fila_desliza_para_o_foco_ficar_entre_a_margem_e_a_terceira_peca_do_fim() {
        val m = 56f; val p = 82f; val w = 960f
        assertEquals(0f, Inicio.deslize(56f, 70f, w, m, p), 0.01f)
        assertEquals(-56f, Inicio.deslize(0f, 70f, w, m, p), 0.01f)
        assertEquals(50f, Inicio.deslize(w - m - 3 * p + 50f, 70f, w, m, p), 0.01f)
    }

    @Test
    fun os_aromas_do_catalogo_tem_desenho() {
        assertEquals(listOf(Aroma.TROPICAL, Aroma.UVA, Aroma.FRUTADO), Aroma.de("fruta tropical, uva fresca, frutado", 3))
        assertEquals(listOf(Aroma.CITRINO, Aroma.TERRA, Aroma.INCENSO), Aroma.de("cítricos, terra, incenso", 3))
        assertEquals(listOf(Aroma.MANGA, Aroma.TROPICAL, Aroma.CREME), Aroma.de("manga, tropical, creme", 3))
    }

    @Test
    fun a_versao_3_do_catalogo_traz_as_fotos_a_ficha_e_a_cena() {
        val crm = Sincronizacao.ler("""{"versao":3,"itens":[],"parafernalia":[{"slug":"cuia-silicone","nome":"Cuia","categoria":"cuias","estado":"venda","preco":4,
            "imagem":"https://x/img/produtos/cuia-silicone-roxo.webp","fotos":[{"cor":"Roxo","imagem":"https://x/img/produtos/cuia-silicone-roxo.webp","estado":"venda"},{"cor":"Vazia","imagem":""}],
            "ficha":[{"rotulo":"Material","valor":"Silicone"}],"cena":null},
            {"slug":"piteira-vidro-fauna-brasileira-6mm","nome":"P","categoria":"piteiras","estado":"venda","preco":15,"imagem":"x","cena":"https://x/video/tv/p.mp4?v=1","cartaz":"https://x/video/tv/p.jpg"}]}""")!!
        val ps = Sincronizacao.parafernalia(crm)
        assertEquals(listOf("Roxo"), ps[0].fotos.map { it.cor })
        assertEquals("Silicone", ps[0].ficha.single().valor)
        assertEquals(false, ps[0].temCena)
        assertEquals("https://x/video/tv/p.mp4?v=1", ps[1].cena)
    }
}
