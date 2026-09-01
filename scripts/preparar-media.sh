#!/usr/bin/env bash
# Prepara os videos da marca para a app: corta a quadrado, tira as marcas de agua dos
# canais fornecedores, aligeira, e gera uma miniatura tratada de cada um.
#
#   ./scripts/preparar-media.sh
#
# Os originais em videos/CBD e videos/Infusao THCP nao sao tocados.
set -uo pipefail
cd "$(dirname "$0")/.."

DESTINO=videos/web
mkdir -p "$DESTINO/media" "$DESTINO/posters"

# --- estudo de enquadramento -------------------------------------------------
#
# Os clipes sao 1080x1920 com duas marcas de agua: o logo do canal no topo (acaba
# por volta de y=350) e a faixa com o nome em baixo (comeca por volta de y=1520).
# Isso deixa uma janela util de 1170px, e um quadrado de 1080 so la cabe com o
# corte entre y=350 e y=440. Confirmado a vista clipe a clipe: a 560 ja entra a
# faixa do nome. Dai o corte fixo em 420 para todos.
CORTE_Y=420

# O instante da miniatura ja varia: uns comecam em preto, noutros o produto so
# aparece a meio. A percentagem foi escolhida a vista para cada um.
#
# O quarto campo apara o arranque: o Critical Berry abre com 8,8s de preto, quase
# metade do clipe. Detectado com `ffmpeg -vf blackdetect`; os outros dez estao limpos.
#
#   ficheiro | slug | % da duracao para a miniatura | segundos a cortar do inicio
CLIPES=$(cat <<'LISTA'
videos/CBD/Grape Cali.mp4|grape-cali|20|0
videos/CBD/LemonTrip.mp4|lemon-trip|60|0
videos/CBD/Mela Sour.mp4|mela-sour|55|0
videos/CBD/Solero Cream.mp4|solero-cream|20|0
videos/Infusao THCP/BlueBerry.mp4|blueberry-small|20|0
videos/Infusao THCP/Bubba Kush.mp4|bubba-kush|20|0
videos/Infusao THCP/CriticalBerry.mp4|critical-berry|70|8.9
videos/Infusao THCP/Smash Cali.mp4|smash-cali|20|0
videos/Infusao THCP/SourLifter.MOV|sour-lifter|20|0
videos/Infusao THCP/Static.mov|static|60|0
videos/Infusao THCP/Sticky Peach.mp4|sticky-peach-piattella|45|0
LISTA
)

# 900 em vez dos 1080 nativos: no leitor o painel do video tem ~900px de altura num
# ecra 1080p, por isso acima disso estava-se a pagar bits por pixeis que ninguem ve.
# O hqdn3d tira o ruido de sensor do macro a mao levantada, que era o que comia
# metade do bitrate, sem esborratar o tricoma — comparado a 1:1 antes de decidir.
RECORTE="crop=1080:1080:0:$CORTE_Y,fps=30,hqdn3d=1.5:1.5:6:6,scale=900:900"
RECORTE_FOTO="crop=1080:1080:0:$CORTE_Y"

echo "==> A preparar 11 clipes (corte quadrado em y=$CORTE_Y)"
echo

while IFS='|' read -r src slug pct apara; do
  [ -f "$src" ] || { echo "  em falta: $src"; continue; }
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$src")
  apara=${apara:-0}
  util=$(awk -v d="$dur" -v a="$apara" 'BEGIN{printf "%.2f", d-a}')
  instante=$(awk -v u="$util" -v a="$apara" -v p="$pct" 'BEGIN{printf "%.2f", a + u*p/100}')
  inicio=""
  [ "$apara" != "0" ] && inicio="-ss $apara"

  # video: quadrado, 30fps (metade dos originais vinha a 60), faststart para
  # o leitor conseguir arrancar sem descarregar o ficheiro todo
  # -nostdin e obrigatorio: sem isso o ffmpeg consome o stdin do ciclo e as linhas
  # seguintes da lista chegam truncadas.
  ffmpeg -y -nostdin -hide_banner -loglevel error $inicio -i "$src" \
    -map 0:v:0 -map "0:a:0?" \
    -vf "$RECORTE" \
    -c:v libx264 -crf 27 -preset slow -profile:v high -pix_fmt yuv420p \
    -c:a aac -b:a 96k -ac 2 \
    -movflags +faststart \
    "$DESTINO/media/$slug.mp4"

  # miniatura: o frame escolhido, mesmo corte, com um tratamento leve
  ffmpeg -y -nostdin -hide_banner -loglevel error -ss "$instante" -i "$src" \
    -vf "$RECORTE_FOTO,eq=contrast=1.06:saturation=1.10:gamma=0.98,unsharp=5:5:0.7,scale=720:720" \
    -frames:v 1 -q:v 4 \
    "$DESTINO/posters/$slug.jpg"

  antes=$(du -m "$src" | cut -f1)
  depois=$(du -m "$DESTINO/media/$slug.mp4" | cut -f1)
  printf "  %-24s %4s MB -> %3s MB\n" "$slug" "$antes" "$depois"
done <<< "$CLIPES"

echo
echo "==> Total"
du -sh "$DESTINO/media" "$DESTINO/posters"
