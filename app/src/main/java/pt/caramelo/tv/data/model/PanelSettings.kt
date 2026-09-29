package pt.caramelo.tv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PanelSettings(
    /** Arranca o video logo ao abrir os detalhes, sem passar pelo botao Reproduzir. */
    val autoplay: Boolean = false,
    /** Guarda a posicao de cada video e oferece Retomar. */
    val resumePlayback: Boolean = true,
    /** Repete o clipe ao chegar ao fim quando alguem o escolheu a mao. */
    val loopPlayback: Boolean = true,
    /**
     * Segundos de comando parado no menu antes de a montra comecar a passar os
     * produtos sozinha. Zero desliga.
     */
    val attractSeconds: Int = 60,
    /**
     * Prefixo dos caminhos relativos dos itens. Muda-se aqui uma vez em vez de reescrever
     * o URL de cada um dos dezoito slots quando o alojamento muda.
     *
     * **Vazio quer dizer "os ficheiros da propria TV"**, e o [CatalogRepository]
     * preenche-o com a pasta interna da app. Esteve aqui um IP fixo do Mac de
     * quem a fez: bastava uma instalacao limpa ou um `caramelotv://reset` para a
     * montra da loja voltar a depender de um portatil ligado na mesma rede — e
     * ficar sem videos quando ele nao estava.
     */
    val mediaBaseUrl: String = "",
    /** Lingua da interface e das fichas: "pt" ou "en" (29/9/2026). */
    val idioma: String = "pt",
)
