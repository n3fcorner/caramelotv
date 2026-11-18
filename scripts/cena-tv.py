#!/usr/bin/env python3
# A cena de uma piteira (ou de um kit) para o ecra inicial da TV (18/11/2026, maqueta 4 aprovada pelo Francisco).
#
# A TCL da loja so aguenta um video de cada vez, e o ecra inicial mostra a arte animada da piteira a ecra inteiro
# e, por cima, a piteira a rodar numa janela da marca. Por isso as duas coisas juntam-se aqui num video so:
#   - a arte: o loop do Flow da piteira (videos/loops/<tema>-16x9.mp4, fechado pelo fechar-loop.sh); num kit, os
#     tres temas seguem-se com meio segundo de fundido e o ultimo funde de volta no primeiro;
#   - o giro: o video de rotacao do site (public/3d/artigos/giro/<slug>.mp4), dentro do rectangulo da janela.
# A moldura, a sombra, o reflexo e a etiqueta do preco desenham-se na app por cima, no mesmo sitio (Grelha.kt):
# a janela tem 260 x 250 dp a partir de (456, 80) dp, com 2,5 dp de contorno; por dentro, em pixeis deste video
# (1280 x 720, 4/3 px por dp), o rectangulo e' JANELA.
# Temas sem arte (ainda nao gerada, ou retida por decisao) ficam com o fundo azul da marca, so com o giro.
#
# Fecha em loop: a arte fecha sozinha e o giro da um numero inteiro de voltas no mesmo tempo.
#   python3 scripts/cena-tv.py piteira-vidro-flora-brasileira-7mm [...]   ou   --todas   ou   --manifesto (so o manifesto)
# Escreve <destino>/<slug>.mp4 e <slug>.jpg (destino por omissao: ../caramelo-b2b/saas/public/video/tv) e o
# manifesto ../caramelo-b2b/saas/src/dados/cenas-tv.json (slug -> caminho no site).
import hashlib, json, os, subprocess, sys, tempfile

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SITE = os.path.abspath(os.path.join(RAIZ, "..", "caramelo-b2b", "saas"))
GIROS = os.path.join(SITE, "public/3d/artigos/giro")
LOOPS = os.path.join(RAIZ, "videos/loops")
DESTINO = os.environ.get("DESTINO", os.path.join(SITE, "public/video/tv"))
MANIFESTO = os.path.join(SITE, "src/dados/cenas-tv.json")
W, H, FPS = 1280, 720, 24
L, X = 192, 12              # fotogramas de cada tema (8 s) e do fundido entre temas (0,5 s)
VOLTA = 144                 # uma volta do giro perto dos 6 s
JANELA = (611, 110, 340, 327)
FUNDO = "0x0a2c4d"
# Decisoes de 18/11/2026: a Pixacao volta ao Flow (as letras sairam baralhadas) e a Futebol espera pelo Leo (o
# escudo imita o da CBF). Ficam sem arte ate la; tira-se daqui quando houver loop aprovado.
RETIDOS = {"pixacao-estilo-brasileiro", "futebol-do-brasil"}
KITS = {
    "3-tons": ["filtro-de-barro", "vira-lata-caramelo", "pastel-de-feira"],
    "artes": ["essencia-do-brasil", "pixacao-estilo-brasileiro", "coracao-pulsa-arte"],
    "atletas": ["formula-1", "futebol-do-brasil", "surfando-a-onda-boa"],
    "fauna": ["flora-brasileira", "fauna-brasileira", "brasil-indigena"],
    "favela": ["mosaico-da-favela", "retratos-da-favela", "cores-do-ceu-da-favela"],
}

def ff(*a):
    subprocess.run(["ffmpeg", "-v", "error", "-y", *a], check=True)

def n_fotogramas(f):
    return int(subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-count_packets", "-show_entries",
                               "stream=nb_read_packets", "-of", "csv=p=0", f], capture_output=True, text=True, check=True).stdout)

def temas_de(slug):
    if slug.startswith("kit-piteiras-"):
        return KITS[slug[len("kit-piteiras-"):]]
    return [slug[len("piteira-vidro-"):].rsplit("-", 1)[0]]

