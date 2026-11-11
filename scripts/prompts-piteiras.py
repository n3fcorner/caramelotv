#!/usr/bin/env python3
"""Os prompts do Gemini para animar as artes das piteiras, num sitio so.

    python3 scripts/prompts-piteiras.py              escreve arte/piteiras/PROMPTS.md
    python3 scripts/prompts-piteiras.py pagina.html  e tambem a pagina com botoes de copiar

Cada arte passa duas vezes pelo Gemini. O passo 1 (imagem) pinta as zonas cinzentas
da tela 16:9 que o arte-piteiras.py preparou; o passo 2 (video) poe-na a mexer em
loop. Os dois prompts de cada arte sao a mesma base com a parte da cena e a parte do
movimento de cada uma, e e' isso que este ficheiro guarda.

Os prompts vao em ingles porque e' a lingua em que o Veo e o editor de imagem do
Gemini seguem melhor as restricoes (camara parada, nada de texto, voltar a pose
inicial). Nenhum fala do produto nem do uso: descrevem o desenho e o movimento. E
nenhum pede fumo, nevoeiro nem vapor, nem por engano.
"""
import html
import json
import os
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

BASE_IMAGEM = """Edit the attached image. In the middle is a flat illustration. The solid grey areas, the bands above and below it and the grey shapes on top of it, are empty canvas.

Paint over every grey area so that no grey remains: extend the scene up into the top band and down into the bottom band, and fill each grey shape with whatever is behind it in the scene. {cena}

Keep every drawn element exactly as it is: same characters, objects, positions, shapes and colours. Same style everywhere: flat 2D screen-print poster illustration, bold dark navy outlines, flat colour fills, no gradients, no 3D, no photographic detail. Keep the top-left corner simple, background only, because a logo will be placed there later.

No text, no letters, no numbers, no logos, no badges, no watermark, no border. Output a 16:9 image, 1920x1080."""

BASE_VIDEO = """Animate this illustration into an 8-second seamless loop for a shop TV.

Camera: locked off. No zoom, no pan, no camera movement at all.
Style: it stays a flat 2D illustration with the same bold outlines, flat colours and composition as the image. Nothing turns 3D or photographic. No new objects or characters appear.
Motion: {movimento}
Loop: every movement is gentle and cyclical, and on the last frame everything is back in its starting pose, so the end matches the first frame.
Avoid: text, smoke, fog, haze, steam, sparkles, flashes, cuts, transitions. No dialogue."""

