#!/usr/bin/env python3
"""Tira as 15 artes das piteiras da folha-mestra e prepara-as para o Gemini.

    python3 scripts/arte-piteiras.py [folha.pdf] [logotipo.png]

Por omissao le a folha do repositorio do painel, ao lado deste:

    ../caramelo-b2b/plan/piteiras/caramelo-do-brasil-vs8.pdf

e o logotipo principal do manual da marca, no mesmo repositorio.

Escreve em arte/piteiras/:

    original/NN-nome-Xmm.png   a arte plana, recortada sem reamostrar
    16x9/NN-nome-Xmm.jpg       a arte a toda a largura de um quadro 1920x1080,
                               com faixas cinzentas em cima e em baixo e o
                               logotipo e os textos tapados, para o Gemini
                               preencher (passo 1 do PROMPTS.md)
    logo-autocolante.png       o logotipo com contorno branco, como nas artes,
                               para o fechar-loop.sh pôr por cima do video

Porque as faixas cinzentas e nao a arte esticada: o editor de imagem do Gemini
devolve a proporcao da imagem que recebe. Dar-lhe a arte solta (2,6:1) era
receber de volta uma faixa, e nao um quadro de TV. O cinzento liso diz-lhe sem
ambiguidade onde e' tela por pintar.

A folha (Photoshop, 27/2/2025) traz uma imagem so, de 4047x1589 a 300 ppi, com
as 15 artes em tres filas, uma por diametro, e por baixo de cada uma duas vistas
do tubo enrolado. So nos interessa a arte plana de cima. As caixas foram medidas
sobre essa imagem: o fundo e' preto e cada painel e' o bloco nao-preto da fila.
"""
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
B2B = os.path.join(os.path.dirname(RAIZ), "caramelo-b2b")
FOLHA = os.path.join(B2B, "plan/piteiras/caramelo-do-brasil-vs8.pdf")
LOGO = os.path.join(
    B2B,
    "plan/brand/manual/CARAMELO DO BRASIL/1 LOGOTIPO PRINCIPAL/PNG - FUNDO TRASNPARENTE/"
    "LOGOTIPO PRINCIPAL CARAMELO DO BRASIL 2024.png",
)
DESTINO = os.path.join(RAIZ, "arte/piteiras")

# (y0, y1, [(x0, x1) por coluna], [nomes]) — os nomes sao os slugs da loja sem o
# prefixo "piteira-vidro-" nem o diametro, pela ordem em que estao na folha.
FILAS = {
    "7mm": (60, 309,
            [(267, 908), (1027, 1672), (1791, 2432), (2553, 3195), (3313, 3958)],
            ["filtro-de-barro", "flora-brasileira", "mosaico-da-favela",
             "essencia-do-brasil", "formula-1"]),
    "6mm": (616, 831,
            [(267, 908), (1029, 1670), (1792, 2432), (2564, 3201), (3316, 3955)],
            ["vira-lata-caramelo", "fauna-brasileira", "retratos-da-favela",
             "pixacao-estilo-brasileiro", "futebol-do-brasil"]),
    "5mm": (1126, 1309,
            [(270, 906), (1033, 1669), (1793, 2430), (2555, 3192), (3318, 3955)],
            ["pastel-de-feira", "brasil-indigena", "cores-do-ceu-da-favela",
             "coracao-pulsa-arte", "surfando-a-onda-boa"]),
}

# O que se tapa de cinzento antes de a arte ir ao Gemini, em pixeis da arte plana:
# o cao do logotipo, o letreiro CARAMELO DO BRASIL, o selo redondo do H e a faixa
# com o titulo. Medido a vista sobre cada recorte com uma grelha de 25 px.
#
# Tapa-se em vez de pedir ao Gemini que apague, por duas razoes. Um video gerado
# entorta as letras, e o letreiro verdadeiro entra no fim, nitido, pelo
# fechar-loop.sh. E as artes trazem coisas que fazem o Gemini recusar a imagem
# inteira: a folha no logotipo e na bandeira da F1, o THC no escudo do futebol, o
# LEGALIZE da pixacao. Sem elas a arte passa, e o desenho fica intacto.
#
# "r" e' rectangulo, "o" e' elipse (os selos sao redondos e uma caixa comia o que
# esta a volta, como o beija-flor pousado no selo da fauna).
BURACOS = {
    "filtro-de-barro":           [("r", 4, 8, 142, 122), ("r", 393, 44, 620, 108),
                                  ("o", 35, 148, 132, 232), ("r", 311, 172, 636, 209)],
    "flora-brasileira":          [("r", 48, 32, 162, 140), ("r", 18, 164, 202, 220),
                                  ("o", 530, 44, 630, 133), ("r", 405, 174, 636, 216)],
    "mosaico-da-favela":         [("r", 14, 10, 130, 120), ("r", 24, 156, 252, 222),
                                  ("r", 314, 38, 633, 79), ("o", 507, 146, 606, 232)],
    "essencia-do-brasil":        [("r", 15, 2, 130, 102), ("r", 18, 148, 242, 217),
                                  ("r", 297, 35, 612, 70), ("o", 533, 138, 630, 210)],
    "formula-1":                 [("r", 6, 4, 110, 102), ("r", 110, 34, 322, 96),
                                  ("r", 10, 157, 233, 200), ("o", 227, 134, 320, 220),
                                  ("o", 398, 40, 462, 80)],          # a folha da bandeira
    "vira-lata-caramelo":        [("r", 6, 12, 240, 80), ("r", 390, 20, 630, 59),
                                  ("r", 70, 110, 196, 159),          # a etiqueta R$ 4,20
                                  ("o", 463, 72, 557, 166)],
    "fauna-brasileira":          [("r", 24, 131, 252, 197), ("r", 400, 11, 633, 47),
                                  ("o", 405, 124, 496, 210)],
    "retratos-da-favela":        [("o", 12, 4, 110, 90), ("r", 360, 18, 626, 57),
                                  ("r", 6, 106, 116, 215), ("r", 117, 138, 330, 200)],
    "pixacao-estilo-brasileiro": [("r", 6, 2, 110, 100), ("r", 14, 128, 233, 194),
                                  ("o", 457, 108, 550, 187),
                                  ("r", 250, 131, 456, 184)],        # LEGALIZE
    "futebol-do-brasil":         [("r", 12, 14, 120, 110), ("r", 14, 141, 206, 197),
                                  ("o", 144, 4, 250, 100), ("r", 374, 11, 626, 50),
                                  ("r", 424, 117, 473, 144)],        # o THC do escudo
    "pastel-de-feira":           [("r", 400, 18, 620, 79), ("o", 254, 86, 346, 175),
                                  ("r", 340, 106, 623, 152)],
    "brasil-indigena":           [("r", 26, 1, 137, 110), ("r", 10, 114, 166, 164),
                                  ("r", 367, 34, 623, 74), ("o", 517, 94, 613, 180)],
    "cores-do-ceu-da-favela":    [("r", 6, 1, 116, 100), ("r", 14, 108, 243, 174),
                                  ("r", 310, 8, 626, 50), ("o", 410, 58, 506, 144)],
    "coracao-pulsa-arte":        [("r", 13, 2, 117, 102), ("r", 17, 102, 243, 165),
                                  ("r", 420, 39, 630, 72), ("o", 524, 89, 613, 178)],
    "surfando-a-onda-boa":       [("r", 6, 0, 107, 100), ("r", 110, 24, 330, 86),
                                  ("r", 20, 120, 290, 160), ("o", 534, 4, 630, 90)],
}

