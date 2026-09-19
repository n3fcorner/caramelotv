#!/usr/bin/env bash
# Versoes leves dos clipes para a montra no telemovel. O ecra de um iPhone nao
# tira partido dos 900px da TV, e quem abre o link fora de casa esta em dados
# moveis — por isso 640x640 e um bitrate mais baixo.
set -uo pipefail
cd "$(dirname "$0")/.."
DESTINO=videos/web-movel
mkdir -p "$DESTINO"

# Com slugs na linha de comando so se refazem esses:  preparar-web.sh uranium mela-sour
SO="$*"

for f in videos/web/media/*.mp4; do
  slug=$(basename "$f" .mp4)
  if [ -n "$SO" ] && ! grep -qw -- "$slug" <<< "$SO"; then continue; fi
  ffmpeg -y -nostdin -hide_banner -loglevel error -i "$f" \
    -map 0:v:0 -map "0:a:0?" \
    -vf "scale=640:640" \
    -c:v libx264 -crf 30 -preset slow -profile:v high -pix_fmt yuv420p \
    -c:a aac -b:a 80k -ac 2 -movflags +faststart \
    "$DESTINO/$slug.mp4"
  printf "  %-26s %3s MB -> %3s MB\n" "$slug" \
    "$(du -m "$f" | cut -f1)" "$(du -m "$DESTINO/$slug.mp4" | cut -f1)"
done
echo
du -sh "$DESTINO"

# Posters em WebP a 720px: um terco do peso do JPEG para o mesmo cartao. E tudo
# o que a montra do telemovel usa vai parar a web/media, pronto a publicar.
mkdir -p web/media/posters
for j in videos/web/posters/*.jpg; do
  cwebp -quiet -q 82 -m 6 "$j" -o "web/media/posters/$(basename "$j" .jpg).webp"
done
rm -f web/media/posters/*.jpg
cp "$DESTINO"/*.mp4 web/media/
echo "web/media: $(ls web/media/*.mp4 | wc -l | tr -d ' ') videos, $(ls web/media/posters/*.webp | wc -l | tr -d ' ') posters"
