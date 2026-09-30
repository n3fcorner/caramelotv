#!/usr/bin/env bash
# Compila e instala o CarameloTV numa Google TV pela rede.
#
#   ./scripts/install-tv.sh 192.168.1.100
#
# Requer que a TV tenha a depuracao ADB ligada (ver README).
set -euo pipefail

TV_IP="${1:-192.168.1.100}"
TV="${TV_IP}:5555"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

cd "$(dirname "$0")/.."

echo "==> Compilar"
# Release e nao debug: no Compose a versao debug e varias vezes mais lenta, e numa TCL
# nota-se em cada movimento do foco.
./gradlew --quiet assembleRelease
APK="app/build/outputs/apk/release/app-release.apk"

echo "==> Ligar a $TV"
if ! adb connect "$TV" | grep -qE "connected to"; then
    echo
    echo "Nao consegui ligar a $TV."
    echo "Na TV: Definicoes > Sistema > Acerca de > 7 cliques em Compilacao,"
    echo "depois Sistema > Opcoes de programador > Depuracao ADB / por rede."
    echo
    echo "Se a TV mostrar 'Depuracao sem fios' com um codigo de emparelhamento,"
    echo "usa primeiro:  adb pair ${TV_IP}:<porta-mostrada-na-TV>"
    exit 1
fi

# Aceitar o aviso de autorizacao que aparece no ecra da TV, se for a primeira vez.
adb -s "$TV" wait-for-device

echo "==> Instalar"
adb -s "$TV" install -r "$APK"

echo "==> Arrancar"
adb -s "$TV" shell am start -n pt.caramelo.tv/.MainActivity

echo
echo "Feito. A app fica tambem na linha de aplicacoes do ecra inicial da Google TV."
