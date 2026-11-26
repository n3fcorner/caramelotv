#!/usr/bin/env python3
# Tapa um pormenor de um loop do Flow, fotograma a fotograma (18/11/2026, Francisco: "tapo eu o cigarro").
# O tucano da arte Fauna Brasileira tem um cigarro no bico; a regra de copy da TV nao deixa fumo. Dentro de uma
# zona pequena (o rectangulo ZONA, em pixeis do video), os pixeis claros e pouco saturados sao o cigarro: saem com o
# contorno escuro a volta (dilatado) e o buraco enche-se linha a linha a partir dos lados. Fora da zona
# nada muda. O loop continua a fechar, porque cada fotograma e tratado sozinho.
#
#   python3 scripts/tapar.py videos/loops/fauna-brasileira-16x9.mp4 1805,488,1872,568
#   (escreve por cima, guardando o original em <nome>.original.mp4)
import os, shutil, subprocess, sys
import cv2
import numpy as np

entrada = sys.argv[1]
x0, y0, x1, y1 = map(int, sys.argv[2].split(","))
CLARO_V, CLARO_S, BORDA = 165, 70, 5

info = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height,r_frame_rate",
                       "-of", "csv=p=0", entrada], capture_output=True, text=True, check=True).stdout.strip().split(",")
W, H, fps = int(info[0]), int(info[1]), info[2]
raw = subprocess.run(["ffmpeg", "-v", "error", "-i", entrada, "-f", "rawvideo", "-pix_fmt", "bgr24", "-"], capture_output=True, check=True).stdout
quadros = np.frombuffer(raw, np.uint8).reshape(-1, H, W, 3).copy()

nucleo = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * BORDA + 1, 2 * BORDA + 1))
tapados = 0
for q in quadros:
    zona = q[y0:y1, x0:x1]
    hsv = cv2.cvtColor(zona, cv2.COLOR_BGR2HSV)
    claro = ((hsv[..., 2] > CLARO_V) & (hsv[..., 1] < CLARO_S)).astype(np.uint8) * 255
    if not claro.any():
        continue
    mascara = cv2.dilate(claro, nucleo) > 0
    # Linha a linha: o troco tapado e' a interpolacao entre o pixel logo a esquerda e o logo a direita. Ali as
    # estruturas (o verde, a faixa laranja do bico, o contorno) sao quase horizontais, por isso continuam sem mancha;
    # o inpaint de Telea misturava-as num borrao.
    z = zona.astype(np.float32)
    for y in range(mascara.shape[0]):
        xs = np.where(mascara[y])[0]
        if not len(xs): continue
        # cada troco seguido da linha
        cortes = np.split(xs, np.where(np.diff(xs) > 1)[0] + 1)
        for c in cortes:
            a_, b_ = max(0, c[0] - 2), min(mascara.shape[1] - 1, c[-1] + 2)
            t = (np.arange(c[0], c[-1] + 1) - a_) / max(1, b_ - a_)
            z[y, c[0]:c[-1] + 1] = z[y, a_] * (1 - t[:, None]) + z[y, b_] * t[:, None]
    liso = cv2.GaussianBlur(z, (3, 3), 0)
    z[mascara] = liso[mascara]
    q[y0:y1, x0:x1] = np.clip(z, 0, 255).astype(np.uint8)
    tapados += 1

original = entrada.replace(".mp4", ".original.mp4")
if not os.path.exists(original):
    shutil.copy(entrada, original)
saida = entrada + ".tmp.mp4"
p = subprocess.Popen(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "bgr24", "-s", f"{W}x{H}", "-r", fps, "-i", "-",
                      "-c:v", "libx264", "-crf", "16", "-preset", "slow", "-pix_fmt", "yuv420p", "-movflags", "+faststart", "-an", saida],
                     stdin=subprocess.PIPE)
p.stdin.write(quadros.tobytes()); p.stdin.close(); p.wait()
os.replace(saida, entrada)
print(f"{len(quadros)} fotogramas, {tapados} tapados na zona {x0},{y0}-{x1},{y1}")
