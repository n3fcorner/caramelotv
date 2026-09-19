#!/usr/bin/env python3
"""Poe as flores sem foto de estudio a flutuar no roxo, como as outras.

    ./scripts/recortar-flores.py

Treze das dezoito referencias tem foto de estudio da WeNeed, e o fotos-weneed.mjs
recorta-as sobre o roxo da marca. As outras — Smash Cali, Sour Lifter, Lemon Trip
e White Banger — sao da Trim Rocket, que nao tem loja, e por isso o cartaz saia do
proprio video: um rectangulo fotografico com luva preta e bancada, no meio de
catorze recortes limpos. Destoava, e era a unica coisa que destoava na prateleira.

Como se resolve, sem foto nenhuma:

  1. **Vision levanta o objecto do fundo** (VNGenerateForegroundInstanceMask, o
     mesmo que o Preview usa). Sozinho nao chega: numa macro de cabecas na mao a
     luva tambem e' primeiro plano, e vem agarrada.
  2. **Tira-se a luva por medicao.** E' escura, sem cor e lisa; a flor tem textura
     a toda a escala. Ficam-se com os pixeis que nao sao nem luva nem fundo claro.
  3. **Fica a maior mancha**, com os buracos tapados.

O instante de cada clipe foi escolhido varrendo o video de 3 em 3% e medindo se a
cabeca — ja sem luva — encosta aos limites do enquadramento, e desde 17/9/2026
tambem a **nitidez** (variancia do laplaciano dentro da mancha): os recortes de
video ficavam sistematicamente abaixo dos de estudio, e parte disso era escolher
fotogramas com desfoque de movimento. A escolha final continua a ser a vista. Encostar em BAIXO
nao faz mal: as fotos de estudio tambem tem o pe cortado, e le-se como a flor
assente. Encostar de lado ou no topo e' que da a cabeca ceifada a direito, e
esses frames sao postos fora. Sobram poucos: 7 dos 23 no Smash Cali, 3 no Sour
Lifter, 4 no Lemon Trip.

Duas armadilhas que custaram tempo, e ficam escritas para nao se repetirem:

  * O `croppedToInstancesExtent` do Vision devolve a caixa do objecto, e nessa
    caixa a mascara **nunca** toca a borda — mesmo com a cabeca cortada. Medir ali
    da sempre zero. Por isso o vision-recortar.swift devolve o frame inteiro.
  * O encosto tem de se medir na mancha da FLOR, nao na mascara bruta do Vision:
    a mascara bruta leva a luva, que atravessa o enquadramento de lado a lado e
    faz parecer cortado tudo o que se meca.

O White Banger nao tem um unico frame aproveitavel — e' uma macro apertada de tres
cabecas, e em todos os 21 elas saem pelos lados. Esse sai da fotografia que veio
com o clipe, que e' o mesmo produto com espaco a volta.

A **Sky** parecia sem solucao — a cabeca enche o frame e sai sempre cortada — ate
se perceber que o problema era outro: as luvas desse clipe sao cinzentas, e o
filtro so tirava luva preta, por isso a mancha "cabeca" trazia luva e tocava nos
bordos. Com o filtro de cinzento, o frame dos 9% sai inteiro, so com o pe
cortado, e e' o mais nitido do clipe todo (2059). Ligou-se o mesmo ao OG Ivory
Dry, que sofria do mesmo.
"""
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

RAIZ = Path(__file__).resolve().parent.parent
CARTAZES = RAIZ / "videos/web/posters"
# Para o cartaz usa-se a janela limpa INTEIRA — entre o fim do logo do canal
# (y=350) e o inicio da faixa do nome (y=1520) — e nao o quadrado de 1080 que o
# video leva. Sao mais 90px de altura, e e' o que chega para varias cabecas
# deixarem de sair pelo cimo do enquadramento.
CORTE = "crop=1080:1170:0:350"

FUNDO = (0x1E, 0x14, 0x33)
LADO = 720

# **Iguala-se area, e nao altura**, e desde 17/9/2026 TODOS os cartazes passam por
# aqui, os de estudio incluidos (cartazes-de-estudio.sh chama `estudio()`). Ate
# ai o produto ocupava ~17% do cartaz, que num mosaico de 97dp numa TV que
# desenha a 1080p dava uma cabeca de 81x142 px — e o Francisco viu-a "com
# pessima resolucao". Nao era resolucao, era tamanho: a 30% de area o produto
# fica 1,3x maior em cada dimensao sem tocar num pixel de origem. O centro
# optico fica a 0,33 da altura: com 30% de area o produto acaba por volta dos 60%
# e o terco de baixo do cartaz e' margem roxa — e' ai, e so ai, que a faixa do
# nome assenta no mosaico. A foto inteira, o nome inteiro, nada por cima de nada.
AREA_ALVO = 0.30
CENTRO = 0.33

