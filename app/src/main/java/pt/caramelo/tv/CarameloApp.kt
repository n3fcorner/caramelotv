package pt.caramelo.tv

import android.app.Application
import pt.caramelo.tv.data.CatalogRepository

class CarameloApp : Application() {
    val repository: CatalogRepository by lazy { CatalogRepository(this) }
}
