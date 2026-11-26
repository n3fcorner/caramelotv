# O frasco de vidro da colecao (18/11/2026): um frasco de exemplar feito por torno, renderizado uma vez em camadas para a app
# o compor por cima e por baixo da foto recortada de cada flor. Assim uma flor nova no painel entra num frasco sozinha.
#
#   python3 -I scripts/frasco/frasco.py <pasta de saida> [amostras=96] [lado=1100]
#
# Camadas (PNG RGBA, todas com a mesma camara e o mesmo enquadramento, para se empilharem a direito):
#   frasco_tras.png    o vidro de tras (a metade longe da camara) e o fundo grosso
#   frasco_frente.png  o vidro da frente (a metade perto da camara), com os reflexos e a boca
#   tampa_<cor>.png    a tampa sozinha, em cada cor de seccao (para levantar sem o frasco)
#   geometria.json     onde fica o interior (a caixa onde a flor assenta) e a boca, em fracoes da imagem
# O contorno a tinta e a reducao para a app fazem-se depois, em scripts/frasco/camadas.py.
import bpy, bmesh, math, json, os, sys
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view

args = [a for a in sys.argv[1:] if not a.startswith("-")]
OUT = os.path.abspath(args[0] if args else "frasco-out")
AMOSTRAS = int(args[1]) if len(args) > 1 else 96
ALTO = int(args[2]) if len(args) > 2 else 1100
LARGO = round(ALTO * 0.727)
os.makedirs(OUT, exist_ok=True)

bpy.ops.wm.read_factory_settings(use_empty=True)
cena = bpy.context.scene

# --- o perfil do frasco (raio, altura), de fora para dentro; uma volta no torno faz o vidro ---------------------------------
E = 0.014  # espessura do vidro
FORA = [(0.0, 0.0), (0.33, 0.0), (0.355, 0.012), (0.36, 0.04), (0.36, 0.74), (0.355, 0.78), (0.33, 0.83),
        (0.29, 0.865), (0.272, 0.88), (0.272, 0.925), (0.285, 0.93), (0.292, 0.945), (0.285, 0.958), (0.27, 0.96)]
DENTRO = [(0.258, 0.958), (0.258, 0.885), (0.278, 0.865), (0.318, 0.832), (0.343, 0.785), (0.346, 0.745),
          (0.346, 0.04), (0.335, 0.03), (0.0, 0.03)]


def torno(nome, perfil, passos=128):
    me = bpy.data.meshes.new(nome); bm = bmesh.new()
    vs = [bm.verts.new((r, 0.0, z)) for r, z in perfil]
    for a, b in zip(vs, vs[1:]): bm.edges.new((a, b))
    bmesh.ops.spin(bm, geom=bm.verts[:] + bm.edges[:], cent=(0, 0, 0), axis=(0, 0, 1), angle=math.tau, steps=passos, use_duplicate=False)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-5)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(me); bm.free()
    ob = bpy.data.objects.new(nome, me); cena.collection.objects.link(ob)
    for p in me.polygons: p.use_smooth = True
    return ob


vidro = torno("vidro", FORA + DENTRO)

# a tampa: um disco com a borda arredondada, por cima da boca
def tampa():
    perfil = [(0.0, 0.952), (0.296, 0.952), (0.305, 0.958), (0.308, 0.975), (0.305, 1.012), (0.296, 1.02), (0.0, 1.024)]
    return torno("tampa", perfil, 96)

tam = tampa()

# --- materiais ---------------------------------------------------------------------------------------------------------
def mat_vidro():
    m = bpy.data.materials.new("vidro"); m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (0.93, 0.98, 0.96, 1)   # um verde de vidro muito leve
    b.inputs["Roughness"].default_value = 0.03
    b.inputs["IOR"].default_value = 1.47
    b.inputs["Transmission Weight"].default_value = 1.0
    return m

def mat_tampa(rgb):
    m = bpy.data.materials.new("tampa"); m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*rgb, 1)
    b.inputs["Roughness"].default_value = 0.42
    b.inputs["Coat Weight"].default_value = 0.25
    return m

vidro.data.materials.append(mat_vidro())

# --- luz: tres caixas de luz que a camara nao ve, para os reflexos verticais do vidro --------------------------------------
def caixa(nome, pos, rot, tam_xy, forca):
    bpy.ops.mesh.primitive_plane_add(size=1, location=pos, rotation=rot)
    ob = bpy.context.active_object; ob.name = nome; ob.scale = (tam_xy[0], tam_xy[1], 1)
    m = bpy.data.materials.new(nome); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear()
    em = nt.nodes.new("ShaderNodeEmission"); em.inputs["Strength"].default_value = forca
    sai = nt.nodes.new("ShaderNodeOutputMaterial"); nt.links.new(em.outputs[0], sai.inputs[0])
    ob.data.materials.append(m); ob.visible_camera = False
    return ob

