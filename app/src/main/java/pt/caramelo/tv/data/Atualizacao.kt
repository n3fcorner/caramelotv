package pt.caramelo.tv.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pt.caramelo.tv.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * A app atualiza-se sozinha (4/10/2026, Francisco: "nao e' suposto cada vez que faco uma alteracao ter este trabalho, e' suposto
 * ela mudar sozinha"). O workflow apk do repositorio, no main, compila a release (versionCode = numero da compilacao) e manda-a
 * para o site; aqui, um minuto depois do arranque e depois de hora a hora, pergunta-se a /tv/versao.json se ha uma versao maior
 * do que esta. Havendo, descarrega-se /tv/caramelotv.apk para a pasta da app, confere-se o sha256 e entrega-se ao instalador do
 * Android. A Google TV mostra entao uma janela "Instalar": e' o unico gesto que fica, e o Android nao deixa tira-lo.
 *
 * Da primeira vez a TV pede tambem para deixar esta app instalar aplicacoes (Definicoes > Apps > Acesso especial); a app abre
 * esse ecra sozinha. Sem rede, sem versao nova ou com o ficheiro errado, nao acontece nada e tenta-se na hora seguinte.
 */
object Atualizacao {
    private const val BASE = "https://caramelodobrasil.eu/tv/"
    private const val TAG = "Atualizacao"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Versao(val versao: Int = 0, val nome: String = "", val sha256: String = "", val tamanho: Long = 0)

    fun comecar(contexto: Context) {
        val app = contexto.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            delay(60_000)
            while (true) {
                runCatching { verificar(app) }.onFailure { Log.w(TAG, "falhou: ${it.message}") }
                delay(60 * 60_000L)
            }
        }
    }

    private fun verificar(app: Context) {
        val v = json.decodeFromString<Versao>(ler(URL(BASE + "versao.json")).decodeToString())
        if (v.versao <= BuildConfig.VERSION_CODE || v.sha256.length != 64) return
        val ficheiro = File(app.cacheDir, "atualizacao-${v.versao}.apk")
        if (!ficheiro.exists() || sha256(ficheiro) != v.sha256) {
            app.cacheDir.listFiles { f -> f.name.startsWith("atualizacao-") }?.forEach { it.delete() }
            ficheiro.writeBytes(ler(URL(BASE + "caramelotv.apk")))
            if (sha256(ficheiro) != v.sha256) { ficheiro.delete(); Log.w(TAG, "sha256 nao bate certo"); return }
        }
        instalar(app, ficheiro)
    }

    private fun instalar(app: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            // Uma vez so: o ecra onde se deixa esta app instalar aplicacoes.
            app.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val instalador = app.packageManager.packageInstaller
        val parametros = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(app.packageName) }
        val id = instalador.createSession(parametros)
        instalador.openSession(id).use { sessao ->
            sessao.openWrite("caramelotv.apk", 0, apk.length()).use { saida -> apk.inputStream().use { it.copyTo(saida) }; sessao.fsync(saida) }
            val aviso = PendingIntent.getBroadcast(
                app, id, Intent(app, ResultadoDaInstalacao::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0),
            )
            sessao.commit(aviso.intentSender)
        }
    }

    private fun ler(url: URL): ByteArray {
        val c = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 60_000; useCaches = false }
        try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes() }
        } finally { c.disconnect() }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { entrada -> val buf = ByteArray(64 * 1024); while (true) { val n = entrada.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

/** O instalador responde aqui: quando precisa da confirmacao de quem esta na TV, abre a janela "Instalar". */
class ResultadoDaInstalacao : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmar = (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else intent.getParcelableExtra(Intent.EXTRA_INTENT))
                confirmar?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { contexto.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> Log.i("Atualizacao", "instalada")
            else -> Log.w("Atualizacao", "falhou: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }
}
