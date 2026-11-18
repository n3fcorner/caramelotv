# As camadas do frasco para a app (18/11/2026), a partir do render do frasco.py:
#   - o contorno a tinta (o traco preto grosso do mundo ilustrado), tirado da silhueta: o do frasco vai no vidro da frente
#     e o da tampa em cada tampa, para o traco subir com ela;
#   - a reducao ao tamanho da app (400 x 550, o maior a que o frasco se desenha) em WebP sem perdas, para res/drawable-nodpi;
#   - as fracoes do interior e da boca, para o FrascoGeo.
#   python3 -I scripts/frasco/camadas.py <pasta do render> <pasta res/drawable-nodpi>
import json, os, sys
import numpy as np, cv2
from PIL import Image

ENT, SAI = sys.argv[1], sys.argv[2]
LARGO, ALTO = 400, 550
TINTA = (7, 38, 69)  # #072645, a tinta do manual

def ler(n):
    return np.array(Image.open(os.path.join(ENT, n)).convert("RGBA"))

def contorno(alfa, espessura):
    m = (alfa > 90).astype(np.uint8)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7)))
    cs, _ = cv2.findContours(m, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    linha = np.zeros(alfa.shape, np.uint8)
    cv2.drawContours(linha, cs, -1, 255, espessura, lineType=cv2.LINE_AA)
    return linha

def com_linha(rgba, linha):
    a = linha.astype(np.float32)[..., None] / 255
    out = rgba.astype(np.float32).copy()
    out[..., :3] = out[..., :3] * (1 - a) + np.array(TINTA, np.float32) * a
    out[..., 3:] = np.maximum(out[..., 3:], linha[..., None].astype(np.float32))
    return out.clip(0, 255).astype(np.uint8)

def gravar(rgba, nome):
    im = Image.fromarray(rgba, "RGBA").resize((LARGO, ALTO), Image.LANCZOS)
    im.save(os.path.join(SAI, nome), "WEBP", lossless=True, quality=100, method=6)
    print(nome, os.path.getsize(os.path.join(SAI, nome)) // 1024, "KB")

tras, frente = ler("frasco_tras.png"), ler("frasco_frente.png")
H, W = tras.shape[:2]
esp = max(3, round(W / 160))  # ~2,5 px depois de reduzir
corpo = np.maximum(tras[..., 3], frente[..., 3])
gravar(tras, "frasco_tras.webp")
gravar(com_linha(frente, contorno(corpo, esp)), "frasco_frente.webp")
for cor in ("verde", "roxo", "laranja"):
    t = ler(f"tampa_{cor}.png")
    gravar(com_linha(t, contorno(t[..., 3], esp)), f"frasco_tampa_{cor}.webp")

g = json.load(open(os.path.join(ENT, "geometria.json")))
print(json.dumps({k: g[k] for k in ("interior", "boca", "tampa_y", "fundo_y")}))
