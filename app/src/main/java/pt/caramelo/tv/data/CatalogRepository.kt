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

    val catalog: Flow<Catalog> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY]
        if (raw.isNullOrBlank()) {
            DefaultCatalog.build()
        } else {
            runCatching { json.decodeFromString(Catalog.serializer(), raw) }
                .getOrElse { DefaultCatalog.build() }
        }
    }

    suspend fun update(transform: (Catalog) -> Catalog) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY]?.let {
                runCatching { json.decodeFromString(Catalog.serializer(), it) }.getOrNull()
            } ?: DefaultCatalog.build()
            prefs[KEY] = json.encodeToString(Catalog.serializer(), transform(current))
        }
    }

    suspend fun reset() {
        context.dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("catalog_json")
    }
}
