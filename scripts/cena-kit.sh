#!/usr/bin/env bash
# A cena de um kit para a montra automatica: as animacoes dos tres temas do kit,
# uma a seguir a outra, com as tres piteiras do kit a rodar por cima.
#
#   ./scripts/cena-kit.sh atletas
#   RASCUNHO=1 ./scripts/cena-kit.sh atletas    # sem os loops do Gemini, com a arte parada
#
# Precisa dos tres loops em videos/loops/<tema>-16x9.mp4 (tirados pelo
# fechar-loop.sh). Com RASCUNHO=1, um tema sem loop entra com a arte original a
# deslizar devagar: serve para ver o enquadramento antes de haver Gemini.
#
# Sai videos/loops/cena-<kit>.mp4, 1920x1080, mudo, e o cartaz cena-<kit>.jpg.
#
# Como fecha. Cada tema dura L fotogramas e ja fecha sozinho. Os tres cruzam-se
# X fotogramas na passagem, e o terceiro cruza de volta para o primeiro, como no
# fechar-loop.sh: a cena tem 3L-3X fotogramas e o ultimo e' vizinho do primeiro.
# As piteiras dao uma volta por tema, L-X fotogramas cada, por isso tambem
# acabam onde comecaram. Com L=168 e X=12 (a 24 fps): 19,5 s de cena, 6,5 s por
# volta, perto dos 6 s dos giros da prateleira.
set -euo pipefail
cd "$(dirname "$0")/.."

KIT="${1:?uso: cena-kit.sh <3-tons|artes|atletas|fauna|favela>}"
case "$KIT" in
  3-tons)  TEMAS="filtro-de-barro vira-lata-caramelo pastel-de-feira" ;;
  artes)   TEMAS="essencia-do-brasil pixacao-estilo-brasileiro coracao-pulsa-arte" ;;
  atletas) TEMAS="formula-1 futebol-do-brasil surfando-a-onda-boa" ;;
  fauna)   TEMAS="flora-brasileira fauna-brasileira brasil-indigena" ;;
  favela)  TEMAS="mosaico-da-favela retratos-da-favela cores-do-ceu-da-favela" ;;
  *) echo "kit desconhecido: $KIT"; exit 1 ;;
esac

FPS=24
L="${L:-168}"          # fotogramas de cada tema (7 s)
X="${X:-12}"           # fotogramas do cruzamento (0,5 s)
VOLTA=$((L - X))
TOTAL=$((3 * L - 3 * X))
# O kit: o quadrado do render escalado a KIT_LADO, ao centro e um pouco abaixo do
# meio, longe do logotipo no canto de cima.
KIT_LADO="${KIT_LADO:-1240}"
KIT_DESCE="${KIT_DESCE:-110}"
# A janela onde as piteiras assentam (11/11/2026, Francisco: "uma janela para as
# piteiras assentarem em cima e se verem bem em contraste"). Sobre artes tao
# cheias de cor, as piteiras perdiam-se. E' uma faixa solida da marca, como a
# regra da app para o texto sobre fotos: roxo de fundo, contorno creme e tinta,
# sombra dura amarela, sem cantos redondos. Mede-se pelas proprias piteiras (a
# caixa que as contem em todas as posicoes da volta) mais JANELA_FOLGA a toda a
# volta, por isso serve a qualquer kit sem acertos a mao.
JANELA_FOLGA="${JANELA_FOLGA:-56}"
JANELA_COR="${JANELA_COR:-0x2E1F4C}"
DESTINO=videos/loops
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

seg() { awk "BEGIN{printf \"%.4f\", $1 / $FPS}"; }

