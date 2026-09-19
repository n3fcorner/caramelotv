#!/usr/bin/env bash
# Prepara os videos da marca para a app: corta a quadrado, tira as marcas de agua dos
# canais fornecedores, aligeira, e gera uma miniatura tratada de cada um.
#
#   ./scripts/preparar-media.sh
#
# Os originais em videos/CBD e videos/Infusao THCP nao sao tocados.
#
# Os cartazes que este script tira do video sao o ponto de partida. Por cima vao
# ainda dois passos, e sao chamados no fim daqui para nao haver maneira de os
# saltar — ja aconteceu, e o resultado foi meia prateleira com fotogramas de
# video no meio dos recortes:
#
#   cartazes-de-estudio.sh   poe a foto do fornecedor onde ela existe (treze)
#   recortar-flores.py       recorta as outras quatro do proprio video
set -uo pipefail
cd "$(dirname "$0")/.."

DESTINO=videos/web
mkdir -p "$DESTINO/media" "$DESTINO/posters"

# --- estudo de enquadramento -------------------------------------------------
#
# Os clipes do fornecedor sao 1080x1920 com duas marcas de agua: o logo do canal no
# topo (acaba por volta de y=350) e a faixa com o nome em baixo (comeca por volta de
# y=1520). Isso deixa uma janela util de 1170px, e um quadrado de 1080 so la cabe com
# o corte entre y=350 e y=440. Confirmado a vista clipe a clipe: a 560 ja entra a
# faixa do nome. Dai o corte fixo em 420 para esses.
CORTE_Y=420
CORTE_VERTICAL="1080:1080:0:$CORTE_Y"

# Nem tudo vem do fornecedor. O Leo tambem filma com o telemovel, e ai o material e
# 4K ao alto: 2160x3840, sem marca de agua nenhuma. ATENCAO: o ffmpeg aplica a
# rotacao do ficheiro ao descodificar, por isso o crop ve sempre o quadro ja ao
# alto (2160 de largura), mesmo quando o ffprobe diz 3840x2160 com rotate=-90
# (o Uranium). A largura toda e a altura escolhida a vista: e' isso o corte por
# clipe. Um crop que saia do quadro nao da erro, o ffmpeg empurra-o para dentro,
# e foi assim que o Uranium andou meses com um zoom no canto.
CORTE_4K="2160:2160:0:800"

# O instante da miniatura ja variava: uns comecam em preto, noutros o produto so
# aparece a meio. A percentagem foi escolhida a vista para cada um.
#
# O quarto campo apara o arranque: o Critical Berry infundido abre com 8,8s de preto,
# quase metade do clipe. Detectado com `ffmpeg -vf blackdetect`; os outros estao
# limpos, incluindo os quatro que entraram em setembro.
#
# O sexto campo limita a duracao. So o Uranium precisa: e material cru, com a mao a
# entrar e a sair do enquadramento nas pontas.
#
# O quarto campo tambem serve para acertar o enquadramento, e nao so para tirar
# preto: a Berry Pie abre com a cabeca pequena ao fundo de uma parede branca e so
# aproxima a meio do clipe. Aparados os primeiros 10,8s, o que fica e' a parte em
# que o produto enche o quadro, como nos outros. Nao ha maneira de AFASTAR onde
# esta colado de mais — o clipe ja usa a largura toda do original, e alargar era
# deixar entrar a marca de agua. E' o caso do Sticky Peach, que fica macro.
#
# O quinto campo, quando preenchido nos clipes do fornecedor, e' o "acerto de
# camara" medido a 17/9/2026: o quadrado mais fechado que contem o produto em
# 85% dos fotogramas. Deu quase sempre a largura toda (1060-1080) — o fornecedor
# filma colado — e so a altura varia, entre 394 e 460. E' pouco, mas e' medido.
#
# A Mango Melt e' o caso extremo: dos 91s do clipe, os primeiros 46 sao a abrir a
# embalagem — celofane, tesoura, papel — e o hash so aparece depois. Aparados,
# sobram 45s de produto. A Pulsar e' o mesmo padrao em pequeno: dos 14,2s, os
# primeiros 5 tem a cabeca pequena ao fundo do enquadramento e so depois a camara
# aproxima. Aparados, ficam 9,2s em que o produto enche o quadro.
#
#   ficheiro | slug | % da duracao para a miniatura | apara o inicio | corte | duracao
CLIPES=$(cat <<'LISTA'
videos/CBD/Grape Cali.mp4|grape-cali|20|0||
videos/CBD/LemonTrip.mp4|lemon-trip|60|0|1060:1060:9:460|
videos/CBD/Mela Sour.mp4|mela-sour|55|0|2160:2160:0:800|
videos/CBD/Solero Cream.mp4|solero-cream|20|0||
videos/CBD/Critical Berry.mp4|critical-berry-cbd|45|0||
videos/CBD/Uranium.MOV|uranium|25|3.2|2160:2160:0:1000|9
videos/Infusao THCP/BlueBerry.mp4|blueberry-small|20|0||
videos/Infusao THCP/Bubba Kush.mp4|bubba-kush|20|0||
videos/Infusao THCP/CriticalBerry.mp4|critical-berry|70|8.9||
videos/Infusao THCP/Smash Cali.mp4|smash-cali|20|0|1080:1080:0:402|
videos/Infusao THCP/SourLifter.MOV|sour-lifter|20|0|1080:1080:0:394|
videos/Infusao THCP/Static.mov|static|60|0||
videos/Infusao THCP/Sticky Peach.mp4|sticky-peach-piattella|45|0||
videos/Infusao THCP/White Banger.mp4|white-banger|80|0||
videos/Infusao THCP/Gorilla Mandarini.MOV|gorilla-mandarini|40|0|2160:2160:840:0|
videos/Infusao THCP/Mango Melt.mp4|mango-melt|40|46||
videos/Infusao THCP/Berry Pie.mp4|berry-pie|60|10.8||
videos/Infusao THCP/Pulsar.mp4|pulsar|55|5.0|1080:1080:0:418|
videos/Infusao THCP/Sky.mp4|sky|68|0|1080:1080:0:394|
videos/Infusao THCP/Peanut Butter.mp4|peanut-butter|44|0|1080:1080:0:435|
videos/Hash/Papaya OG.mp4|papaya-og|32|0|1080:1080:0:440|
videos/Hash/La Mousse.mp4|la-mousse|56|0|1080:1080:0:394|
videos/Hash/OG Ivory Dry.mp4|og-ivory-dry|56|0|1060:1060:9:460|
LISTA
)

