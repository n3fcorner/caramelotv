package pt.caramelo.tv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta da Caramelo do Brasil, a oficial do manual da marca (30/9/2026, "alinha a app da TV com o manual"):
 * Verde Brasil #00923F, Laranja Caramelo #F58634, Amarelo Canario #FFC20F, Bege #F4EED6, Vermelho Vivo #E13831,
 * Roxo Tropical #7554A3, Azul Real #3374D8, Azul Escuro #072645.
 *
 * O manual poe o azul escuro como fundo e o roxo em blocos, e nunca o roxo a cobrir um ecra inteiro no escuro:
 * o fundo passa a azul escuro e o roxo fica nos mosaicos. Os nomes das variaveis ficaram os de antes para nao
 * mexer no resto da app; os valores sao os do manual (e dois tons derivados, para o foco e para as barras).
 */
val Roxo = Color(0xFF7554A3)          // Roxo Tropical, superficie dos mosaicos
val RoxoClaro = Color(0xFF8A6BB8)     // superficie em foco (o roxo do manual um degrau acima)
val RoxoFundo = Color(0xFF072645)     // Azul Escuro, fundo do ecra
val RoxoProfundo = Color(0xFF041A30)  // barras, letterbox, sombras (o azul escuro um degrau abaixo)

val Amarelo = Color(0xFFFFC20F)       // Amarelo Canario
val Laranja = Color(0xFFF58634)       // Laranja Caramelo
val LaranjaEscuro = Color(0xFFD9701F)
val Verde = Color(0xFF00923F)         // Verde Brasil
val Vermelho = Color(0xFFE13831)      // Vermelho Vivo
val AzulReal = Color(0xFF3374D8)      // Azul Real

val Creme = Color(0xFFF4EED6)         // Bege, cor do texto
val CremeFraco = Color(0xFFC4BCA2)    // texto secundario
val Marinho = Color(0xFF072645)       // Azul Escuro

/** O Verde Brasil aberto o suficiente para ler sobre o azul escuro e o roxo. */
val VerdeClaro = Color(0xFF46C47A)
/** Lilas dos contornos da mascote no fundo — pouco contraste, de proposito. */
val Lilas = Color(0xFFC9B6E8)       // contorno do logo, texto sobre amarelo
