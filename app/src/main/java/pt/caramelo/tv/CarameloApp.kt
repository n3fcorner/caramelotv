package pt.caramelo.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import pt.caramelo.tv.data.Atualizacao
import pt.caramelo.tv.data.CatalogRepository

class CarameloApp : Application(), ImageLoaderFactory {
    val repository: CatalogRepository by lazy { CatalogRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // A app procura versoes novas sozinha (4/10/2026): ver Atualizacao.kt. No build de debug nao, para nao substituir o que se testa,
        // nem na variante "nova", que esta ao lado da antiga (a atualizacao do site e' a antiga).
        if (!BuildConfig.DEBUG && BuildConfig.ATUALIZA) Atualizacao.comecar(this)
    }

    /**
     * As fotos dos produtos (18/11/2026, revisao tecnica). Por omissao o Coil guarda na memoria um quarto do que a app pode
     * usar, e a TCL da loja so tem ~65 MB livres com a app aberta: 24 MB chegam para as fotos dos frascos e das pecas ao
     * tamanho a que se desenham. No disco ficam 150 MB, sem olhar aos cabecalhos de cache do site, para as fotos nao voltarem
     * a descer a cada arranque.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizeBytes(24 * 1024 * 1024).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("imagens")).maxSizeBytes(150L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .crossfade(false)
        .build()
}