#   origem | slug | % da duracao (None quando a origem ja e' uma fotografia) | opcoes
#
# opcoes: `luva="cinza"` quando o clipe e' filmado com luvas cinzentas em vez de
# pretas — o OG Ivory Dry e' o unico ate agora. O filtro normal so tira luva
# escura, e nesse clipe ou vinha luva agarrada ou sobrava so o miolo partido.
# `abertura=N` alarga a operacao morfologica que corta fiapos entre a luva e a placa.
# `rotacao="auto"` poe de pe uma cabeca que o fornecedor filmou deitada (eixo maior
# a vertical); num recorte sobre fundo liso a orientacao e' apresentacao, nao registo.
FONTES = [
    ("videos/Infusao THCP/Smash Cali.mp4", "smash-cali", 18, {"rotacao": "auto"}),
    ("videos/Infusao THCP/SourLifter.MOV", "sour-lifter", 84, {"rotacao": "auto"}),
    ("videos/CBD/LemonTrip.mp4", "lemon-trip", 24),
    ("videos/fotos-trimrocket/white-banger-02.jpg", "white-banger", None),
    ("videos/Infusao THCP/Pulsar.mp4", "pulsar", 78),
    ("videos/Infusao THCP/Peanut Butter.mp4", "peanut-butter", 84),
    ("videos/Infusao THCP/Sky.mp4", "sky", 9, {"luva": "cinza", "abertura": 9}),
    ("videos/Hash/Papaya OG.mp4", "papaya-og", 32),
    ("videos/Hash/La Mousse.mp4", "la-mousse", 45),
    ("videos/Hash/OG Ivory Dry.mp4", "og-ivory-dry", 6, {"luva": "cinza", "abertura": 15}),
]


def constroi_ferramenta():
    """Compila o recortador do Vision a primeira vez que faz falta."""
    binario = RAIZ / "scripts/.vision-recortar"
    fonte = RAIZ / "scripts/vision-recortar.swift"
    if not binario.exists() or binario.stat().st_mtime < fonte.stat().st_mtime:
        subprocess.run(["swiftc", "-O", "-o", str(binario), str(fonte)], check=True)
    return binario


def frame(origem: Path, pct, destino: Path):
    """Tira a janela limpa da origem — um instante do clipe, ou a foto inteira."""
    posicao = []
    if pct is not None:
        dur = float(subprocess.run(
            ["ffprobe", "-v", "error", "-show_entries", "format=duration",
             "-of", "csv=p=0", str(origem)],
            capture_output=True, text=True, check=True).stdout)
        posicao = ["-ss", f"{dur * pct / 100:.2f}"]
    subprocess.run(
        ["ffmpeg", "-y", "-nostdin", "-v", "error", *posicao,
         "-i", str(origem), "-vf", CORTE, "-frames:v", "1", "-q:v", "2", str(destino)],
        check=True)


def so_a_flor(png: Path, luva: str = "preta", abertura: int = 5, rotacao=0.0):
    """Tira a luva e o fundo claro da mascara, e fica com a maior mancha."""
    a = np.asarray(Image.open(png).convert("RGBA")).astype(np.float32)
    rgb, dentro = a[..., :3], a[..., 3] > 127

    mx, mn = rgb.max(2), rgb.min(2)
    lum = rgb.mean(2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1), 0)
    med = ndimage.uniform_filter(lum, 7)
    textura = np.sqrt(np.maximum(ndimage.uniform_filter(lum * lum, 7) - med * med, 0))

    mascara_luva = (lum < 105) & (sat < 0.30) & (textura < 13)
    if luva == "cinza":
        # Luva cinzenta: neutra (sem tom quente), a meio da escala, lisa. O hash e'
        # creme quente, com o vermelho claramente acima do azul — e' isso que os separa.
        quente = rgb[..., 0] - rgb[..., 2]
        mascara_luva |= (quente < 14) & (lum >= 80) & (lum <= 210) & (textura < 16)
        mascara_luva |= lum < 60
    fundo = (lum > 185) & (sat < 0.12) & (textura < 9)

    flor = ndimage.binary_opening(dentro & ~mascara_luva & ~fundo, np.ones((abertura, abertura)))
    flor = ndimage.binary_closing(flor, np.ones((11, 11)))
    etiq, n = ndimage.label(flor)
    if n == 0:
        raise SystemExit(f"{png.name}: nao sobrou mancha nenhuma")
    maior = int(np.argmax(ndimage.sum(flor, etiq, range(1, n + 1)))) + 1
    mancha = ndimage.binary_fill_holes(etiq == maior)

    ys, xs = np.where(mancha)
    corte = (slice(ys.min(), ys.max() + 1), slice(xs.min(), xs.max() + 1))
    saida = np.dstack([rgb, np.where(mancha, 255, 0)]).astype(np.uint8)[corte]
    im = Image.fromarray(saida, "RGBA")
    # a mascara vem dura; um pixel de esbatimento tira o degrau na silhueta
    r, g, b, alpha = im.split()
    im = Image.merge("RGBA", (r, g, b, alpha.filter(ImageFilter.GaussianBlur(0.8))))
    return tratar(im, rotacao)


