package pt.caramelo.tv.data

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
 *
 * Sem janela por cima da montra (5/10/2026, Francisco: "a app da TV nao abre, carrego e nunca abre"): a janela "Instalar" do
 * Android abria sozinha de hora a hora por cima de tudo, e carregar na app trazia-a a frente em vez da montra. Desde o Android 12,
 * quando a propria app e' quem a instalou (o que passa a ser verdade depois da primeira atualizacao por aqui), a instalacao faz-se
 * sem pedir nada (USER_ACTION_NOT_REQUIRED), e so quando a montra nao esta no ecra (Visivel), porque instalar fecha a app. Se o
 * Android ainda assim pedir confirmacao, a janela abre uma vez por versao, nunca de hora a hora.
 */
object Atualizacao {
    private const val BASE = "https://caramelodobrasil.eu/tv/"
    private const val TAG = "Atualizacao"
    const val VERSAO = "versao"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Versao(val versao: Int = 0, val nome: String = "", val sha256: String = "", val tamanho: Long = 0)

    fun comecar(contexto: Context) {
        val app = contexto.applicationContext
        (app as? Application)?.registerActivityLifecycleCallbacks(Visivel)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            delay(60_000)
            while (true) {
                val pronta = runCatching { verificar(app) }.onFailure { Log.w(TAG, "falhou: ${it.message}") }.getOrDefault(false)
                // Com uma versao descarregada a espera de a montra sair do ecra, volta-se a ver de 10 em 10 minutos.
                delay(if (pronta) 10 * 60_000L else 60 * 60_000L)
            }
        }
    }

    /**
     * Quantos ecras da app estao a vista. A instalacao fecha a app, e a TV da TCL nao a deixa reabrir-se sozinha sem a permissao de
     * arranque automatico (5/10/2026, TclAppBoot: "callee doesn't have OP_AUTO_START permission"): por isso so se instala quando a
     * montra nao esta no ecra (a TV em espera, ou outra app a frente). Quem abre a app a seguir ja a encontra atualizada.
     */
    private object Visivel : Application.ActivityLifecycleCallbacks {
        @Volatile var abertos = 0
        override fun onActivityStarted(a: Activity) { abertos++ }
        override fun onActivityStopped(a: Activity) { abertos = maxOf(0, abertos - 1) }
        override fun onActivityCreated(a: Activity, b: Bundle?) {}
        override fun onActivityResumed(a: Activity) {}
        override fun onActivityPaused(a: Activity) {}
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        override fun onActivityDestroyed(a: Activity) {}
    }

    /** true = ha uma versao nova descarregada, a espera de a montra sair do ecra. */
    private fun verificar(app: Context): Boolean {
        val v = json.decodeFromString<Versao>(ler(URL(BASE + "versao.json")).decodeToString())
        if (v.versao <= BuildConfig.VERSION_CODE || v.sha256.length != 64) return false
        val ficheiro = File(app.cacheDir, "atualizacao-${v.versao}.apk")
        if (!ficheiro.exists() || sha256(ficheiro) != v.sha256) {
            app.cacheDir.listFiles { f -> f.name.startsWith("atualizacao-") }?.forEach { it.delete() }
            ficheiro.writeBytes(ler(URL(BASE + "caramelotv.apk")))
            if (sha256(ficheiro) != v.sha256) { ficheiro.delete(); Log.w(TAG, "sha256 nao bate certo"); return false }
        }
        if (Visivel.abertos > 0) { Log.i(TAG, "versao ${v.versao} pronta; instala-se quando a montra sair do ecra"); return true }
        instalar(app, ficheiro, v.versao)
        return false
    }

    private fun instalar(app: Context, apk: File, versao: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            // Uma vez so: o ecra onde se deixa esta app instalar aplicacoes.
            val prefs = app.getSharedPreferences("atualizacao", Context.MODE_PRIVATE)
            if (prefs.getBoolean("permissao-pedida", false)) return
            prefs.edit().putBoolean("permissao-pedida", true).apply()
            runCatching { app.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        val instalador = app.packageManager.packageInstaller
        val parametros = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = instalador.createSession(parametros)
        instalador.openSession(id).use { sessao ->
            sessao.openWrite("caramelotv.apk", 0, apk.length()).use { saida -> apk.inputStream().use { it.copyTo(saida) }; sessao.fsync(saida) }
            val aviso = PendingIntent.getBroadcast(
                app, id, Intent(app, ResultadoDaInstalacao::class.java).putExtra(VERSAO, versao),
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
                // Uma vez por versao: a janela "Instalar" fica por cima de tudo ate alguem carregar, e de hora a hora tapava a montra.
                val versao = intent.getIntExtra(Atualizacao.VERSAO, 0)
                val prefs = contexto.getSharedPreferences("atualizacao", Context.MODE_PRIVATE)
                if (prefs.getInt("pedida", 0) == versao) {
                    // ja se pediu: larga-se esta sessao (senao ficava uma pendurada por hora)
                    runCatching { contexto.packageManager.packageInstaller.abandonSession(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)) }
                    Log.i("Atualizacao", "confirmacao da $versao ja pedida"); return
                }
                prefs.edit().putInt("pedida", versao).apply()
                @Suppress("DEPRECATION")
                val confirmar = (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else intent.getParcelableExtra(Intent.EXTRA_INTENT))
                confirmar?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { contexto.startActivity(it) } }
            }
            PackageInstaller.STATUS_SUCCESS -> Log.i("Atualizacao", "instalada")
            else -> Log.w("Atualizacao", "falhou: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }
}

/**
 * Depois de se atualizar, a app volta a abrir-se (5/10/2026): o Android fecha-a para trocar o ficheiro e nao a reabre. Como o
 * arranque automatico, so funciona se a TV deixar: na TCL da loja e' preciso ligar o "arranque automatico" da app nas definicoes
 * (sem isso o TclAppBoot nem entrega o aviso). Por isso a instalacao so acontece com a montra fora do ecra.
 */
class Reabrir : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runCatching { contexto.startActivity(Intent(contexto, pt.caramelo.tv.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.w("Atualizacao", "nao reabriu: ${it.message}") }
    }
}
