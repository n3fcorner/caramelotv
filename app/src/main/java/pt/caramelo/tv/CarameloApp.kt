package pt.caramelo.tv

import android.app.Application
import pt.caramelo.tv.data.Atualizacao
import pt.caramelo.tv.data.CatalogRepository

class CarameloApp : Application() {
    val repository: CatalogRepository by lazy { CatalogRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // A app procura versoes novas sozinha (4/10/2026): ver Atualizacao.kt. No build de debug nao, para nao substituir o que se testa.
        if (!BuildConfig.DEBUG) Atualizacao.comecar(this)
    }
}