LARGURA, ALTURA = 1920, 1080
CINZENTO = (128, 128, 128)


def tapar(arte, buracos, folga=3):
    """Pinta de cinzento liso o que o Gemini nao deve ver nem copiar."""
    arte = arte.copy()
    d = ImageDraw.Draw(arte)
    for forma, x0, y0, x1, y1 in buracos:
        caixa = (x0 - folga, y0 - folga, x1 + folga, y1 + folga)
        (d.ellipse if forma == "o" else d.rectangle)(caixa, fill=CINZENTO)
    return arte


def imagem_da_folha(pdf):
    """A imagem embebida, tal como esta no PDF. Rasterizar a pagina reamostrava."""
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["pdfimages", "-f", "1", "-l", "1", "-png", pdf, f"{tmp}/i"], check=True)
        # A primeira e' a arte; as outras tres sao mascaras de 39 KB.
        return Image.open(f"{tmp}/i-000.png").convert("RGB")


def tela_16x9(arte):
    """A arte a toda a largura, centrada na vertical, sobre cinzento liso."""
    alto = round(arte.height * LARGURA / arte.width)
    grande = arte.resize((LARGURA, alto), Image.LANCZOS)
    tela = Image.new("RGB", (LARGURA, ALTURA), CINZENTO)
    tela.paste(grande, (0, (ALTURA - alto) // 2))
    return tela


def autocolante(logo_png, contorno=22):
    """O logotipo com o contorno branco grosso que tem em todas as artes."""
    logo = Image.open(logo_png).convert("RGBA")
    logo = logo.crop(logo.getbbox())
    margem = contorno + 4
    base = Image.new("RGBA", (logo.width + 2 * margem, logo.height + 2 * margem), (0, 0, 0, 0))
    base.paste(logo, (margem, margem), logo)
    alfa = base.getchannel("A").point(lambda a: 255 if a > 8 else 0)
    # MaxFilter com tamanho impar dilata a mascara; repetido fica redondo e nao quadrado.
    for _ in range(contorno // 5):
        alfa = alfa.filter(ImageFilter.MaxFilter(11))
    alfa = alfa.filter(ImageFilter.GaussianBlur(1.2))
    branco = Image.new("RGBA", base.size, (255, 255, 255, 255))
    branco.putalpha(alfa)
    branco.alpha_composite(base)
    return branco


def main():
    pdf = sys.argv[1] if len(sys.argv) > 1 else FOLHA
    logo = sys.argv[2] if len(sys.argv) > 2 else LOGO
    for pasta in ("original", "16x9"):
        os.makedirs(os.path.join(DESTINO, pasta), exist_ok=True)

    folha = imagem_da_folha(pdf)
    if folha.size != (4047, 1589):
        sys.exit(f"A folha mudou de tamanho ({folha.size}); as caixas tem de ser medidas outra vez.")

    n = 0
    for mm, (y0, y1, colunas, nomes) in FILAS.items():
        for (x0, x1), nome in zip(colunas, nomes):
            n += 1
            base = f"{n:02d}-{nome}-{mm}"
            arte = folha.crop((x0, y0, x1, y1))
            arte.save(os.path.join(DESTINO, "original", base + ".png"), optimize=True)
            tela_16x9(tapar(arte, BURACOS[nome])).save(os.path.join(DESTINO, "16x9", base + ".jpg"), quality=93, subsampling=0)
            print(f"{base:44s} {arte.width}x{arte.height}")

    autocolante(logo).save(os.path.join(DESTINO, "logo-autocolante.png"), optimize=True)
    print("logo-autocolante.png")


if __name__ == "__main__":
    main()
