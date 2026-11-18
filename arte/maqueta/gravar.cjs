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
  for (const [t, n] of [[2900, "1-cbd"], [6500, "2-infusao"], [13900, "3-hash"], [17300, "4-piteira"], [20900, "5-kit"], [24200, "6-grinder"], [27900, "7-cuia"]]) {
    await p.waitForTimeout(Math.max(0, t - (Date.now() - t0)));
    await p.screenshot({ path: path.join(D, `quadro-${n}.png`) });
  }
  await p.waitForTimeout(Math.max(0, 29300 - (Date.now() - t0)));
  await ctx.close(); await b.close();
  if (!semVideo) console.log(fs.readdirSync(path.join(D, "video")).map((f) => path.join(D, "video", f)).join("\n"));
})();
