package pt.caramelo.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Abre a montra assim que a TV acaba de arrancar, para ninguem ter de pegar no
 * comando de manha.
 *
 * Nem todas as TVs deixam uma app abrir um ecra a partir do arranque — o Android
 * restringe o arranque de actividades em segundo plano. Nas que nao deixam, a
 * alternativa garantida e declarar esta app como ecra inicial da TV, ou usar a
 * opcao de arranque automatico do proprio fabricante. Ver o README.
 */
class ArranqueAutomatico : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