def estudio(webp: Path, destino: Path):
    """Uma foto de estudio (com alfa) pelo mesmo compositor dos recortes de video."""
    im = Image.open(webp).convert("RGBA")
    caixa = im.getchannel("A").point(lambda v: 255 if v > 16 else 0).getbbox()
    cartaz(im.crop(caixa) if caixa else im, destino)


def tratar(flor: Image.Image, rotacao=0.0) -> Image.Image:
    """O mesmo toque de luz dos cartazes de video (contraste, saturacao), e a rotacao.

    Os recortes vem de video comprimido e por vezes mal exposto; as fotos de
    estudio nao. Sem isto, ao lado umas das outras, as de video pareciam baças.
    """
    from PIL import ImageEnhance
    r, g, b, a = flor.split()
    rgb = Image.merge("RGB", (r, g, b))
    rgb = ImageEnhance.Contrast(rgb).enhance(1.08)
    rgb = ImageEnhance.Color(rgb).enhance(1.10)
    rgb = ImageEnhance.Brightness(rgb).enhance(1.04)
    flor = Image.merge("RGBA", (*rgb.split(), a))
    if rotacao == "auto":
        # Endireitar: o eixo maior da mancha (por momentos de segunda ordem) passa
        # a vertical. Adivinhar graus a olho ja deu uma cabeca deitada ao contrario.
        m = np.asarray(flor)[..., 3] > 127
        ys, xs = np.nonzero(m)
        x, y = xs - xs.mean(), ys - ys.mean()
        angulo = 0.5 * np.degrees(np.arctan2(2 * (x * y).mean(), (x * x).mean() - (y * y).mean()))
        # `angulo` e' o do eixo maior em relacao ao horizontal (y para baixo);
        # rodar por (angulo - 90) poe-no vertical. O rotate() do PIL roda no
        # sentido anti-horario e tem o y para cima, dai o sinal.
        # rotate() do PIL: positivo = anti-horario, com o y para cima. Verificado
        # a vista: e' `angulo + 90` que poe o eixo maior na vertical, com a
        # ponta da cabeca para cima quando ela apontava para a direita.
        rotacao = angulo + 90.0
        while rotacao > 90: rotacao -= 180
        while rotacao < -90: rotacao += 180
    if rotacao:
        flor = flor.rotate(float(rotacao), resample=Image.BICUBIC, expand=True)
        caixa = flor.getchannel("A").point(lambda v: 255 if v > 16 else 0).getbbox()
        if caixa:
            flor = flor.crop(caixa)
    return flor


def cartaz(flor: Image.Image, destino: Path):
    # escala que poe a mancha do produto na area mediana dos cartazes de estudio
    opacos = (np.asarray(flor)[..., 3] > 127).sum()
    k = ((AREA_ALVO * LADO * LADO) / max(opacos, 1)) ** 0.5
    larg, alto = max(1, round(flor.width * k)), max(1, round(flor.height * k))
    if alto > LADO or larg > LADO:                 # nunca sair do mosaico
        k *= min(LADO / alto, LADO / larg)
        larg, alto = max(1, round(flor.width * k)), max(1, round(flor.height * k))
    flor = flor.resize((larg, alto), Image.LANCZOS)

    base = Image.new("RGBA", (LADO, LADO), FUNDO + (255,))
    topo = int(LADO * CENTRO) - alto // 2
    base.alpha_composite(flor, ((LADO - larg) // 2, max(0, min(topo, LADO - alto))))
    base.convert("RGB").save(destino, quality=92)


def main():
    recortar = constroi_ferramenta()
    trabalho = RAIZ / "videos/.recorte"
    trabalho.mkdir(exist_ok=True)
    for fonte in FONTES:
        caminho, slug, pct = fonte[:3]
        opcoes = fonte[3] if len(fonte) > 3 else {}
        origem = RAIZ / caminho
        if not origem.exists():
            print(f"  em falta: {caminho}")
            continue
        jpg, png = trabalho / f"{slug}.jpg", trabalho / f"{slug}.png"
        frame(origem, pct, jpg)
        subprocess.run([str(recortar), str(jpg), str(png)],
                       check=True, capture_output=True)
        cartaz(so_a_flor(png, **opcoes), CARTAZES / f"{slug}.jpg")
        print(f"  {slug:<14} {f'{pct}% do clipe' if pct is not None else 'da fotografia'}")


if __name__ == "__main__":
    # `recortar-flores.py estudio <foto.webp> <cartaz.jpg>` compoe uma foto de estudio.
    if len(sys.argv) == 4 and sys.argv[1] == "estudio":
        estudio(Path(sys.argv[2]), Path(sys.argv[3]))
    else:
        sys.exit(main())