# (numero, chave, titulo, mm, cena, movimento)
TEMAS = [
    (1, "filtro-de-barro", "Filtro de barro", 7,
     "The scene is a repeating pattern of orange clay water filters, the classic Brazilian ceramic filter, outlined in yellow, green and red on a dark navy background. Continue the same filters in the same grid above and below, so the whole frame is the pattern.",
     "The clay water filters do a slow stadium wave: one after another, from left to right, each filter rises a little, about a tenth of its height, and settles back. The wave crosses every row twice during the clip. A soft highlight slides down each filter as it rises. The navy background stays still."),
    (2, "flora-brasileira", "Flora brasileira", 7,
     "The scene is a Brazilian rainforest along a blue river under a red sky, with palms, ferns, big leaves and a yellow bird in flight. Extend the red sky and the treetops above, and the river and the leafy foreground below.",
     "A soft breeze moves through the forest: palm fronds, ferns and big leaves sway slowly, each at its own rhythm. The river flows towards the viewer, its ripple lines drifting down continuously. The yellow bird flaps its wings and glides in a small circle, ending where it started."),
    (3, "mosaico-da-favela", "Mosaico da favela", 7,
     "The scene is a hillside favela of stacked, brightly painted houses in yellow, red, green and blue under a yellow sky. Extend the yellow sky above and continue the houses, walls and stairs below.",
     "Windows light up warm yellow one by one and switch off again, in a slow random twinkle across the hill. Now and then a few house fronts change colour in a gentle domino, yellow to green to blue, and go back to their original colours before the end. Nothing changes position."),
    (4, "essencia-do-brasil", "Essência do Brasil", 7,
     "The scene is a big graffiti piece of interlaced ribbons and arrows in green, blue and yellow with navy outlines on a white wall, with small paint splashes. Continue the ribbons, arrows and splashes into the new space above and below.",
     "The graffiti flows like living paint: the stripes inside each ribbon travel continuously along the ribbon in the direction of its arrowhead, the arrowheads pulse forward and back a little, and the small splashes wobble. The outline of the whole piece stays where it is."),
    (5, "formula-1", "Fórmula 1", 7,
     "The scene is a caramel-coloured dog with red sunglasses driving a red and white Formula 1 car on a curving white track over a blue background, holding up a yellow flag. The flag is plain yellow with nothing on it. Extend the blue background and the white track curves above and below.",
     "Racing on the spot: the car stays in the same place while its wheels spin fast, the white track curves stream past behind it from right to left with a few flat speed lines, the car vibrates slightly, the yellow flag ripples in the wind and the dog's ears flap back. The dog keeps its sunglasses on and keeps smiling."),
    (6, "vira-lata-caramelo", "Vira-lata caramelo", 6,
     "The scene is a lazy afternoon: a caramel mutt with red sunglasses lying on a blue rug next to a white plastic chair, a potted snake plant, a glass and a small mirror, against a yellow wall. Extend the yellow wall above and the floor and rug below.",
     "The dog breathes slowly, its belly rising and falling, wags its tail lazily twice and twitches one ear. The snake plant leaves sway a tiny bit. A gleam of light slides across the mirror once. Everything else stays still."),
    (7, "fauna-brasileira", "Fauna brasileira", 6,
     "The scene is a line-up of Brazilian animals on a background split in red on the left and green on the right: a blue macaw, a golden lion tamarin, a sloth with a white egret on its back, a hummingbird and a toucan. Extend the red and green background above and below, and complete the bodies the edges cut.",
     "The animals are calm and alive: the macaw tilts its head and blinks, the tamarin's mane ruffles and it blinks slowly, the sloth turns its head very slowly and back, the egret opens and closes its wings once, the hummingbird hovers in place with its wings fluttering fast, and the toucan bobs its big beak and blinks. Every animal ends in its starting pose."),
    (8, "retratos-da-favela", "Retratos da favela", 6,
     "The scene is pairs of sneakers hanging by their laces from crossing electric wires against an orange sky, with a faint favela skyline along the bottom. Extend the orange sky and the wires above, and the skyline below.",
     "The sneakers swing gently on their laces like slow pendulums, each pair at a slightly different rhythm, and the wires sway softly with them. All of them come back to their starting positions at the end. The sky and the skyline stay still."),
    (9, "pixacao-estilo-brasileiro", "Pixação estilo brasileiro", 6,
     "The scene is a yellow wall covered in tall, angular Brazilian pixação-style graffiti strokes in blue, orange and green, with the dark silhouette of a person reaching up to paint on the right. Turn all the writing on the wall into abstract pixação-style strokes in the same colours, so that no readable word remains. Extend the wall above and below.",
     "The silhouette's arm moves slowly as if painting, and new abstract strokes appear one by one in front of the hand, then gently fade back so the wall ends as it began. The other strokes stay still. No readable letters or words appear."),
    (10, "futebol-do-brasil", "Futebol do Brasil", 6,
     "The scene is a green football pitch seen from above, with white field lines, a yellow and green shield with five stars above it, and a black and white football. The shield is plain, with no letters on it. Extend the pitch, with its stripes and white lines, above and below.",
     "The football spins and bounces gently on the spot, the grass stripes shimmer as if a breeze crosses the pitch, and the five stars above the shield twinkle one after another. The field lines and the shield stay fixed."),
    (11, "pastel-de-feira", "Pastel de feira", 5,
     "The scene is a hand holding a golden pastel, a Brazilian fried pastry, in a white napkin, over a blue and white diagonal checkered pattern with navy lines. Extend the checkered pattern above and below so it fills the frame.",
     "The checkered background slides slowly and steadily on the diagonal, moving exactly one square over the length of the clip, so it loops perfectly. The hand holding the pastel bobs up and down gently. The pastel and the napkin keep their shape."),
    (12, "brasil-indigena", "O Brasil é indígena", 5,
     "The scene is horizontal bands of Brazilian indigenous geometric patterns, diamonds, arrows, zigzags and steps, in yellow, blue, green, orange and red on dark navy. Repeat the same bands above and below, so the whole frame is made of horizontal bands.",
     "Each band of pattern slides sideways continuously, like ribbons on a loom: neighbouring bands move in opposite directions at different slow speeds, each by exactly one repeat of its own pattern over the clip, so the loop is seamless. The shapes and colours of the patterns never change."),
    (13, "cores-do-ceu-da-favela", "Cores do céu da favela", 5,
     "The scene is colourful kites (pipas) in red, orange, navy, green and yellow, with white strings and tails, flying in a blue sky above a red favela skyline. Extend the blue sky above and the red skyline below.",
     "The kites dance in the wind: each one bobs, tilts and sways in a small figure of eight at its own rhythm, its tail ripples and its white string curves and straightens. Every kite returns to its starting spot at the end. The skyline stays still."),
    (14, "coracao-pulsa-arte", "O coração pulsa arte", 5,
     "The scene is an anatomical heart drawn in red and white, with leafy branches growing out of it like veins, over a mosaic of triangles in yellow, green, blue and orange. Extend the triangle mosaic above and below.",
     "The heart beats slowly and steadily: with each beat it swells a little and relaxes, and a ripple runs outward through the triangle mosaic, briefly flipping the triangles to other colours of the same palette before they return to their own. The small leaves on the heart flutter with each beat."),
    (15, "surfando-a-onda-boa", "Surfando a onda boa", 5,
     "The scene is a surfer on a green board riding a big curling blue and white wave under a yellow sky, with a small white cloud. Extend the yellow sky above and the blue sea below.",
     "The wave keeps curling over itself in place, its white foam lines rolling continuously, and the surfer rides it on the spot, leaning and balancing with small movements of the arms. The little cloud bobs slightly. The surfer stays in the same place in the frame."),
]

