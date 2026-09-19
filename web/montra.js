// Montra da Caramelo do Brasil para telemovel. Sem compilacao: o catalogo vem de
// catalogo.json e os videos de MEDIA, para trocar de alojamento ser uma edicao so.

const MEDIA = window.CARAMELO_MEDIA || 'media';

const montra = document.getElementById('montra');
const ficha = document.getElementById('ficha');

const cartaz = (slug) => `${MEDIA}/posters/${slug}.webp`;
const video = (slug) => `${MEDIA}/${slug}.mp4`;

// O catalogo inteiro, achatado, para a ficha saber quem vem antes e depois.
let seccoes = [];

fetch('catalogo.json')
  .then((r) => r.json())
  .then((dados) => {
    seccoes = dados.seccoes;
    desenhar(dados);
    abrirPeloEndereco(true);
  })
  .catch(() => {
    montra.innerHTML = '<p style="padding:18px">Não foi possível carregar o catálogo.</p>';
  });

function desenhar(dados) {
  montra.innerHTML = '';
  const nav = document.getElementById('seccoes');
  nav.innerHTML = dados.seccoes
    .map((s) => `<a href="#${s.id}">${escapar(s.nome)}</a>`)
    .join('');
  for (const seccao of dados.seccoes) {
    const bloco = document.createElement('section');
    bloco.className = 'seccao';
    bloco.id = seccao.id;
    const n = seccao.produtos.length;
    bloco.innerHTML = `<h2>${escapar(seccao.nome)}<small>${n} ${n === 1 ? 'referência' : 'referências'}</small></h2><div class="grelha"></div>`;
    const grelha = bloco.querySelector('.grelha');
    seccao.produtos.forEach((produto, i) => grelha.appendChild(cartao(produto, seccao, i)));
    const rodape = legenda(seccao);
    if (rodape) bloco.appendChild(rodape);
    montra.appendChild(bloco);
  }
  // A ancora activa acompanha o scroll: a seccao que ocupa o topo do ecra.
  const ligacoes = [...nav.querySelectorAll('a')];
  const observador = new IntersectionObserver((entradas) => {
    for (const e of entradas) {
      if (!e.isIntersecting) continue;
      ligacoes.forEach((a) => a.classList.toggle('activa', a.getAttribute('href') === '#' + e.target.id));
    }
  }, { rootMargin: '-45% 0px -50% 0px' });
  document.querySelectorAll('.seccao').forEach((s) => observador.observe(s));
}

// Os tres cultivos que a Caramelo trabalha, na ordem da legenda. Um cultivo fora
// desta lista (ou vazio) nao leva ponto: melhor sem marca do que com marca errada.
const CULTIVOS = [
  { chave: 'indoor', nome: 'Indoor' },
  { chave: 'greenhouse', nome: 'Greenhouse' },
  { chave: 'glasshouse', nome: 'Glasshouse' },
];
const cultivoDe = (produto) =>
  CULTIVOS.find((c) => c.nome.toLowerCase() === String(produto.cultivo || '').trim().toLowerCase()) || null;

const ponto = (c) => `<i class="cultivo cultivo-${c.chave}" title="${c.nome}"></i>`;

function legenda(seccao) {
  const presentes = CULTIVOS.filter((c) => seccao.produtos.some((p) => cultivoDe(p) === c));
  if (!presentes.length) return null;
  const p = document.createElement('p');
  p.className = 'legenda';
  p.innerHTML = presentes.map((c) => `<span>${ponto(c)}${c.nome}</span>`).join('');
  return p;
}

/** Peso da indica de 0 a 100, ou null quando nao esta documentado. */
const pesoIndica = (produto) => {
  const peso = produto.indica;
  return typeof peso === 'number' && peso >= 0 && peso <= 100 ? peso : null;
};

function cartao(produto, seccao, posicao) {
  const botao = document.createElement('button');
  botao.className = 'cartao';
  botao.type = 'button';

  const temVideo = Boolean(produto.video);
  // As primeiras seis fotografias estao no ecra ao abrir: pedem-se ja, as outras
  // so quando se aproximam.
  const carga = posicao < 6 ? 'loading="eager" fetchpriority="high"' : 'loading="lazy"';
  const imagem = produto.slug
    ? `<img src="${cartaz(produto.slug)}" alt="" ${carga} decoding="async">`
    : '<div class="sem-foto"></div>';
  const cultivo = cultivoDe(produto);
  const peso = pesoIndica(produto);

  botao.innerHTML = `
    ${imagem}
    ${cultivo ? ponto(cultivo) : ''}
    <span class="nome">
      <strong>${escapar(produto.nome)}</strong>
      ${temVideo ? '' : '<span class="breve">Em breve</span>'}
    </span>
    ${peso === null ? '' : `<span class="mini-perfil" title="Índica ${peso} · Sativa ${100 - peso}"><i style="flex:${peso}"></i><b style="flex:${100 - peso}"></b></span>`}`;

  botao.addEventListener('click', () => abrir(produto, seccao));
  return botao;
}