caixa("caixa-esq", (-1.6, -0.9, 0.55), (math.radians(90), 0, math.radians(-60)), (0.55, 1.6), 9)
caixa("caixa-dir", (1.7, -0.6, 0.6), (math.radians(90), 0, math.radians(70)), (0.35, 1.6), 5)
caixa("caixa-cima", (0, -0.2, 2.4), (0, 0, 0), (1.4, 1.4), 3)

mundo = bpy.data.worlds.new("mundo"); cena.world = mundo; mundo.use_nodes = True
mundo.node_tree.nodes["Background"].inputs["Color"].default_value = (1.0, 0.97, 0.92, 1)  # uma sala clara e quente: os lados do vidro refletem claro, nao cinzento
mundo.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.55

# --- camara: um pouco de cima, para se ver a boca em elipse -------------------------------------------------------------
cam_d = bpy.data.cameras.new("cam"); cam_d.lens = 85
cam = bpy.data.objects.new("cam", cam_d); cena.collection.objects.link(cam); cena.camera = cam
alvo = Vector((0, 0, 0.51)); dist = 2.95; elev = math.radians(9)
cam.location = alvo + Vector((0, -dist * math.cos(elev), dist * math.sin(elev)))
cam.rotation_euler = (alvo - cam.location).to_track_quat("-Z", "Y").to_euler()

cena.render.engine = "CYCLES"
cena.cycles.device = "CPU"
cena.cycles.samples = AMOSTRAS
cena.cycles.use_denoising = True
cena.render.film_transparent = True
cena.cycles.film_transparent_glass = True
cena.render.resolution_x, cena.render.resolution_y = LARGO, ALTO
cena.render.image_settings.file_format = "PNG"; cena.render.image_settings.color_mode = "RGBA"
cena.view_settings.view_transform = "Standard"
cena.cycles.max_bounces = 16; cena.cycles.transmission_bounces = 16; cena.cycles.glossy_bounces = 8

# --- as metades: corta-se o vidro pelo plano que passa no eixo e e' perpendicular a camara --------------------------------
frente_dir = (Vector((cam.location.x, cam.location.y, 0))).normalized()  # aponta para a camara, no plano do chao

def metade(perto):
    ob = vidro.copy(); ob.data = vidro.data.copy(); cena.collection.objects.link(ob)
    bm = bmesh.new(); bm.from_mesh(ob.data)
    n = frente_dir if perto else -frente_dir
    bmesh.ops.bisect_plane(bm, geom=bm.verts[:] + bm.edges[:] + bm.faces[:], plane_co=(0, 0, 0), plane_no=-n, clear_outer=True)
    bm.to_mesh(ob.data); bm.free()
    return ob

def render(nome, visiveis):
    for ob in cena.objects:
        if ob.type == "MESH" and not ob.name.startswith("caixa"):
            ob.hide_render = ob not in visiveis
    cena.render.filepath = os.path.join(OUT, nome)
    bpy.ops.render.render(write_still=True)
    print("render", nome)

tras = metade(False); frente = metade(True)
vidro.hide_render = True
render("frasco_tras.png", [tras])
render("frasco_frente.png", [frente])

CORES = {"verde": (0.0, 0.287, 0.05), "roxo": (0.18, 0.09, 0.38), "laranja": (0.91, 0.24, 0.05)}  # #00923F, #7554A3, #F58634 em linear
for nome, rgb in CORES.items():
    tam.data.materials.clear(); tam.data.materials.append(mat_tampa(rgb))
    render(f"tampa_{nome}.png", [tam])

# --- geometria: o interior (onde a flor assenta) e a boca, em fracoes da imagem ------------------------------------------
def proj(p):
    v = world_to_camera_view(cena, cam, Vector(p)); return (v.x, 1 - v.y)

r_in = 0.33; base = 0.045; ombro = 0.84
cantos = [proj((s * r_in, 0, z)) for s in (-1, 1) for z in (base, ombro)]
boca = [proj((s * 0.27, 0, 0.955)) for s in (-1, 1)]
tampa_cima = proj((0, 0, 1.024)); fundo = proj((0, 0, 0.0))
geo = {
    "interior": {"x0": min(c[0] for c in cantos), "x1": max(c[0] for c in cantos), "y0": min(c[1] for c in cantos), "y1": max(c[1] for c in cantos)},
    "boca": {"x0": boca[0][0], "x1": boca[1][0], "y": boca[0][1]},
    "tampa_y": tampa_cima[1], "fundo_y": fundo[1], "largura": LARGO, "altura": ALTO,
}
json.dump(geo, open(os.path.join(OUT, "geometria.json"), "w"), indent=1)
print(json.dumps(geo))
