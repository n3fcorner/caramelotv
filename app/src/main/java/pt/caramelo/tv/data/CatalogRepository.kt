package pt.caramelo.tv.data

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

    val catalog: Flow<Catalog> = context.dataStore.data.map { prefs -> guardado(prefs[KEY]).comBase() }

    suspend fun update(transform: (Catalog) -> Catalog) {
        context.dataStore.edit { prefs ->
            prefs[KEY] = json.encodeToString(Catalog.serializer(), transform(guardado(prefs[KEY])))
        }
    }

    suspend fun reset() {
        context.dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("catalog_json")
    }
}