// ---------------------------------------------------------------------------
// A ficha: uma pagina por referencia da seccao, lado a lado numa pista com snap.
// Desliza-se com o dedo como nas fotografias do telemovel, e a fita de miniaturas
// em cima mostra onde se esta e leva a qualquer uma com um toque. O "voltar" do
// iPhone fecha a ficha em vez de sair do site, e cada flor tem endereco proprio
// (#bubba-kush) para se poder enviar a um cliente.

const miniaturas = ficha.querySelector('.miniaturas');
const pista = ficha.querySelector('.pista');

let actual = null; // { seccao, indice }
let empilhado = false; // a ficha aberta pos uma entrada no historico
let observadorDePaginas = null;

function abrir(produto, seccao, { historia = true } = {}) {
  const indice = seccao.produtos.indexOf(produto);
  const mesmaSeccao = actual && actual.seccao === seccao && ficha.open;
  actual = { seccao, indice };

  if (!mesmaSeccao) montarSeccao(seccao);
  if (!ficha.open) {
    ficha.showModal();
    document.documentElement.classList.add('aberto');
  }
  // Ir direito a pagina, sem animacao: e' uma abertura, nao uma passagem.
  pista.scrollTo({ left: indice * pista.clientWidth, behavior: 'instant' });
  tornarActual(indice);

  if (historia) {
    const estado = { ficha: produto.slug || produto.nome };
    if (empilhado) history.replaceState(estado, '', '#' + estado.ficha);
    else { history.pushState(estado, '', '#' + estado.ficha); empilhado = true; }
  }
}

/** Constroi as miniaturas e as paginas de uma seccao inteira (uma vez por abertura). */
function montarSeccao(seccao) {
  observadorDePaginas?.disconnect();
  miniaturas.innerHTML = seccao.produtos.map((p, i) => `
    <button type="button" role="tab" data-indice="${i}" aria-label="${escapar(p.nome)}" title="${escapar(p.nome)}">
      ${p.slug ? `<img src="${cartaz(p.slug)}" alt="" loading="lazy" decoding="async">` : '<span class="sem-foto"></span>'}
    </button>`).join('');
  miniaturas.querySelectorAll('button').forEach((b) =>
    b.addEventListener('click', () => irPara(Number(b.dataset.indice), true)));

  pista.innerHTML = seccao.produtos.map((p, i) => pagina(p, seccao, i)).join('');
  pista.querySelectorAll('.pagina-nav button').forEach((b) =>
    b.addEventListener('click', () => irPara(Number(b.dataset.indice), true)));

  // A pagina que ocupa o ecra e' a actual: quem desliza com o dedo tambem muda
  // de referencia, sem haver que adivinhar gestos.
  observadorDePaginas = new IntersectionObserver((entradas) => {
    for (const e of entradas) {
      if (e.isIntersecting && e.intersectionRatio >= 0.6) {
        const i = Number(e.target.dataset.indice);
        if (actual && i !== actual.indice) { actual.indice = i; tornarActual(i); anotarEndereco(); }
      }
    }
  }, { root: pista, threshold: [0.6] });
  pista.querySelectorAll('.pagina').forEach((p) => observadorDePaginas.observe(p));
}

