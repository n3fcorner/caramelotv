// As tres piteiras de um kit a rodar, com fundo transparente, para irem por cima
// das animacoes dos temas (scripts/cena-kit.sh).
//
//   node scripts/kit-transparente.mjs atletas 156
//   node scripts/kit-transparente.mjs atletas 156 /caminho/para/caramelo-b2b
//
// O segundo argumento e' o numero de fotogramas de UMA volta. O cena-kit.sh e'
// quem o calcula, para a volta caber um numero certo de vezes na cena e esta
// fechar em loop. Escreve videos/loops/kit-<nome>/000.png ... com alfa.
//
// E' o mesmo visualizador dos giros da app (caramelo-b2b/saas/scripts/3d/giros.mjs:
// a mesma luz, o mesmo ambiente, a mesma inclinacao, as tres deitadas a rolar
// sobre o eixo comprido), com duas diferencas. O fundo e' transparente em vez do
// azul pintado, porque o fundo agora e' a animacao. E a sombra fica, mas mais
// leve: e' ela que assenta as pecas no desenho em vez de parecerem coladas.
//
// Os modelos e o three.js vem do repositorio do painel, que fica ao lado deste.
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium } = require("playwright");

const RAIZ = path.join(path.dirname(new URL(import.meta.url).pathname), "..");
const [kit, voltaArg, b2bArg] = process.argv.slice(2);
if (!kit || !voltaArg) { console.error("uso: kit-transparente.mjs <kit> <fotogramas por volta> [caramelo-b2b]"); process.exit(1); }
const VOLTA = Number(voltaArg);
const LADO = Number(process.env.LADO || 1080);
const PUB = path.join(b2bArg || path.join(RAIZ, "../caramelo-b2b"), "saas/public");
const GLB = `/3d/artigos/kit-piteiras-${kit}.glb`;
if (!fs.existsSync(path.join(PUB, GLB))) { console.error("nao encontro", path.join(PUB, GLB)); process.exit(1); }

const mac = process.platform === "darwin";
const ARGS = mac ? ["--use-angle=metal", "--ignore-gpu-blocklist"]
  : ["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist", "--no-sandbox"];

const HTML = `<!doctype html><html><head><meta charset="utf8"><style>html,body{margin:0;height:100%;overflow:hidden;background:transparent}
#c{display:block;width:100vw;height:100vh}</style></head><body><canvas id="c"></canvas><script type="module">
import * as THREE from "/js/vendor/three/three.module.min.js";
import { GLTFLoader } from "/js/vendor/three/GLTFLoader.js";
import { RoomEnvironment } from "/js/vendor/three/RoomEnvironment.js";
const LADO = ${LADO};
const r = new THREE.WebGLRenderer({ canvas: document.getElementById("c"), antialias: true, alpha: true, preserveDrawingBuffer: true, premultipliedAlpha: false });
r.setClearColor(0x000000, 0); r.setPixelRatio(1); r.setSize(LADO, LADO, false);
r.outputColorSpace = THREE.SRGBColorSpace; r.toneMapping = THREE.NoToneMapping;
r.shadowMap.enabled = true; r.shadowMap.type = THREE.PCFSoftShadowMap;
const cena = new THREE.Scene(); const pm = new THREE.PMREMGenerator(r); cena.environment = pm.fromScene(new RoomEnvironment(), 0.04).texture;
const sol = new THREE.DirectionalLight(0xfff2dc, 1.5); sol.position.set(50,140,70); sol.castShadow = true; sol.shadow.mapSize.set(1024,1024); sol.shadow.radius = 8;
Object.assign(sol.shadow.camera, { left:-120, right:120, top:120, bottom:-120, near:1, far:600 }); cena.add(sol);
const chao = new THREE.Mesh(new THREE.PlaneGeometry(900,900), new THREE.ShadowMaterial({ opacity: 0.28 })); chao.rotation.x = -Math.PI/2; chao.receiveShadow = true; cena.add(chao);
const cam = new THREE.PerspectiveCamera(26,1,1,4000); const pivo = new THREE.Group(); cena.add(pivo);
const yaw = -0.35, pitch = 0.5; let raio = 50, altura = 30; const rolos = [];
function coloca(){ const d = (raio/Math.sin(THREE.MathUtils.degToRad(cam.fov/2)))*1.14; const ay = altura*0.5;
  cam.position.set(Math.sin(yaw)*Math.cos(pitch)*d, ay+Math.sin(pitch)*d, Math.cos(yaw)*Math.cos(pitch)*d); cam.lookAt(0,ay,0); }
new GLTFLoader().load("${GLB}", (g) => { const m = g.scene; m.scale.setScalar(1000); m.traverse(o=>{ if(o.isMesh){o.castShadow=true;o.receiveShadow=true;} });
  const bx = new THREE.Box3().setFromObject(m), c = bx.getCenter(new THREE.Vector3()); m.position.sub(new THREE.Vector3(c.x, bx.min.y, c.z)); pivo.add(m);
  raio = bx.getBoundingSphere(new THREE.Sphere()).radius; altura = bx.max.y - bx.min.y;
  pivo.updateMatrixWorld(true);
  for (const o of [...m.children]) { const bb = new THREE.Box3().setFromObject(o); if (bb.isEmpty()) continue; const t = bb.getSize(new THREE.Vector3());
    const gg = new THREE.Group(); gg.position.copy(m.worldToLocal(bb.getCenter(new THREE.Vector3()))); m.add(gg); gg.attach(o); rolos.push({ g: gg, eixo: t.x>=t.z?"x":"z" }); }
  coloca(); r.render(cena,cam); window.__ready = true;
}, undefined, (e) => { window.__erro = String((e&&e.message)||e); });
window.setAngle = (i, n) => { const a = 2*Math.PI*i/n; for (const o of rolos) o.g.rotation[o.eixo] = a; r.render(cena,cam); return true; };
<\/script></body></html>`;

const MIME = { ".js": "text/javascript", ".glb": "model/gltf-binary" };
const srv = http.createServer((req, res) => {
  const u = decodeURIComponent(req.url.split("?")[0]);
  if (u === "/kit.html") { res.writeHead(200, { "content-type": "text/html" }); res.end(HTML); return; }
  fs.readFile(path.join(PUB, u), (e, b) => {
    if (e) { res.writeHead(404); res.end(); return; }
    res.writeHead(200, { "content-type": MIME[path.extname(u)] || "application/octet-stream" }); res.end(b);
  });
});
await new Promise((ok) => srv.listen(0, "127.0.0.1", ok));
const porta = srv.address().port;

const dir = path.join(RAIZ, "videos/loops", `kit-${kit}`);
fs.rmSync(dir, { recursive: true, force: true }); fs.mkdirSync(dir, { recursive: true });
const b = await chromium.launch({ headless: true, args: ARGS });
const pg = await (await b.newContext({ viewport: { width: LADO, height: LADO }, deviceScaleFactor: 1 })).newPage();
await pg.goto(`http://127.0.0.1:${porta}/kit.html`);
await pg.waitForFunction("window.__ready === true || window.__erro", null, { timeout: 30000 });
const erro = await pg.evaluate("window.__erro || null");
if (erro) { console.error("FALHOU", kit, erro); process.exit(1); }
for (let i = 0; i < VOLTA; i++) {
  await pg.evaluate(([i, n]) => window.setAngle(i, n), [i, VOLTA]);
  await pg.screenshot({ path: path.join(dir, String(i).padStart(3, "0") + ".png"), omitBackground: true });
}
await b.close(); srv.close();
console.log(`kit-${kit}: ${VOLTA} fotogramas em ${path.relative(RAIZ, dir)}`);
