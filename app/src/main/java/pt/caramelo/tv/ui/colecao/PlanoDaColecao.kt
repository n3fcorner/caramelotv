package pt.caramelo.tv.ui.colecao

import pt.caramelo.tv.data.model.MenuItem
import pt.caramelo.tv.data.model.Section

/**
 * Como os frascos se arrumam nas prateleiras da colecao (18/11/2026).
 *
 * Uma prateleira por seccao deixava o CBD com dois frascos sozinhos numa tabua inteira e o Hash com tres (o catalogo de
 * hoje e' CBD 2, Infusao 11, Hash 3). Por isso as seccoes pequenas (ate [PEQUENA] frascos) partilham uma prateleira,
 * separadas por uma placa com o nome de cada uma; as grandes ficam com a sua. A ordem das seccoes dentro de uma
 * prateleira e' a de sempre (CBD, Infusao, Hash), e as prateleiras vao da maior para a mais pequena: a de cima e' a que
 * tem mais para ver. Hoje sao duas prateleiras e cabe tudo num ecra, sem a camara a subir e descer.
 *
 * E' logica pura (sem Compose) para se testar sem TV.
 */
data class Troco(val seccao: Section, val itens: List<MenuItem>)

data class Prateleira(val trocos: List<Troco>) {
    val itens: List<MenuItem> get() = trocos.flatMap { it.itens }
}

object PlanoDaColecao {
    const val PEQUENA = 4
    /** Quantos frascos cabem numa prateleira partilhada antes de abrir outra. */
    const val CABEM = 8
    val ORDEM = listOf(Section.CBD, Section.INFUSAO, Section.HASH)

    fun de(itens: List<MenuItem>): List<Prateleira> {
        val porSeccao = ORDEM.map { s -> Troco(s, itens.filter { it.section == s }) }.filter { it.itens.isNotEmpty() }
        val grandes = porSeccao.filter { it.itens.size > PEQUENA }.map { Prateleira(listOf(it)) }
        val partilhadas = mutableListOf<Prateleira>()
        var actual = mutableListOf<Troco>()
        for (t in porSeccao.filter { it.itens.size <= PEQUENA }) {
            if (actual.sumOf { it.itens.size } + t.itens.size > CABEM && actual.isNotEmpty()) {
                partilhadas += Prateleira(actual); actual = mutableListOf()
            }
            actual += t
        }
        if (actual.isNotEmpty()) partilhadas += Prateleira(actual)
        return (grandes + partilhadas).sortedByDescending { it.itens.size }
    }
}