function pagina(produto, seccao, indice) {
  const temVideo = Boolean(produto.video);
  const resumo = [produto.formato, produto.cultivo, produto.infundido ? 'Infundido' : null]
    .filter(Boolean)
    .join(' · ');
  const foto = produto.slug || produto.foto;
  const cultivo = cultivoDe(produto);
  const lista = seccao.produtos;
  const anterior = lista[(indice - 1 + lista.length) % lista.length];
  const seguinte = lista[(indice + 1) % lista.length];

  // O video so se cria quando a pagina esta perto de ser vista (ver garantirVideo):
  // doze videos a carregar de uma vez e' o que nao se quer em dados moveis.
  const media = foto
    ? `<div class="media" ${temVideo ? `data-video="${video(produto.slug)}"` : ''}><img class="cartaz" src="${cartaz(foto)}" alt="" decoding="async"></div>`
    : '<div class="media"><div class="cartaz"></div></div>';

  return `
    <article class="pagina" data-indice="${indice}" data-slug="${escapar(produto.slug || '')}">
      ${media}
      <div class="ficha-texto">
        <p class="ficha-linha">${escapar(seccao.nome)}${cultivo ? `<span class="ficha-cultivo">${ponto(cultivo)}${escapar(cultivo.nome)}</span>` : ''}</p>
        <h3>${escapar(produto.nome)}</h3>
        ${resumo ? `<p class="resumo">${escapar(resumo)}</p>` : ''}
        ${perfil(produto)}
        ${terpenos(produto)}
        <dl class="dados">
          ${produto.aroma ? dado('≋', 'Aroma', produto.aroma) : ''}
          ${produto.genetica ? dado('✦', 'Genética', produto.genetica) : ''}
        </dl>
        ${temVideo ? '' : '<p class="aviso">Em breve · sem vídeo ainda</p>'}
      </div>
      <nav class="pagina-nav" aria-label="Outras referências">
        <button type="button" class="anterior" data-indice="${(indice - 1 + lista.length) % lista.length}"><small>Anterior</small>${escapar(anterior.nome)}</button>
        <button type="button" class="seguinte" data-indice="${(indice + 1) % lista.length}"><small>Seguinte</small>${escapar(seguinte.nome)}</button>
      </nav>
    </article>`;
}

/** Marca a pagina [i] como a que esta no ecra: miniatura acesa, video a andar, vizinhos prontos. */
function tornarActual(i) {
  const paginas = pista.querySelectorAll('.pagina');
  paginas.forEach((p, j) => {
    const perto = Math.abs(j - i) <= 1;
    if (perto) garantirVideo(p);
    const v = p.querySelector('video');
    if (!v) return;
    if (j === i) v.play().catch(() => {});
    else v.pause();
  });
  miniaturas.querySelectorAll('button').forEach((b, j) => {
    b.classList.toggle('activa', j === i);
    b.setAttribute('aria-selected', j === i ? 'true' : 'false');
    if (j === i) b.scrollIntoView({ inline: 'center', block: 'nearest', behavior: 'smooth' });
  });
  paginas[i]?.scrollTo({ top: 0, behavior: 'instant' });
}

/** Troca o cartaz pelo video, com o cartaz como poster para nao haver salto. */
function garantirVideo(paginaEl) {
  const media = paginaEl.querySelector('.media');
  if (!media || !media.dataset.video || media.querySelector('video')) return;
  const v = document.createElement('video');
  v.src = media.dataset.video;
  v.poster = media.querySelector('img')?.src || '';
  // playsinline e obrigatorio no Safari do iPhone: sem ele o video salta para
  // ecra inteiro e sai da pagina. Sem som e em ciclo: a flor a rodar e' a montra.
  v.setAttribute('playsinline', '');
  v.muted = true;
  v.loop = true;
  v.controls = true;
  v.preload = 'metadata';
  media.replaceChildren(v);
}

function irPara(i, animado) {
  if (!actual) return;
  const lista = actual.seccao.produtos;
  i = (i + lista.length) % lista.length;
  pista.scrollTo({ left: i * pista.clientWidth, behavior: animado ? 'smooth' : 'instant' });
  // O observador confirma quando a pagina chegar; entretanto ja se acende a miniatura.
  actual.indice = i;
  tornarActual(i);
  anotarEndereco();
}

function anotarEndereco() {
  if (!actual) return;
  const p = actual.seccao.produtos[actual.indice];
  const estado = { ficha: p.slug || p.nome };
  history.replaceState(estado, '', '#' + estado.ficha);
}

function fechar({ historia = true } = {}) {
  if (!ficha.open) return;
  // Parar os videos antes de fechar, ou continuam a tocar em segundo plano.
  pista.querySelectorAll('video').forEach((v) => v.pause());
  ficha.close();
  document.documentElement.classList.remove('aberto');
  actual = null;
  // Fechar pela interface tira do historico a entrada que a abertura pos, para o
  // proximo "voltar" nao reabrir a ficha.
  if (historia && empilhado) { empilhado = false; history.back(); }
  else empilhado = false;
}

