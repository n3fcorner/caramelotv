package pt.caramelo.tv

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section
import pt.caramelo.tv.ui.colecao.PlanoDaColecao

class PlanoDaColecaoTest {
    private fun itens(s: Section, n: Int) = (1..n).map { MenuItem(id = "${s.name}-$it", slot = it, section = s, title = "${s.name} $it") }

    @Test
    fun o_catalogo_de_hoje_cabe_em_duas_prateleiras() {
        // CBD 2, Infusao 11, Hash 3 (18/11/2026): a Infusao em cima, o CBD e o Hash juntos em baixo, por esta ordem.
        val p = PlanoDaColecao.de(itens(Section.INFUSAO, 11) + itens(Section.CBD, 2) + itens(Section.HASH, 3))
        assertEquals(2, p.size)
        assertEquals(listOf(Section.INFUSAO), p[0].trocos.map { it.seccao })
        assertEquals(listOf(Section.CBD, Section.HASH), p[1].trocos.map { it.seccao })
        assertEquals(16, p.sumOf { it.itens.size })
    }

    @Test
    fun seccoes_grandes_ficam_com_a_sua_e_as_vazias_nao_aparecem() {
        val p = PlanoDaColecao.de(itens(Section.CBD, 6) + itens(Section.INFUSAO, 9))
        assertEquals(listOf(listOf(Section.INFUSAO), listOf(Section.CBD)), p.map { pr -> pr.trocos.map { it.seccao } })
    }

    @Test
    fun pequenas_que_nao_cabem_juntas_abrem_outra_prateleira() {
        val p = PlanoDaColecao.de(itens(Section.CBD, 4) + itens(Section.HASH, 4) + itens(Section.INFUSAO, 4))
        assertEquals(8, p[0].itens.size)
        assertEquals(2, p.size)
    }
}