# 900 em vez dos 1080 nativos: no leitor o painel do video tem ~900px de altura num
# ecra 1080p, por isso acima disso estava-se a pagar bits por pixeis que ninguem ve.
# O hqdn3d tira o ruido de sensor do macro a mao levantada, que era o que comia
# metade do bitrate, sem esborratar o tricoma — comparado a 1:1 antes de decidir.
CADEIA="fps=30,hqdn3d=1.5:1.5:6:6,scale=900:900"

# Com slugs na linha de comando so se refazem esses:  preparar-media.sh uranium mela-sour
SO="$*"

total=$(grep -c . <<< "$CLIPES")
echo "==> A preparar $total clipes"
echo

while IFS='|' read -r src slug pct apara corte limite; do
  [ -n "$src" ] || continue
  if [ -n "$SO" ] && ! grep -qw -- "$slug" <<< "$SO"; then continue; fi
  [ -f "$src" ] || { echo "  em falta: $src"; continue; }
  corte=${corte:-$CORTE_VERTICAL}
  [ -n "$corte" ] || corte=$CORTE_VERTICAL
  apara=${apara:-0}

  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$src")
  util=$(awk -v d="$dur" -v a="$apara" 'BEGIN{printf "%.2f", d-a}')
  [ -n "$limite" ] && util=$limite
  instante=$(awk -v u="$util" -v a="$apara" -v p="$pct" 'BEGIN{printf "%.2f", a + u*p/100}')

  inicio=""
  [ "$apara" != "0" ] && inicio="-ss $apara"
  fim=""
  [ -n "$limite" ] && fim="-t $limite"

  # video: quadrado, 30fps (metade dos originais vinha a 60), faststart para
  # o leitor conseguir arrancar sem descarregar o ficheiro todo
  # -nostdin e obrigatorio: sem isso o ffmpeg consome o stdin do ciclo e as linhas
  # seguintes da lista chegam truncadas.
  ffmpeg -y -nostdin -hide_banner -loglevel error $inicio -i "$src" $fim \
    -map 0:v:0 -map "0:a:0?" \
    -vf "crop=$corte,$CADEIA" \
    -c:v libx264 -crf 27 -preset slow -profile:v high -pix_fmt yuv420p \
    -c:a aac -b:a 96k -ac 2 \
    -movflags +faststart \
    "$DESTINO/media/$slug.mp4"

  # miniatura: o frame escolhido, mesmo corte, com um tratamento leve
  ffmpeg -y -nostdin -hide_banner -loglevel error -ss "$instante" -i "$src" \
    -vf "crop=$corte,eq=contrast=1.06:saturation=1.10:gamma=0.98,unsharp=5:5:0.7,scale=720:720" \
    -frames:v 1 -q:v 4 \
    "$DESTINO/posters/$slug.jpg"

  antes=$(du -m "$src" | cut -f1)
  depois=$(du -m "$DESTINO/media/$slug.mp4" | cut -f1)
  printf "  %-24s %4s MB -> %3s MB\n" "$slug" "$antes" "$depois"
done <<< "$CLIPES"

# Os cartazes de estudio (WeNeed) tapam o fotograma que o ciclo acima escreveu
# para esses produtos, por isso correm sempre; os recortes por Vision so na
# passagem completa, que demoram.
echo
echo "==> Cartazes"
./scripts/cartazes-de-estudio.sh
[ -z "$SO" ] && ./scripts/recortar-flores.py

echo
echo "==> Total"
du -sh "$DESTINO/media" "$DESTINO/posters"
