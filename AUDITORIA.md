# Auditoria da app de TV — 17 de Setembro de 2026

Feita com a app instalada na TV da loja (TCL, Android 12, 1,8 GB de RAM), a partir
do código e do que se mediu no aparelho. Não é opinião: cada ponto diz o que se
viu e como se viu.

## Estado

O que está feito e verificado a funcionar na TV:

- 24 referências em três colunas (Infusão 12, CBD 6, Hash 6), mosaicos todos do
  mesmo tamanho (~97 dp), 23 com vídeo, 1 anunciada (MAC).
- Media inteiro na própria TV: 206 MB de clipes, 1,3 MB de cartazes, 4,1 MB de
  boletins. Não depende de rede nem do Mac. Confirmado com o servidor desligado.
- Montra automática ao fim de 1 minuto parado, com o interruptor em Definições, e
  a não arrancar por cima do boletim, das definições e da edição.
- Boletins de análise (4 certificados IFHA/Abexan) com lupa a 1,5/2/3× e arrasto.
- Transições ligadas em todos os caminhos: mosaico → cartaz → vídeo (a mesma foto
  voa e fica no quadrado até o vídeo acender), erva → erva a deslizar sem quadrado
  preto, botões do cabeçalho a crescer até ao título do ecrã que abrem, ficha em
  cascata, arranque em esbatimento. Verificado com gravação de ecrã fotograma a
  fotograma.
- Arranque a frio: 3,4 s até ao primeiro fotograma (`am start -W`), mais 2 a 3 s
  até o menu estar composto. Memória em repouso: 124 MB.
- Compila sem um único aviso.

## O que falta

Por ordem de gravidade para a loja.

1. **Nada disto está no git.** 49 ficheiros alterados desde 1 de Setembro, ou
   seja três semanas de trabalho — pipeline, catálogo, COA, transições, fundo,
   ícone — só existem no teu disco. Um disco que morre leva tudo. É um commit.

2. **Som aleatório na montra.** 5 dos 23 clipes trazem faixa de áudio e a app
   não mexe no volume. Numa loja, a montra automática vai passar em silêncio e
   de repente um clipe fala. Correcção barata: silenciar por omissão e ter o
   interruptor em Definições.

3. **A TV é actualizada só a partir do teu Mac, por adb.** Não há caminho para
   Leo mudar uma foto, um aroma ou acrescentar uma flor sem ti. Era o ponto 5 da
   ordem de trabalhos de 5/9 ("backoffice para Leo"), e continua por fazer. O
   ecrã de edição que existe usa um `OutlinedTextField` do Material 3 — escrever
   com o comando da TV, letra a letra, não é uso real.

4. **Os textos de produto são placeholders.** Os 24 aromas foram escritos por
   mim; cultivo e infusão vêm das faixas dos vídeos e são fiáveis, o resto não.
   Está em `TEXTOS-PARA-LEO.txt` à espera de Leo. Genética, terpenos e barra
   índica/sativa em `DefaultCatalog.kt` idem (regra já registada: não contam como
   facto).

5. **O `versionCode` continua em 1** e o APK assinado que existe é de 1/9. A TV
   corre builds de debug. Para instalar noutra TV (a segunda loja, o Dehesa
   também tem uma) ou entregar a alguém, precisa de um release assinado com
   versão a subir.

6. **Fotos da Trim Rocket.** Dez artigos têm cartaz recortado do vídeo, sempre
   abaixo dos de estúdio em nitidez. As fotos existem nas pastas do Drive que
   Leo partilha (`*_TR_Photo_01.jpg`) e a tabela deles está atrás de senha. Pedir.

7. **Sticky Peach e Sky sem plano aberto.** Os clipes são macro colada; não há
   enquadramento nem recorte que os salve. Precisam de ser refilmados ou de foto.

## O que se pode melhorar

Com o esforço estimado: **B** baixo (uma sessão curta), **M** médio (um dia),
**A** alto (vários dias ou depende de terceiros).

| | Melhoria | Esforço | Porquê |
|---|---|---|---|
| 1 | Silenciar a montra por omissão + interruptor | B | ponto 2 acima |
| 2 | Commit e tag `v1.1`; subir `versionCode`; release assinado | B | pontos 1 e 5 |
| 3 | Boletim ligado a cada produto (botão "Análise" na ficha do Uranium e do Sour Lifter) | B | hoje os 4 boletins são uma lista solta; dois são de artigos da montra |
| 4 | Guardar a página do boletim onde se ficou e o nível de zoom | B | quem sai e volta recomeça do zero |
| 5 | Retirar `sem-foto.jpg` e o estado "livre" da web | B | já ninguém os usa; é código morto |
| 6 | Pré-carregar o cartaz seguinte no leitor | B | na montra automática o cartaz do próximo já podia estar em memória; hoje há um instante em que a Coil ainda o vai buscar |
| 7 | Arranque: mostrar o menu com os cartazes a chegar em vez do "A carregar…" | M | os 2–3 s depois do primeiro fotograma são a Coil a decodificar 24 cartazes de 720 px; a 97 dp chegavam 256 px — uma versão pequena dos cartazes corta o arranque e a memória |
| 8 | Versão inglesa dos textos (interruptor PT/EN) | M | Lisboa tem turistas; o site já tem EN e os textos existem em `catalog-ingles.ts` |
| 9 | Catálogo único: a app a ler o mesmo JSON que o site e o telemóvel | M | hoje são três cópias que se sincronizam à mão; a que a app lê podia ser gerada, como já é a do telemóvel |
| 10 | Actualização pela rede (a app vai buscar catálogo e media a um URL quando há) | A | é o que permite Leo mudar coisas sem adb; é metade do "backoffice" |
| 11 | Backoffice para Leo (fotos, textos, ordem, novas referências) | A | ponto 3; depende do 10 e do dossier B2B, onde já existe `/admin` |
| 12 | Uma TV como ecrã inicial (launcher) | A | hoje a app arranca no boot mas o Dehesa e as apps de streaming disputam o ecrã; só um launcher próprio ou a opção do fabricante resolvem de vez |

## Riscos que não se vêem

- **Memória.** A TV tem 65 MB livres com a app aberta. Cartazes a 720 px para
  mosaicos de 97 dp são 7× os pixéis necessários. Se o Android matar a app em
  segundo plano, o boot receiver volta a abri-la; mas não morre por acaso.
- **Suspensão da TV.** O `FLAG_KEEP_SCREEN_ON` só vale com a app à frente. O
  temporizador de suspensão da própria TV está a ~19 min; se a TV for para
  standby por si, a app não a acorda. É uma definição da TV, não da app.
- **Deep links sem chave.** `caramelotv://reset` apaga o catálogo. Só é
  acessível por adb (ou por uma app instalada na mesma TV), portanto na prática
  está protegido pelo emparelhamento adb. Fica dito.
- **A `MainActivity` é `singleTask`** e o Android reentrega o intent que criou a
  tarefa. Foi o que fez o boletim abrir sozinho; já está consumido uma vez só,
  mas é um comportamento a lembrar em qualquer deep link novo.

## Ordem sugerida

Commit hoje (1). Depois o som e o release (2). Depois os quatro pontos B que
ficam (3 a 6), que cabem numa sessão. O arranque (7) e o inglês (8) são a
sessão seguinte. O catálogo único (9) prepara a actualização pela rede (10),
que é o que desbloqueia Leo (11). O launcher (12) só se a disputa do ecrã com
as outras apps se tornar problema real na loja.
