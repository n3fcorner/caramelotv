#!/usr/bin/env bash
# Poe as fotos de estudio do fornecedor por cima dos cartazes tirados do video.
#
#   ./scripts/cartazes-de-estudio.sh
#
# Trabalha a partir dos .webp ja descarregados em videos/fotos-weneed, e nao da
# rede: o catalogo da WeNeed roda, e em Setembro de 2026 o Uranium e a Gorilla
# Mandarini ja la nao estavam. Quem quiser ir buscar fotos novas corre antes o
# fotos-weneed.mjs, que grava os .webp aqui.
#
# As fotos passam pelo MESMO compositor dos recortes de video (recortar-flores.py,
# modo `estudio`): area do produto igualada e centro optico igual. Antes
# escalavam-se a 0,76 da altura com a margem transparente que trazem, e o produto
# ficava a 17% do cartaz — pequeno de mais para uma TV que desenha a 1080p.
set -euo pipefail
cd "$(dirname "$0")/.."

CARTAZES=videos/web/posters
n=0
for w in videos/fotos-weneed/*.webp; do
  slug=$(basename "$w" .webp)
  python3 scripts/recortar-flores.py estudio "$w" "$CARTAZES/$slug.jpg"
  n=$((n+1))
done

# As duas Critical Berry sao a mesma flor em linhas diferentes: partilham a foto.
cp "$CARTAZES/critical-berry.jpg" "$CARTAZES/critical-berry-cbd.jpg"
echo "  $n cartazes de estudio + critical-berry-cbd"
