// Grava a maqueta animada: 1920x1080, 24 s, e tira uma imagem parada por passo. node gravar.cjs <pasta> [--sem-video]
const { chromium } = require("playwright"); const path = require("path"); const fs = require("fs");
const D = process.argv[2], semVideo = process.argv.includes("--sem-video");
(async () => {
  const b = await chromium.launch({ args: ["--autoplay-policy=no-user-gesture-required"] });
  const opc = { viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 };
  if (!semVideo) { fs.rmSync(path.join(D, "video"), { recursive: true, force: true }); opc.recordVideo = { dir: path.join(D, "video"), size: { width: 1920, height: 1080 } }; }
  const ctx = await b.newContext(opc);
  const p = await ctx.newPage();
  p.on("console", (m) => { if (m.type() === "error") console.log("consola:", m.text()); });
  p.on("pageerror", (e) => console.log("erro:", e.message));
  await p.goto("file://" + path.join(D, "tv.html")); await p.evaluate(() => document.fonts.ready); await p.waitForTimeout(1200);
  const t0 = Date.now(); await p.evaluate(() => window.correr());
  for (const [t, n] of [[2900, "1-flor"], [6700, "2-flor"], [10600, "3-piteira"], [14400, "4-kit"], [18200, "5-grinder"], [22200, "6-cuia"]]) {
    await p.waitForTimeout(Math.max(0, t - (Date.now() - t0)));
    await p.screenshot({ path: path.join(D, `quadro-${n}.png`) });
  }
  await p.waitForTimeout(Math.max(0, 24500 - (Date.now() - t0)));
  await ctx.close(); await b.close();
  if (!semVideo) console.log(fs.readdirSync(path.join(D, "video")).map((f) => path.join(D, "video", f)).join("\n"));
})();
