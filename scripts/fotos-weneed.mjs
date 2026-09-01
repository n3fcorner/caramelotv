#!/usr/bin/env node
// Recolhe as fotos de produto da WeNeed para usar como cartaz de cada strain.
//
//   node scripts/fotos-weneed.mjs
//
// O site e feito em Webflow e a lista de produtos e montada por JavaScript, por
// isso nao ha uma API para consultar. As imagens, essas, vem servidas do CDN do
// Webflow com o nome da strain no proprio ficheiro — e e dai que se pescam.
//
// Cada imagem existe em tres tamanhos: o original e as variantes -p-500 e -p-800.
// Fica-se com o original.

import { execFile } from 'node:child_process'
import { mkdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { promisify } from 'node:util'

const corre = promisify(execFile)

const PAGINAS = [
  'https://www.weneedweed.eu/',
  'https://www.weneedweed.eu/products',
  'https://www.weneedweed.eu/hall-of-fame',
  'https://www.weneedweed.eu/blog',
  'https://www.weneedweed.eu/retailers',
  ...['cali', 'indoor', 'glasshouse', 'greenhouse', 'outdoor', 'hash', 'special']
    .map((c) => `https://www.weneedweed.eu/category/${c}`),
]

// Os nomes no site nao batem certo com os nossos: a Blueberry Small esta como
// BLUBERRY, a Static como STATIC MANGO, e a Berry Pie esta com gralha deles,
// "beryy pie_Edit". Dai a lista de alcunhas e a procura tolerante mais abaixo.
const CATALOGO = {
  'grape-cali': ['grape cali'],
  'lemon-trip': ['lemon trip'],
  'mela-sour': ['mela sour'],
  'solero-cream': ['solero cream'],
  'blueberry-small': ['bluberry', 'blueberry'],
  'bubba-kush': ['bubba kush', 'bubba'],
  'critical-berry': ['critical berry'],
  'smash-cali': ['smash cali'],
  'sour-lifter': ['sour lifter'],
  static: ['static mango', 'static'],
  'sticky-peach-piattella': ['sticky peach piattella', 'sticky peach'],
  uranium: ['uranium'],
  mac: ['mac'],
  'gorilla-mandarini': ['gorilla mandarini', 'gorilla mandarin'],
  'berry-pie': ['berry pie', 'beryy pie'],
  'mango-melt': ['mango melt piattella', 'mango melt'],
}

const AGENTE =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/126 Safari/537.36'

const normaliza = (s) =>
  decodeURIComponent(s)
    .toLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/[^a-z0-9 ]/g, '')
    .replace(/\s+/g, ' ')
    .trim()

async function paginas() {
  const html = []
  for (const url of PAGINAS) {
    try {
      const r = await fetch(url, { headers: { 'User-Agent': AGENTE } })
      if (!r.ok) {
        console.log(`  ${r.status} ${url}`)
        continue
      }
      html.push(await r.text())
    } catch (e) {
      console.log(`  falhou ${url}: ${e.message}`)
    }
  }
  return html
}

function indexaImagens(html) {
  const indice = new Map()
  const padrao = /https:\/\/cdn\.prod\.website-files\.com\/[^"'?\s)]+\.(?:png|jpe?g|webp)/g

  for (const pagina of html) {
    for (const url of pagina.match(padrao) ?? []) {
      const ficheiro = decodeURIComponent(url.split('/').pop())
      // tira o prefixo do Webflow e a extensao
      let nome = ficheiro.replace(/^[0-9a-f]{24}_/, '').replace(/\.\w+$/, '')
      // as variantes -p-500 e -p-800 sao recortes menores da mesma imagem
      const variante = /-p-\d+$/.test(nome)
      nome = normaliza(nome.replace(/-p-\d+$/, ''))
      if (!nome) continue
      // o original ganha sempre a variante
      if (!indice.has(nome) || (!variante && indice.get(nome).variante)) {
        indice.set(nome, { url, variante })
      }
    }
  }
  return indice
}

/**
 * Exacto primeiro; so depois prefixo e conteudo. Sem esta ordem, "mac" apanhava
 * "mango melt" e "static" apanhava qualquer coisa com "sta".
 */
function procura(indice, nomes) {
  for (const n of nomes) if (indice.has(n)) return n
  for (const n of nomes) {
    for (const chave of indice.keys()) if (chave.startsWith(n + ' ')) return chave
  }
  for (const n of nomes) {
    if (n.length < 5) continue
    for (const chave of indice.keys()) if (chave.includes(n)) return chave
  }
  return null
}

async function descarrega(url, destino) {
  const r = await fetch(url, { headers: { 'User-Agent': AGENTE } })
  if (!r.ok) throw new Error(`HTTP ${r.status}`)
  await writeFile(destino, Buffer.from(await r.arrayBuffer()))
}

const DESTINO = 'videos/fotos-weneed'
const CARTAZES = 'videos/web/posters'

// As fotos vem em 1080x1620 com fundo transparente. Ficam melhor recompostas num
// quadrado com o roxo da marca por tras do que recortadas a bruta.
//
// O fundo e exactamente o do mosaico na app, para nao se ver a costura entre a
// foto e o cartao. E a flor assenta em cima, nao ao centro: em baixo passa a
// faixa com o nome, e ao centro ela ficava com o pe tapado.
const FUNDO = '0x1E1433'
const LADO = 720
const ALTURA_FLOR = 0.76
const TOPO = 0.03

async function cartaz(origem, destino) {
  await corre('ffmpeg', [
    '-y', '-nostdin', '-hide_banner', '-loglevel', 'error',
    '-f', 'lavfi', '-i', `color=c=${FUNDO}:s=${LADO}x${LADO}`,
    '-i', origem,
    '-filter_complex',
    `[1]scale=-1:${Math.round(LADO * ALTURA_FLOR)}[b];` +
      `[0][b]overlay=(W-w)/2:${Math.round(LADO * TOPO)}:format=auto`,
    '-frames:v', '1', '-q:v', '3', destino,
  ])
}

console.log(`==> A ler ${PAGINAS.length} páginas da WeNeed`)
const indice = indexaImagens(await paginas())
console.log(`    ${indice.size} imagens de produto no CDN\n`)

await mkdir(DESTINO, { recursive: true })

const encontrados = []
const emFalta = []

for (const [slug, nomes] of Object.entries(CATALOGO)) {
  const alvo = procura(indice, nomes.map(normaliza))
  if (!alvo) {
    emFalta.push(slug)
    continue
  }
  const { url } = indice.get(alvo)
  const extensao = url.split('.').pop()
  const ficheiro = join(DESTINO, `${slug}.${extensao}`)
  try {
    await descarrega(url, ficheiro)
    await cartaz(ficheiro, join(CARTAZES, `${slug}.jpg`))
    encontrados.push(slug)
    console.log(`  ✓ ${slug.padEnd(24)} ← ${alvo}`)
  } catch (e) {
    emFalta.push(slug)
    console.log(`  ✗ ${slug.padEnd(24)} ${e.message}`)
  }
}

console.log(`\n==> ${encontrados.length} de ${Object.keys(CATALOGO).length}`)
console.log(`    originais em ${DESTINO}, cartazes em ${CARTAZES}`)
if (emFalta.length) {
  console.log(`    sem foto na WeNeed: ${emFalta.join(', ')}`)
  console.log('    (a linha de infusão vem de outro fornecedor, não deste site)')
}
