package pt.caramelo.tv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta da Caramelo do Brasil, tirada do logo e dos banners da marca.
 *
 * O roxo do banner (#573B89) e forte de mais para preencher um ecra de TV inteiro
 * numa sala escura, por isso serve de superficie e nao de fundo: o fundo usa dois
 * tons mais fundos da mesma familia.
 */
val Roxo = Color(0xFF6A4D99)          // roxo da marca, superficie dos mosaicos
// Os quatro roxos subiram um degrau (gama 0,82) em Setembro de 2026, com o fundo
// novo: a arte assenta num roxo mais escuro que o antigo, e a paleta de cima tinha
// de subir com ele para os mosaicos nao afundarem no fundo.
val RoxoClaro = Color(0xFF7D5DB2)     // superficie em foco
val RoxoFundo = Color(0xFF3F2D5F)     // fundo do ecra
val RoxoProfundo = Color(0xFF2C2044)  // barras, letterbox, sombras

val Amarelo = Color(0xFFFFBE00)       // amarelo do logo
val Laranja = Color(0xFFFF9100)
val LaranjaEscuro = Color(0xFFFE7F02)
val Verde = Color(0xFF0E8F35)         // verde da folha

val Creme = Color(0xFFF5EED1)         // creme do logo, cor do texto
val CremeFraco = Color(0xFFBFB49A)    // texto secundario
val Marinho = Color(0xFF002448)

/** Verde da folha do logo, aberto o suficiente para ler sobre o roxo. */
val VerdeClaro = Color(0xFF46C47A)
/** Lilas dos contornos da mascote no fundo — pouco contraste, de proposito. */
val Lilas = Color(0xFFC9B6E8)       // contorno do logo, texto sobre amarelo