# --- os tres temas, cada um com exactamente L fotogramas ---------------------
i=0
for t in $TEMAS; do
  i=$((i + 1))
  loop="$DESTINO/$t-16x9.mp4"
  if [ -f "$loop" ]; then
    n=$(ffprobe -v error -select_streams v:0 -count_packets -show_entries stream=nb_read_packets -of csv=p=0 "$loop")
    # Reescala o tempo para L fotogramas: um loop que ja fecha continua a fechar.
    ffmpeg -hide_banner -loglevel error -y -i "$loop" \
      -vf "setpts=(PTS-STARTPTS)*$L/$n,fps=$FPS,trim=end_frame=$L,setsar=1,format=yuv420p" \
      -an -c:v libx264 -crf 14 -preset fast "$TMP/t$i.mp4"
  elif [ -n "${RASCUNHO:-}" ]; then
    arte=$(ls arte/piteiras/original/*-"$t"-*mm.png | head -1)
    echo "  $t: sem loop, entra a arte parada a deslizar"
    # Vai a um lado e volta, num coseno: o fim encosta ao principio.
    ffmpeg -hide_banner -loglevel error -y -loop 1 -framerate $FPS -i "$arte" \
      -vf "scale=-2:1080:flags=lanczos,crop=1920:1080:x='(iw-ow)*(0.5-0.5*cos(2*PI*n/$L))':y=0,setsar=1,format=yuv420p" \
      -frames:v "$L" -an -c:v libx264 -crf 14 -preset fast "$TMP/t$i.mp4"
  else
    echo "falta $loop (faz-se com o fechar-loop.sh, ou corre com RASCUNHO=1)"; exit 1
  fi
done

# --- as piteiras a rodar, uma volta de VOLTA fotogramas -----------------------
PNGS="$DESTINO/kit-$KIT"
if [ "$(ls "$PNGS"/*.png 2>/dev/null | wc -l)" != "$VOLTA" ]; then
  node scripts/kit-transparente.mjs "$KIT" "$VOLTA"
fi

# --- a janela -----------------------------------------------------------------
# A caixa das piteiras em todas as posicoes: o alfa de cada fotograma, o maximo de
# todos (tmix), e o bbox do ffmpeg sobre isso. So o ffmpeg, para correr no Mac sem
# mais nada instalado.
CAIXA=$(ffmpeg -hide_banner -framerate $FPS -i "$PNGS/%03d.png" \
  -vf "format=rgba,alphaextract,tmix=frames=$VOLTA:weights=1:scale=1,scale=$KIT_LADO:$KIT_LADO,bbox=min_val=60" \
  -f null - 2>&1 | grep -o 'x1:[0-9]* x2:[0-9]* y1:[0-9]* y2:[0-9]*' | tail -1)
read -r KX1 KX2 KY1 KY2 <<<"$(echo "$CAIXA" | sed 's/[a-z0-9]*://g')"
[ -n "${KY2:-}" ] || { echo "nao consegui medir as piteiras em $PNGS"; exit 1; }
OX=$(( (1920 - KIT_LADO) / 2 )); OY=$(( (1080 - KIT_LADO) / 2 + KIT_DESCE ))
JX=$((OX + KX1 - JANELA_FOLGA)); JY=$((OY + KY1 - JANELA_FOLGA))
JW=$((KX2 - KX1 + 2 * JANELA_FOLGA)); JH=$((KY2 - KY1 + 2 * JANELA_FOLGA))
# De fora para dentro: a sombra amarela deslocada, o contorno de tinta, o creme, o roxo.
JANELA="drawbox=x=$((JX + 16)):y=$((JY + 16)):w=$JW:h=$JH:color=0xFFBE00:t=fill,\
drawbox=x=$JX:y=$JY:w=$JW:h=$JH:color=0x002448:t=fill,\
drawbox=x=$((JX + 4)):y=$((JY + 4)):w=$((JW - 8)):h=$((JH - 8)):color=0xF5EED1:t=fill,\
drawbox=x=$((JX + 12)):y=$((JY + 12)):w=$((JW - 24)):h=$((JH - 24)):color=$JANELA_COR:t=fill"
echo "  janela ${JW}x${JH} em ${JX},${JY}"

# --- a cena -------------------------------------------------------------------
XS=$(seg "$X")
ffmpeg -hide_banner -loglevel error -y \
  -i "$TMP/t1.mp4" -i "$TMP/t2.mp4" -i "$TMP/t3.mp4" \
  -stream_loop 2 -framerate $FPS -i "$PNGS/%03d.png" \
  -filter_complex "\
[0:v]settb=AVTB,split[a][ac];\
[a]trim=start_frame=$X,setpts=PTS-STARTPTS[a1];\
[ac]trim=end_frame=$X,setpts=PTS-STARTPTS[acab];\
[1:v]settb=AVTB[b];[2:v]settb=AVTB[c];\
[a1][b]xfade=transition=fade:duration=$XS:offset=$(seg $((L - 2 * X)))[s1];\
[s1][c]xfade=transition=fade:duration=$XS:offset=$(seg $((2 * L - 3 * X)))[s2];\
[s2][acab]xfade=transition=fade:duration=$XS:offset=$(seg $((3 * L - 4 * X))),$JANELA[fundo];\
[3:v]format=rgba,scale=$KIT_LADO:$KIT_LADO:flags=lanczos[kit];\
[fundo][kit]overlay=$OX:$OY:shortest=1,trim=end_frame=$TOTAL,format=yuv420p[v]" \
  -map "[v]" -an -c:v libx264 -preset slow -crf 22 -pix_fmt yuv420p -movflags +faststart "$DESTINO/cena-$KIT.mp4"

ffmpeg -hide_banner -loglevel error -y -i "$DESTINO/cena-$KIT.mp4" -frames:v 1 -q:v 3 "$DESTINO/cena-$KIT.jpg"
printf '%s  %s fotogramas, %s s, %s\n' "$DESTINO/cena-$KIT.mp4" \
  "$(ffprobe -v error -select_streams v:0 -count_packets -show_entries stream=nb_read_packets -of csv=p=0 "$DESTINO/cena-$KIT.mp4")" \
  "$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$DESTINO/cena-$KIT.mp4" | cut -c1-5)" \
  "$(du -h "$DESTINO/cena-$KIT.mp4" | cut -f1)"