def cena(slug, tmp):
    giro = os.path.join(GIROS, slug + ".mp4")
    if not os.path.exists(giro):
        print(f"  {slug}: sem giro, fica de fora"); return None
    temas = [t for t in temas_de(slug) if t not in RETIDOS and os.path.exists(os.path.join(LOOPS, t + "-16x9.mp4"))]
    # cada tema com exactamente L fotogramas, a 1280 x 720
    partes = []
    for i, t in enumerate(temas):
        f = os.path.join(LOOPS, t + "-16x9.mp4"); n = n_fotogramas(f); p = os.path.join(tmp, f"t{i}.mp4")
        ff("-i", f, "-vf", f"setpts=(PTS-STARTPTS)*{L}/{n},fps={FPS},trim=end_frame={L},scale={W}:{H}:flags=lanczos,setsar=1,format=yuv420p",
           "-an", "-c:v", "libx264", "-crf", "14", "-preset", "fast", p)
        partes.append(p)
    arte = os.path.join(tmp, "arte.mp4")
    if not partes:
        total = VOLTA
        ff("-f", "lavfi", "-i", f"color=c={FUNDO}:s={W}x{H}:r={FPS}", "-frames:v", str(total), "-c:v", "libx264", "-crf", "14", "-pix_fmt", "yuv420p", arte)
    elif len(partes) == 1:
        total = 3 * L   # 24 s: a arte da tres voltas e o giro quatro
        ff("-stream_loop", "2", "-i", partes[0], "-frames:v", str(total), "-c:v", "libx264", "-crf", "14", "-preset", "fast", "-pix_fmt", "yuv420p", arte)
    else:
        # t1 -> t2 -> ... -> t1 outra vez, em fundidos de X fotogramas; corta-se do t1[X+1] ate ao t1[X] do segundo t1,
        # ja fora do fundido: a volta seguinte continua sem salto. (Fundir o fim
        # da cadeia no principio, como no fechar-loop.sh, nao chegava ao t1 no ultimo fotograma.)
        cadeia = partes + [partes[0]]
        ent = []; [ent.extend(["-i", p]) for p in cadeia]
        g = ""; ult = "[0:v]"; dur = L
        for i in range(1, len(cadeia)):
            g += f"{ult}[{i}:v]xfade=transition=fade:duration={X / FPS:.4f}:offset={(dur - X) / FPS:.4f}[s{i}];"
            ult = f"[s{i}]"; dur = dur + L - X
        total = len(partes) * (L - X)
        g += f"{ult}trim=start_frame={X + 1}:end_frame={X + 1 + total},setpts=PTS-STARTPTS[laco]"
        ff(*ent, "-filter_complex", g, "-map", "[laco]", "-frames:v", str(total), "-c:v", "libx264", "-crf", "14", "-preset", "fast", "-pix_fmt", "yuv420p", arte)
    voltas = max(1, round(total / VOLTA)); v = total / voltas
    ng = n_fotogramas(giro)
    jx, jy, jw, jh = JANELA
    lado = max(jw, jh) + 4   # um nada maior que a janela: o contorno da app tapa a borda
    # uma volta do giro (ng fotogramas a 30 fps) tem de durar v fotogramas desta cena (a 24 fps)
    g = (f"[1:v]setpts=(PTS-STARTPTS)*{v}*30/({ng}*{FPS}),fps={FPS},scale={lado}:{lado}:flags=lanczos,"
         f"crop={jw + 2}:{jh + 2},trim=end_frame={total}[giro];"
         f"[0:v][giro]overlay={jx - 1}:{jy - 1}:shortest=1,format=yuv420p[v]")
    os.makedirs(DESTINO, exist_ok=True)
    mp4 = os.path.join(DESTINO, slug + ".mp4"); jpg = os.path.join(DESTINO, slug + ".jpg")
    ff("-i", arte, "-stream_loop", str(voltas), "-i", giro, "-filter_complex", g, "-map", "[v]", "-frames:v", str(total),
       "-c:v", "libx264", "-crf", "23", "-preset", "slow", "-profile:v", "high", "-level", "4.0", "-movflags", "+faststart", "-an", mp4)
    ff("-i", mp4, "-frames:v", "1", "-q:v", "3", jpg)
    print(f"  {slug}: {len(temas)} tema(s) {temas}, {total} fotogramas ({total / FPS:.1f} s), {voltas} voltas, {os.path.getsize(mp4) // 1024} KB")
    return caminho(slug)

def caminho(slug):
    # com um resumo do conteudo: os /video/ do site ficam uma semana em cache, e uma cena refeita tem o mesmo nome
    h = hashlib.md5(open(os.path.join(DESTINO, slug + ".mp4"), "rb").read()).hexdigest()[:8]
    return f"/video/tv/{slug}.mp4?v={h}"

alvo = sys.argv[1:]
if alvo == ["--todas"]:
    alvo = sorted(f[:-4] for f in os.listdir(GIROS) if f.endswith(".mp4"))
man = json.load(open(MANIFESTO)) if os.path.exists(MANIFESTO) else {}
if alvo == ["--manifesto"]:   # so refaz o manifesto a partir das cenas que ja existem
    man = {f[:-4]: caminho(f[:-4]) for f in os.listdir(DESTINO) if f.endswith(".mp4")}; alvo = []
with tempfile.TemporaryDirectory() as tmp:
    for s in alvo:
        c = cena(s, tmp)
        if c: man[s] = c
open(MANIFESTO, "w").write(json.dumps(dict(sorted(man.items())), indent=1, ensure_ascii=False) + "\n")
print(f"manifesto: {len(man)} cenas")
