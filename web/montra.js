// Montra da Caramelo do Brasil para telemovel. Sem compilacao: o catalogo vem de
// catalogo.json e os videos de MEDIA, para trocar de alojamento ser uma edicao so.

const MEDIA = window.CARAMELO_MEDIA || 'media';

const montra = document.getElementById('montra');
const ficha = document.getElementById('ficha');
const fichaConteudo = ficha.querySelector('.ficha-conteudo');

const cartaz = (slug) => `${MEDIA}/posters/${slug}.jpg`;
const video = (slug) => `${MEDIA}/${slug}.mp4`;

fetch('catalogo.json')
  .then((r) => r.json())
  .then(desenhar)
  .catch(() => {
    montra.innerHTML = '<p style="padding:18px">Não foi possível carregar o catálogo.</p>';
  });

function desenhar(dados) {
  montra.innerHTML = '';
  for (const seccao of dados.seccoes) {
    const bloco = document.createElement('section');
    bloco.className = 'seccao';
    bloco.innerHTML = `<h2>${seccao.nome}</h2><div class="grelha"></div>`;
    const grelha = bloco.querySelector('.grelha');
    for (const produto of seccao.produtos) {
      grelha.appendChild(cartao(produto, seccao));
    }
    montra.appendChild(bloco);
  }
}

function cartao(produto, seccao) {
  const botao = document.createElement('button');
  botao.className = 'cartao';
  botao.type = 'button';

  const temVideo = Boolean(produto.slug);
  const imagem = temVideo
    ? `<img src="${cartaz(produto.slug)}" alt="" loading="lazy" decoding="async">`
    : '<div class="sem-foto"></div>';

  botao.innerHTML = `
    ${imagem}
    <span class="nome">
      <strong>${escapar(produto.nome)}</strong>
      ${temVideo ? '' : '<span class="breve">Em breve</span>'}
    </span>`;

  botao.addEventListener('click', () => abrir(produto, seccao));
  return botao;
}

function abrir(produto, seccao) {
  const temVideo = Boolean(produto.slug);
  const resumo = [produto.formato, produto.cultivo, produto.infundido ? 'Infundido' : null]
    .filter(Boolean)
    .join(' · ');

  // playsinline e obrigatorio no Safari do iPhone: sem ele o video salta para
  // ecra inteiro e sai da pagina.
  const media = temVideo
    ? `<video src="${video(produto.slug)}" poster="${cartaz(produto.slug)}"
              controls playsinline preload="metadata"></video>`
    : '<div class="cartaz"></div>';

  fichaConteudo.innerHTML = `
    ${media}
    <div class="ficha-texto">
      <p class="ficha-linha">${escapar(seccao.nome)}</p>
      <h3>${escapar(produto.nome)}</h3>
      ${resumo ? `<p class="resumo">${escapar(resumo)}</p>` : ''}
      <p class="descricao">${escapar(produto.descricao)}</p>
      <dl class="dados">
        ${produto.cultivo ? dado('❁', 'Cultivo', produto.cultivo) : ''}
        ${produto.aroma ? dado('≋', 'Aroma', produto.aroma) : ''}
      </dl>
      ${temVideo ? '' : '<p class="aviso">Em breve na linha</p>'}
    </div>`;

  ficha.showModal();
}

const dado = (icone, titulo, valor) => `
  <div class="dado">
    <span class="icone" aria-hidden="true">${icone}</span>
    <div><dt>${escapar(titulo)}</dt><dd>${escapar(valor)}</dd></div>
  </div>`;

function fechar() {
  // Parar o video antes de fechar, ou continua a tocar em segundo plano.
  fichaConteudo.querySelector('video')?.pause();
  ficha.close();
}

ficha.querySelector('.fechar').addEventListener('click', fechar);
ficha.addEventListener('close', () => { fichaConteudo.innerHTML = ''; });
// Toque fora da ficha fecha-a.
ficha.addEventListener('click', (e) => { if (e.target === ficha) fechar(); });

function escapar(texto) {
  const d = document.createElement('div');
  d.textContent = texto ?? '';
  return d.innerHTML;
}