// Voltar (gesto ou botao) fecha a ficha; avancar ou um endereco com #flor abre-a.
window.addEventListener('popstate', () => {
  if (ficha.open) { empilhado = false; fechar({ historia: false }); }
  else abrirPeloEndereco(false);
});

function abrirPeloEndereco(arranque) {
  const slug = decodeURIComponent(location.hash.slice(1));
  if (!slug) return;
  for (const seccao of seccoes) {
    const produto = seccao.produtos.find((p) => p.slug === slug);
    if (!produto) continue;
    // No arranque a entrada ja existe no historico (e' o proprio endereco); nao se
    // empilha outra, mas marca-se para o "voltar" a apagar.
    empilhado = true;
    abrir(produto, seccao, { historia: false });
    if (arranque) history.replaceState({ ficha: slug }, '', '#' + slug);
    return;
  }
}

ficha.querySelector('.fechar').addEventListener('click', () => fechar());
ficha.addEventListener('cancel', (e) => { e.preventDefault(); fechar(); });
ficha.addEventListener('close', () => { pista.innerHTML = ''; miniaturas.innerHTML = ''; observadorDePaginas?.disconnect(); });

// Ao rodar o telemovel a largura muda: volta a alinhar a pagina actual.
window.addEventListener('resize', () => { if (actual && ficha.open) pista.scrollTo({ left: actual.indice * pista.clientWidth, behavior: 'instant' }); });

// Deslizar para baixo com a pagina no topo fecha a ficha. Limiar alto de
// proposito: um scroll normal nunca fecha nada.
let toque = null;
pista.addEventListener('touchstart', (e) => {
  const t = e.changedTouches[0];
  const paginaEl = e.target.closest('.pagina');
  toque = { x: t.clientX, y: t.clientY, topo: !paginaEl || paginaEl.scrollTop === 0 };
}, { passive: true });
pista.addEventListener('touchend', (e) => {
  if (!toque) return;
  const t = e.changedTouches[0];
  const dx = t.clientX - toque.x, dy = t.clientY - toque.y;
  const topo = toque.topo;
  toque = null;
  if (dy > 140 && Math.abs(dx) < 40 && topo) fechar();
}, { passive: true });

// Setas no teclado (iPad com teclado, ou o portatil): a mesma navegacao.
ficha.addEventListener('keydown', (e) => {
  if (!actual) return;
  if (e.key === 'ArrowRight') irPara(actual.indice + 1, true);
  else if (e.key === 'ArrowLeft') irPara(actual.indice - 1, true);
});

const dado = (icone, titulo, valor) => `
  <div class="dado">
    <span class="icone" aria-hidden="true">${icone}</span>
    <div><dt>${escapar(titulo)}</dt><dd>${escapar(valor)}</dd></div>
  </div>`;

/**
 * Peso da indica contra o da sativa: as duas pontas com o numero, a barra por
 * baixo. Sem peso documentado nao ha barra: uma a meio para toda a gente seria
 * decoracao, nao dado.
 */
function perfil(produto) {
  const peso = pesoIndica(produto);
  if (peso === null) return '';
  const sativa = 100 - peso;
  return `
    <section class="perfil" aria-label="Perfil índica e sativa">
      <p class="perfil-topo"><span class="rotulo">Perfil</span><span class="tipo">${escapar(produto.tipo || '')}</span></p>
      <div class="perfil-pontas"><span>Índica <strong>${peso}</strong></span><span><strong>${sativa}</strong> Sativa</span></div>
      <div class="perfil-barra" role="img" aria-label="Índica ${peso} por cento, sativa ${sativa} por cento">
        <i style="flex:${peso}"></i><b style="flex:${sativa}"></b>
      </div>
    </section>`;
}

/** Os terpenos dominantes em etiquetas, com a origem dita ao lado. */
function terpenos(produto) {
  const lista = Array.isArray(produto.terpenos) ? produto.terpenos.filter(Boolean) : [];
  if (!lista.length) return '';
  return `
    <section class="terpenos" aria-label="Terpenos dominantes">
      <p class="rotulo">Terpenos dominantes</p>
      <p class="etiquetas">${lista.map((t) => `<span>${escapar(t)}</span>`).join('')}</p>
      <p class="nota">Segundo o produtor ou a genética. O boletim de análise do lote manda.</p>
    </section>`;
}

function escapar(texto) {
  const d = document.createElement('div');
  d.textContent = texto ?? '';
  return d.innerHTML;
}