# A mesma composicao do artigos.json do painel (campo "artes" de cada kit).
KITS = [
    ("3-tons", "Kit 3 tons", ["filtro-de-barro", "vira-lata-caramelo", "pastel-de-feira"]),
    ("artes", "Kit artes", ["essencia-do-brasil", "pixacao-estilo-brasileiro", "coracao-pulsa-arte"]),
    ("atletas", "Kit atletas", ["formula-1", "futebol-do-brasil", "surfando-a-onda-boa"]),
    ("fauna", "Kit fauna", ["flora-brasileira", "fauna-brasileira", "brasil-indigena"]),
    ("favela", "Kit favela", ["mosaico-da-favela", "retratos-da-favela", "cores-do-ceu-da-favela"]),
]

# O que mexe em cada uma, para conferir o resultado do Gemini a olho.
MEXE_PT = {
    1: "Os filtros fazem a onda do estádio, da esquerda para a direita, duas vezes.",
    2: "Folhas a baloiçar, o rio a correr, o pássaro amarelo a dar uma volta.",
    3: "Janelas a acender e a apagar; umas fachadas trocam de cor e voltam.",
    4: "A tinta corre dentro das fitas, no sentido das setas.",
    5: "O carro corre parado: rodas a girar, pista a passar, bandeira ao vento.",
    6: "O cão respira, abana a cauda, mexe uma orelha; um brilho passa no espelho.",
    7: "Cada bicho pisca e mexe a cabeça; o beija-flor paira a bater as asas.",
    8: "Os ténis baloiçam pendurados nos fios, cada par ao seu ritmo.",
    9: "A silhueta pinta traços novos que depois se apagam; nada se lê.",
    10: "A bola gira e salta no sítio; a relva ondula; as estrelas piscam.",
    11: "O xadrez desliza na diagonal um quadrado inteiro; a mão balança.",
    12: "Cada faixa de padrão corre para o lado, alternando o sentido.",
    13: "As pipas dançam ao vento e as rabiolas ondulam.",
    14: "O coração bate, e cada batida passa uma onda pelos triângulos.",
    15: "A onda enrola sem parar e o surfista equilibra-se no sítio.",
}


def ficheiro(n, chave, mm):
    return f"{n:02d}-{chave}-{mm}mm"


def temas():
    for n, chave, titulo, mm, cena, mov in TEMAS:
        yield {
            "n": n, "chave": chave, "titulo": titulo, "mm": mm,
            "mexe": MEXE_PT[n], "ficheiro": ficheiro(n, chave, mm),
            "imagem": BASE_IMAGEM.format(cena=cena), "video": BASE_VIDEO.format(movimento=mov),
        }


def markdown():
    por = {t["chave"]: t for t in temas()}
    out = ["# Loops das piteiras: prompts do Gemini", "",
           "Gerado por `scripts/prompts-piteiras.py`. Muda-se lá, não aqui.", "",
           "Passo 1 no Gemini (criar imagem) com `16x9/<ficheiro>.jpg` e o prompt de imagem.",
           "Passo 2 no Gemini (vídeo) ou no Flow com a imagem limpa e o prompt de vídeo.",
           "Passo 3: `./scripts/fechar-loop.sh clipe.mp4 <chave>`. Passo 4, com os três temas do kit: `./scripts/cena-kit.sh <kit>`.", ""]
    for kit, nome, chaves in KITS:
        out += [f"## {nome}", "", f"Cena: `./scripts/cena-kit.sh {kit}`", ""]
        for c in chaves:
            t = por[c]
            out += [f"### {t['n']:02d} · {t['titulo']} ({t['mm']} mm)", "",
                    f"Imagem: `16x9/{t['ficheiro']}.jpg` · chave: `{t['chave']}`  ", f"O que mexe: {t['mexe']}", "",
                    "**Passo 1, imagem**", "", "```", t["imagem"], "```", "",
                    "**Passo 2, vídeo**", "", "```", t["video"], "```", ""]
    return "\n".join(out)


def main():
    md = os.path.join(RAIZ, "arte/piteiras/PROMPTS.md")
    with open(md, "w") as f:
        f.write(markdown())
    print(os.path.relpath(md, RAIZ))
    if len(sys.argv) > 1:
        # A pagina le os dados de um bloco JSON; o molde fica no ficheiro que se publica.
        dados = {"kits": [{"kit": k, "nome": n, "temas": c} for k, n, c in KITS],
                 "temas": {t["chave"]: t for t in temas()}}
        destino = sys.argv[1]
        molde = open(destino).read()
        marca_a, marca_b = '<script id="dados" type="application/json">', "</script>"
        i = molde.index(marca_a) + len(marca_a)
        j = molde.index(marca_b, i)
        js = json.dumps(dados, ensure_ascii=False).replace("</", "<\\/")
        with open(destino, "w") as f:
            f.write(molde[:i] + js + molde[j:])
        print(destino)


if __name__ == "__main__":
    main()
