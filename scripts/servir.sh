#!/usr/bin/env bash
# Serve os videos, os cartazes e o APK na rede local, com suporte a Range para o
# leitor poder avancar no video sem voltar a descarregar tudo.
#
#   ./scripts/servir.sh [porta]
#
# Depois aponta a app para este servidor:
#   adb shell am start -a android.intent.action.VIEW \
#     -d "caramelotv://base?url=http://<IP-DESTA-MAQUINA>:8000"
set -euo pipefail
cd "$(dirname "$0")/.."

PORTA="${1:-8000}"
RAIZ="$(pwd)/videos/web"

[ -d "$RAIZ" ] || { echo "Corre primeiro ./scripts/preparar-media.sh"; exit 1; }

# O APK fica ao lado dos media, para a TV o poder descarregar pelo mesmo endereco.
APK="app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] && cp "$APK" "$RAIZ/tv.apk"

IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "?")
echo "media    http://$IP:$PORTA/media/<nome>.mp4"
echo "cartazes http://$IP:$PORTA/posters/<nome>.jpg"
echo "apk      http://$IP:$PORTA/tv.apk"
echo
echo "O IP muda de rede para rede. Aponta a app a este servidor com:"
echo
echo "  adb shell am start -a android.intent.action.VIEW \\"
echo "    -d \"caramelotv://base?url=http://$IP:$PORTA\""
echo
echo "A TV tem de estar na MESMA rede que esta maquina."
echo
exec python3 scripts/servidor.py "$RAIZ" "$PORTA"
