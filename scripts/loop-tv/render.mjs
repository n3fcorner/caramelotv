// O troço de mapa do loop da TV (21/10/2026): a cena encolhe até ser um postal no sítio de onde a pessoa vem, num globo nas cores do
// manual, e um tracejado leva-a até Lisboa com a cara na ponta. Desenha cada fotograma no Chromium (mapa.html) e passa-o ao ffmpeg.
//   npm install && node render.mjs [--cena ultimo-fotograma.png] [--saida troco.mp4] [--segundos 7.5]
// Sem --cena usa a cena provisória (cena.html). No loop a cena é o último fotograma do clipe do Flow (ffmpeg -sseof -0.1 -i clipe.mp4
// -frames:v 1 ultimo.png). A pessoa, a origem e a vista estão em window.TROCO no mapa.html; os prompts das cenas vivem no caramelo-b2b,
// em saas/src/dados/prompts-gemini.js (PERSONAGENS_TV, CENAS_TV, finalTV). As fontes e o cão vêm do caramelo-b2b ao lado deste repositório.
import { chromium } from "playwright-core";
import { spawn } from "node:child_process";
import { readFileSync, existsSync, copyFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const arg = (k, d) => { const i = process.argv.indexOf(k); return i > 0 ? process.argv[i + 1] : d; };
const saida = path.resolve(arg("--saida", path.join(AQUI, "troco.mp4")));
const DUR = +arg("--segundos", 7.5), FPS = 30;
const cena = arg("--cena");

const exe = process.env.CHROME || ["/opt/pw-browsers/chromium", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"].find((f) => existsSync(f));
const browser = await chromium.launch({ ...(exe ? { executablePath: exe } : {}), args: ["--allow-file-access-from-files"] });
const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });

// a imagem do postal: a do clipe, ou a provisória
const alvo = path.join(AQUI, "cena-postal.png");
if (cena) copyFileSync(path.resolve(cena), alvo);
else {
  await page.goto("file://" + path.join(AQUI, "cena.html"));
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(300);
  await page.screenshot({ path: alvo });
}

await page.goto("file://" + path.join(AQUI, "mapa.html"));
const topo = JSON.parse(readFileSync(path.join(AQUI, "node_modules/world-atlas/countries-50m.json"), "utf8"));
await page.evaluate((t) => { window.preparar(t); window.TROCO.cena = "cena-postal.png"; }, topo);
await page.evaluate(() => document.fonts.ready);

const ff = spawn("ffmpeg", ["-v", "error", "-y", "-f", "image2pipe", "-framerate", String(FPS), "-i", "-", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "20", "-preset", "medium", "-movflags", "+faststart", saida], { stdio: ["pipe", "inherit", "inherit"] });
const N = Math.round(DUR * FPS);
for (let i = 0; i < N; i++) {
  await page.evaluate((t) => window.frame(t), i / FPS);
  if (i === 0) await page.waitForTimeout(400); // o postal e a cara têm de estar carregados antes do primeiro fotograma
  const buf = await page.screenshot({ type: "png" });
  if (!ff.stdin.write(buf)) await new Promise((r) => ff.stdin.once("drain", r));
}
ff.stdin.end();
await new Promise((r) => ff.on("close", r));
await browser.close();
console.log(saida, N, "fotogramas");
