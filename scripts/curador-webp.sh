#!/usr/bin/env bash
# Os loops do Caramelo curador para a app (18/11/2026). O Flow gera-os sobre magenta e o video-pose.py do site
# recorta-os para WebM VP9 com transparencia (arte/curador/tv-*.webm). O Android nao le a transparencia do VP9,
# por isso passam a WebP animado, como os de antes: 12 fps (um sim, um nao dos 24 do Flow), 480 px de altura,
# preset "drawing". O primeiro fotograma sai em WebP parado para o cartaz (o que se ve enquanto o loop carrega,
# nos testes e abaixo do Android 9).
#
#   ./scripts/curador-webp.sh
#
# Escreve app/src/main/assets/caramelo/curador-<pose>.webp e app/src/main/res/drawable-nodpi/caramelo_curador_<pose>.webp.
set -euo pipefail
cd "$(dirname "$0")/.."
A=app/src/main/assets/caramelo; R=app/src/main/res/drawable-nodpi
mkdir -p "$A" "$R"
for f in arte/curador/tv-*.webm; do
  pose=$(basename "$f" .webm | sed -E 's/^tv-//; s/-[0-9a-f]{8}$//')
  # o descodificador libvpx-vp9 e' o que le o canal alfa; o nativo do ffmpeg deita-o fora
  ffmpeg -v error -y -c:v libvpx-vp9 -i "$f" -vf "fps=12,scale=-2:480:flags=lanczos" \
    -c:v libwebp_anim -preset drawing -quality 70 -compression_level 5 -loop 0 -pix_fmt yuva420p "$A/curador-$pose.webp"
  ffmpeg -v error -y -c:v libvpx-vp9 -i "$f" -vf "scale=-2:480:flags=lanczos" -frames:v 1 \
    -c:v libwebp -quality 85 -pix_fmt yuva420p "$R/caramelo_curador_$pose.webp"
  echo "$pose: $(du -k "$A/curador-$pose.webp" | cut -f1) KB, cartaz $(du -k "$R/caramelo_curador_$pose.webp" | cut -f1) KB"
done
