package pt.caramelo.tv.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

/**
 * Os videos guardados na propria TV (18/11/2026, revisao tecnica da app nova). Ate aqui cada video das flores e das
 * pecas vinha do site sempre que tocava: na montra automatica, o mesmo clip descarregava-se dezenas de vezes por dia,
 * e o arranque de cada um esperava pela rede. Com a cache, a primeira vez descarrega-se e as outras leem-se do disco.
 *
 * 256 MB chegam para os ~16 videos das flores (1 a 8 MB) e os ~20 de rotacao (0,5 a 1 MB), com folga; o mais antigo
 * sai primeiro. Uma so cache para a app inteira: o SimpleCache nao admite duas instancias na mesma pasta.
 */
@OptIn(UnstableApi::class)
object CacheDeMedia {
    private const val TAMANHO = 256L * 1024 * 1024
    @Volatile private var cache: SimpleCache? = null

    private fun cacheDe(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "videos"),
            LeastRecentlyUsedCacheEvictor(TAMANHO),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }

    /** Um ExoPlayer que le os videos pela cache: o que se usa em todos os ecras. */
    fun leitor(context: Context): ExoPlayer {
        val fonte = CacheDataSource.Factory()
            .setCache(cacheDe(context))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context.applicationContext))
            // Se a cache falhar (disco cheio, ficheiro estragado), le-se do site como antes, sem erro no ecra.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(fonte))
            .build()
    }
}
