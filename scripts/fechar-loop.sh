#!/usr/bin/env bash
# Fecha em loop um clipe gerado no Gemini (Veo) a partir das artes das piteiras,
# poe-lhe o logotipo por cima e tira as versoes para a TV.
#
#   ./scripts/fechar-loop.sh clipe.mp4 flora-brasileira          # cruza 1 s
#   ./scripts/fechar-loop.sh clipe.mp4 flora-brasileira 0        # ja fecha sozinho
#   SEM_LOGO=1 ./scripts/fechar-loop.sh clipe.mp4 flora-brasileira
#
# Sai para videos/loops/ (fora do git, como o resto da media):
#
#   <nome>-16x9.mp4   1920x1080, para ecra inteiro
#   <nome>-1x1.mp4    900x900, para o painel quadrado da parafernalia
#   <nome>.jpg        o primeiro fotograma, para cartaz
#
# Como fecha o loop. O Veo nao garante que o ultimo fotograma volte ao primeiro.
# Com o terceiro argumento a 1 (omissao) o ultimo segundo do clipe cruza-se com o
# primeiro: o video final comeca no fotograma 1 s e acaba exactamente nele, por
# isso o salto desaparece. Custa esse segundo: de 8 s ficam 7.
#
# Quando o clipe vem do Flow com o mesmo fotograma no inicio e no fim, ja fecha
# sozinho, e o cruzamento so o estragava. Ai passa-se 0: tira-se apenas o ultimo
# fotograma, que e' repetido do primeiro e daria um soluco a cada volta.
#
# O audio sai sempre: a montra e' muda.
set -euo pipefail
cd "$(dirname "$0")/.."

ENTRADA="${1:?uso: fechar-loop.sh clipe.mp4 nome [segundos de cruzamento]}"
NOME="${2:?falta o nome, ex.: flora-brasileira}"
CRUZA="${3:-1}"
LOGO=arte/piteiras/logo-autocolante.png
DESTINO=videos/loops
mkdir -p "$DESTINO"

# Duracao e fotogramas do proprio stream de video, nao do contentor: o Veo poe
# audio, e o audio pode ser uns milissegundos mais comprido.
DUR=$(ffprobe -v error -select_streams v:0 -show_entries stream=duration -of csv=p=0 "$ENTRADA")
NFOT=$(ffprobe -v error -select_streams v:0 -count_packets -show_entries stream=nb_read_packets -of csv=p=0 "$ENTRADA")

# Qualquer proporcao que venha entra a encher 16:9 sem deformar.
NORMAL="scale=1920:1080:force_original_aspect_ratio=increase:flags=lanczos,crop=1920:1080,setsar=1,format=yuv420p"

if awk "BEGIN{exit !($CRUZA > 0)}"; then
  awk "BEGIN{exit !($DUR > 3 * $CRUZA)}" || { echo "clipe curto de mais ($DUR s) para cruzar $CRUZA s"; exit 1; }
  OFFSET=$(awk "BEGIN{printf \"%.3f\", $DUR - 2 * $CRUZA}")
  LACO="[0:v]$NORMAL,split[a][b];\
[a]trim=start=$CRUZA,setpts=PTS-STARTPTS[corpo];\
[b]trim=end=$CRUZA,setpts=PTS-STARTPTS[cabeca];\
[corpo][cabeca]xfade=transition=fade:duration=$CRUZA:offset=$OFFSET[laco]"
else
  LACO="[0:v]$NORMAL,trim=end_frame=$((NFOT - 1)),setpts=PTS-STARTPTS[laco]"
fi

# O logotipo no canto de cima a esquerda, dentro da margem de seguranca da TV
# (5% de cada lado), que e' onde o prompt do passo 1 pede para deixar calmo.
if [ -n "${SEM_LOGO:-}" ]; then
  MARCA="[laco]split[l16][q];\
[q]crop=1080:1080:420:0,scale=900:900:flags=lanczos[l1]"
else
  MARCA="[laco]split[s16][q];\
[q]crop=1080:1080:420:0,scale=900:900:flags=lanczos[sq];\
[1:v]split[lg][lp];\
[lg]scale=230:-1:flags=lanczos[lg2];\
[lp]scale=150:-1:flags=lanczos[lp2];\
[s16][lg2]overlay=96:54[l16];\
[sq][lp2]overlay=45:45[l1]"
fi

ffmpeg -hide_banner -loglevel error -y -i "$ENTRADA" -i "$LOGO" \
  -filter_complex "$LACO;$MARCA" \
  -map "[l16]" -an -c:v libx264 -preset slow -crf 20 -pix_fmt yuv420p -movflags +faststart "$DESTINO/$NOME-16x9.mp4" \
  -map "[l1]"  -an -c:v libx264 -preset slow -crf 27 -pix_fmt yuv420p -movflags +faststart "$DESTINO/$NOME-1x1.mp4"

ffmpeg -hide_banner -loglevel error -y -i "$DESTINO/$NOME-16x9.mp4" -frames:v 1 -q:v 3 "$DESTINO/$NOME.jpg"

for f in "$DESTINO/$NOME-16x9.mp4" "$DESTINO/$NOME-1x1.mp4"; do
  printf '%-40s %s s  %s\n' "$f" \
    "$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$f" | cut -c1-5)" \
    "$(du -h "$f" | cut -f1)"
done
