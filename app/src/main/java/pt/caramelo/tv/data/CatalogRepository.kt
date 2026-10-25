package pt.caramelo.tv.data

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import pt.caramelo.tv.data.model.Catalog

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "caramelo_catalog")

/**
 * Guarda o catalogo inteiro como um unico blob JSON no DataStore. Sao 15 itens: nao
 * justifica uma base de dados, e assim o estado sobrevive a force-stop e a reinicios da TV.
 */
class CatalogRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * A pasta interna da app, que e o alojamento por omissao dos media.
     *
     * O caminho nao pode ser constante: depende do utilizador do Android. Sai
     * daqui porque o [PanelSettings] e um data class serializavel e nao ve o
     * Context.
     */
    private val baseLocal: String get() = "file://" + context.filesDir.absolutePath

    /** Base vazia = os ficheiros da propria TV. */
    private fun Catalog.comBase(): Catalog =
        if (settings.mediaBaseUrl.isBlank()) {
            copy(settings = settings.copy(mediaBaseUrl = baseLocal))
        } else {
            this
        }

    /**
     * O que esta guardado, com os itens do codigo por cima quando o codigo mudou.
     * Os produtos sao dados de fabrica (DefaultCatalog); o que a TV guarda de seu
     * sao as definicoes, as posicoes e o ultimo visto. Sem isto, a primeira
     * gravacao congelava os produtos tal como estavam nesse dia, e uma instalacao
     * com terpenos novos nao mostrava nada de novo.
     */
    private fun guardado(raw: String?): Catalog {
        val fabrica = DefaultCatalog.build()
        if (raw.isNullOrBlank()) return fabrica
        val lido = runCatching { json.decodeFromString(Catalog.serializer(), raw) }.getOrNull() ?: return fabrica
        return if (lido.defaultsFingerprint == fabrica.defaultsFingerprint) lido
        else lido.copy(items = fabrica.items, defaultsFingerprint = fabrica.defaultsFingerprint)
    }

    /**
     * Com catalogo do CRM guardado, os itens sao os dele, com a ficha de fabrica por cima
     * (ver [Sincronizacao]). As definicoes, as posicoes e o ultimo visto continuam a ser
     * da TV. Sem CRM, tudo como antes.
     */
    private fun Catalog.comCrm(raw: String?): Catalog {
        val crm = Sincronizacao.ler(raw) ?: return this
        if (settings.crmUrl.isBlank()) return this
        // A parafernalia vem sempre do CRM, nunca da fabrica nem do que esta gravado: enche-se a cada leitura.
        val comParaf = copy(parafernalia = Sincronizacao.parafernalia(crm))
        val itens = Sincronizacao.juntar(crm, fabrica)
        if (itens.isEmpty()) return comParaf
        // O que se editou na propria TV (titulo, video, imagem em Definicoes > Editar) continua por
        // cima do CRM: sem isto o Guardar nao fazia nada assim que havia catalogo do CRM (auditoria 29/9).
        val porId = fabrica.associateBy { it.id }
        val editados = items.filter { s -> porId[s.id]?.let { f -> f.title != s.title || f.videoUrl != s.videoUrl || f.imageUrl != s.imageUrl } ?: true }.associateBy { it.id }
        return comParaf.copy(items = itens.map { i -> editados[i.id]?.let { e -> i.copy(title = e.title, videoUrl = e.videoUrl, imageUrl = e.imageUrl) } ?: i })
    }

    private val fabrica by lazy { DefaultCatalog.build().items }

    // Fora da linha principal: juntar o CRM com a fabrica descodifica JSON e percorre o catalogo, e
    // corre a cada escrita (a posicao do video ao sair do leitor, cada sincronizacao que mudou algo).
    val catalog: Flow<Catalog> = context.dataStore.data.map { prefs -> guardado(prefs[KEY]).comCrm(prefs[KEY_CRM]).comBase() }.flowOn(Dispatchers.Default)

    /** Grava o catalogo do CRM so se mudou; devolve se mudou. */
    suspend fun gravarCrm(canonico: String): Boolean {
        var mudou = false
        context.dataStore.edit { prefs -> if (prefs[KEY_CRM] != canonico) { prefs[KEY_CRM] = canonico; mudou = true } }
        return mudou
    }

    suspend fun update(transform: (Catalog) -> Catalog) {
        context.dataStore.edit { prefs ->
            prefs[KEY] = json.encodeToString(Catalog.serializer(), transform(guardado(prefs[KEY])))
        }
    }

    suspend fun reset() {
        context.dataStore.edit { it.remove(KEY); it.remove(KEY_CRM) }
    }

    private companion object {
        val KEY = stringPreferencesKey("catalog_json")
        val KEY_CRM = stringPreferencesKey("crm_json")
    }
}
