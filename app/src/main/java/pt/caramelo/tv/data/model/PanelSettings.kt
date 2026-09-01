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
     * o URL de cada um dos quinze slots quando o alojamento muda.
     */
    val mediaBaseUrl: String = "http://192.168.1.216:8000",
)
