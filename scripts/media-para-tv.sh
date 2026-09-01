#!/usr/bin/env bash
# Copia os videos e os cartazes para dentro da propria TV, e aponta a app aos
# ficheiros locais. A partir daqui a montra nao depende de nenhum computador.
#
#   ./scripts/media-para-tv.sh 192.168.1.101
#
# Porque o armazenamento interno e nao a pasta externa da app: no Android 11+ os
# ficheiros que o adb escreve em Android/data/<pkg> ficam com dono `shell` e um
# contexto SELinux que a propria app nao consegue ler. Testado nesta TV: dava
# "Permission denied" ate com run-as. O interno resolve, a troco de uma copia
# intermedia por /data/local/tmp, que so o adb consegue escrever.
set -uo pipefail
cd "$(dirname "$0")/.."

TV_IP="${1:-192.168.1.101}"
TV="${TV_IP}:5555"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

PACOTE=pt.caramelo.tv
STAGING=/data/local/tmp/ctv
DESTINO=/data/user/0/$PACOTE/files

adb connect "$TV" >/dev/null 2>&1
adb -s "$TV" get-state >/dev/null 2>&1 || { echo "TV nao ligada em $TV"; exit 1; }

echo "==> A enviar $(du -sh videos/web | cut -f1) para a TV"
adb -s "$TV" shell "rm -rf $STAGING && mkdir -p $STAGING"
adb -s "$TV" push videos/web/media "$STAGING/" | tail -1
adb -s "$TV" push videos/web/posters "$STAGING/" | tail -1

echo "==> A mover para o armazenamento da app"
adb -s "$TV" shell "run-as $PACOTE sh -c 'rm -rf files/media files/posters && cp -r $STAGING/media files/ && cp -r $STAGING/posters files/'"
adb -s "$TV" shell "rm -rf $STAGING"
echo -n "    ficheiros: "
adb -s "$TV" shell "run-as $PACOTE sh -c 'ls files/media | wc -l; ls files/posters | wc -l'" | tr '\n' ' '
echo

echo "==> A apontar a app aos ficheiros locais"
adb -s "$TV" shell am start -a android.intent.action.VIEW \
  -d "caramelotv://base?url=file://$DESTINO" >/dev/null 2>&1
sleep 2
adb -s "$TV" shell am force-stop $PACOTE
adb -s "$TV" shell am start -n $PACOTE/.MainActivity >/dev/null 2>&1

echo
echo "Feito. A montra le do armazenamento da TV — nao precisa de servidor nenhum."
