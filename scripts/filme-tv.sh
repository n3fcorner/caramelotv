#!/usr/bin/env bash
# Faz um video da app da TV sem TV (22/11/2026, Francisco: "da-me um video para mostrar, nao estou na loja ja").
#
#   ./scripts/filme-tv.sh                 # fica em entrega/tv-nova.mp4
#   ./scripts/filme-tv.sh outro-nome.mp4
#
# A app desenha-se no computador, fotograma a fotograma (app/src/test/.../FilmeTest.kt), com o catalogo que a loja tem
# nesse momento, as fotos do site e os fotogramas dos videos verdadeiros; o ffmpeg junta tudo. Sao uns 45 segundos:
# as flores e o leitor, as piteiras, a parafernalia e as analises. Para mudar o percurso, o guiao esta no fim do FilmeTest.
#
# Precisa de curl, ffmpeg e python3 com o Pillow. Demora uns 5 minutos; o que descarrega fica em app/build/filme/cache
# (apaga-se essa pasta para ir buscar o catalogo e as fotos de novo).
set -euo pipefail
cd "$(dirname "$0")/.."

SAIDA="${1:-entrega/tv-nova.mp4}"
mkdir -p "$(dirname "$SAIDA")"

echo "==> Desenhar os fotogramas"
FILME=1 ./gradlew --quiet :app:testDebugUnitTest --tests '*FilmeTest*' -Proborazzi.test.record=true

N=$(ls app/build/filme/quadros | wc -l | tr -d ' ')
[ "$N" -gt 0 ] || { echo "Nao saiu fotograma nenhum: ver app/build/reports/tests."; exit 1; }

echo "==> Juntar $N fotogramas"
# 25 por segundo, como o relogio do FilmeTest. H.264 em yuv420p: abre no telemovel, no WhatsApp e no QuickTime.
ffmpeg -loglevel error -y -framerate 25 -i app/build/filme/quadros/%05d.png \
    -c:v libx264 -preset slow -crf 20 -pix_fmt yuv420p -movflags +faststart "$SAIDA"

echo
echo "Feito: $SAIDA ($(du -h "$SAIDA" | cut -f1), $((N / 25)) s)"
