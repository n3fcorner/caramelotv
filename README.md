# Caramelo do Brasil — TV

Esqueleto de uma app **Android TV** (Google TV / TCL) com um menu navegável apenas
com o comando: **9 componentes principais** à esquerda numa grelha 3×3, **6 secundários**
à direita numa grelha 3×2 e um **painel de controlos e definições** a ocupar o espaço
que sobra em baixo à direita.

Cada um dos 15 slots abre um ecrã de **detalhes** e reproduz um **vídeo** (MP4 ou HLS),
e é **editável dentro da própria app**.

```
+--------------------------------+-------------------+
|  9 principais (3x3)            |  6 secundarios    |
|                                |  (3x2)            |
|                                +-------------------+
|                                |  painel           |
+--------------------------------+-------------------+
```

> **Estado:** esqueleto. A arquitetura está completa e a app compila, instala e funciona
> ponta a ponta, mas o conteúdo são placeholders e cada ecrã está no mínimo necessário —
> a ideia é ser fácil de estender, não estar acabado.

## Identidade

A interface segue a marca **Caramelo do Brasil** (caramelodobrasil.com). A paleta foi
tirada a pixel dos ficheiros da marca em `franflabs-site/public/caramelo theme/`:

| | |
|---|---|
| Roxo da marca | `#573B89` — superfícies |
| Roxo fundo / profundo | `#2E1F4C` · `#1E1433` — fundo do ecrã e faixas |
| Amarelo | `#FFBE00` — foco, acentos |
| Laranja | `#FF9100` · `#FE7F02` |
| Creme | `#F5EED1` — texto |
| Marinho | `#002448` — texto sobre amarelo |
| Verde | `#0E8F35` — folha do logo |

O roxo do banner é forte de mais para preencher um ecrã de TV inteiro numa sala escura,
por isso serve de **superfície** e não de fundo: o fundo usa dois tons mais fundos da
mesma família. A marca geométrica do logo foi recortada dos ficheiros originais e
gravada com fundo transparente em `res/drawable-nodpi/logo_caramelo.png`; o banner do
launcher e o ícone são gerados a partir dela.

### O canto inferior direito

Já não é um painel de definições. Aquilo estava sempre à vista sem ninguém lhe mexer,
e o espaço rende mais a mostrar a **ficha do produto em foco** — linha, formato,
cultivo, aroma. Não é focável de propósito: lê-se, não se navega.

As definições passaram para o botão do cabeçalho e para a **tecla MENU** do comando.

### Regras de estilo

- **Sem cantos redondos.** Tudo usa `RectangleShape`.
- **Sem degradês pretos sobre as fotos.** O texto assenta numa faixa sólida da marca.
- **Os mosaicos expandem em foco** (escala 1.09 com mola), passando por cima dos
  vizinhos via `zIndex`, e a faixa abre para revelar a linha secundária.
- **A foto voa entre ecrãs.** `SharedTransitionLayout` liga o mosaico ao cartaz dos
  detalhes, por isso abrir um item não é um corte seco.
- **Os mosaicos preenchem.** Como todo o catálogo é vertical, enquadrar cada 9:16
  dentro de um mosaico deitado deixava mais fundo do que produto. O mosaico recorta
  e preenche; o formato é respeitado onde importa, no cartaz e no leitor.

## Media

Os onze vídeos da marca vivem em `videos/`, em duas pastas por linha de produto. São
**todos verticais 1080×1920** (um em 4K), o que decidiu boa parte do desenho.

```bash
./scripts/preparar-media.sh    # converte + tira cartazes -> videos/web/
./scripts/servir.sh            # serve videos/web na rede local, com Range
```

`preparar-media.sh` faz três coisas: converte para 1080×1920 H.264 CRF 23 com
`+faststart` (o índice fica no início do ficheiro, sem o qual a reprodução progressiva
só arranca depois de descarregar tudo), tira um cartaz a 35% de cada clipe, e normaliza
os nomes. Os originais ficam intactos.

| | antes | depois |
|---|---|---|
| Tamanho | 828 MB | 337 MB |

`servir.sh` levanta um servidor estático **com suporte a `Range`** — o
`python3 -m http.server` não o tem, e sem ele o leitor não consegue avançar no vídeo
sem voltar a descarregar do início.

Os itens guardam **caminhos relativos** (`media/grape-cali.mp4`). O prefixo vem de
`PanelSettings.mediaBaseUrl`, por isso mudar de alojamento é uma edição só:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "caramelotv://base?url=http://192.168.1.216:8000"
```

O manifesto permite **HTTP em claro** (`network_security_config.xml`), sem o que o
Android bloqueia o servidor local em silêncio. Quando o alojamento passar a HTTPS,
essa excepção pode desaparecer.

## Catálogo

Três secções, uma por linha comercial, mais o hash à parte. Não é decoração: 5×2 daria
mosaicos de ~96 dp, pequenos de mais para ler a três metros, e o hash no meio de oito
flores passaria despercebido — são produtos artesanais, visualmente distintos.

| Secção | Grelha | Produtos |
|---|---|---|
| **Infusão** | 3×3 | 5 com vídeo · MAC, Gorilla Mandarini e Berry Pie em breve · 1 livre |
| **CBD** | 3×2 | 4 com vídeo · Critical Berry e Uranium em breve |
| **Hash** | 3×1 | Static e Sticky Peach · 1 livre |

Há dois `Critical Berry` — um em cada linha — desambiguados pela secção na ficha. Um
produto anunciado sem vídeo aparece com a etiqueta **Em breve**; um slot mesmo vazio só
se vê em Definições.

Cada item tem uma ficha (`Strain`) com linha, formato, cultivo e aroma. As descrições
falam de **produto** — formato, cultivo, aroma — e não de efeitos: isso é matéria
regulada e tem de vir da marca. Reveja-as antes de irem para uma montra.

## Vídeo quadrado

Os clipes da marca vinham 1080×1920 com duas marcas de água de terceiros: o logo do
canal no topo e a faixa com o nome em baixo. Um corte **quadrado** resolve as duas de
uma vez, e ainda deixa um formato que num ecrã 16:9 liberta a coluna onde vive a ficha.

O estudo de enquadramento está no [preparar-media.sh](scripts/preparar-media.sh): a
janela útil vai de y≈350 a y≈1520, por isso um quadrado de 1080 só cabe com o corte
entre 350 e 440 — a 560 já entra a faixa do nome. Daí `crop=1080:1080:0:420`, igual
para todos os onze.

```bash
./scripts/preparar-media.sh     # 828 MB -> 114 MB, e as 11 miniaturas
```

Além do corte: 30 fps (metade vinha a 60), `hqdn3d` para o ruído de sensor do macro à
mão levantada, 900×900 e CRF 27. O 900 é deliberado — no leitor o painel do vídeo tem
cerca de 900px de altura num ecrã 1080p, e acima disso pagavam-se bits por pixéis que
ninguém vê. Comparado a 1:1 com 1080 antes de decidir.

As miniaturas saem do frame mais representativo de cada clipe, com o instante escolhido
à vista (uns começam em preto, noutros o produto só aparece a meio), o mesmo corte
quadrado e um tratamento leve de contraste, saturação e nitidez.

## Stack

Kotlin 2.0 · Jetpack Compose · `androidx.tv:tv-material` (Compose for TV) ·
Media3/ExoPlayer · DataStore + kotlinx.serialization · Coil · AGP 8.7 / Gradle 8.11.

## Setup (uma vez)

Não é preciso Android Studio, mas se o usares ele já traz JDK e SDK.

```bash
brew install openjdk@17 gradle
brew install --cask android-commandlinetools
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

O `local.properties` (ignorado pelo git) aponta para o SDK:

```
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

Como o `openjdk@17` do Homebrew é *keg-only*, não há `java` no PATH. Exporta o `JAVA_HOME`
antes de usar o Gradle na linha de comandos:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

## Compilar e instalar

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
```

**Emulador de TV** (as setas do teclado fazem de D-pad — a forma mais rápida de testar foco):

```bash
sdkmanager "system-images;android-34;android-tv;arm64-v8a"
avdmanager create avd -n tv -k "system-images;android-34;android-tv;arm64-v8a"
emulator -avd tv &
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**TCL real.** Primeiro ligar a depuração na TV, senão a porta 5555 fica fechada e o
`adb connect` devolve *Connection refused*:

1. `Definições → Sistema → Acerca de` → 7 cliques em **Compilação**;
2. `Definições → Sistema → Opções de programador` → ativar **Depuração ADB** e
   **Depuração por rede** (nalgumas versões chama-se *Wireless debugging*);
3. o IP está em `Definições → Rede e Internet` → a tua ligação.

Depois basta:

```bash
./scripts/install-tv.sh 192.168.1.100
```

Da primeira vez a TV mostra um aviso a pedir autorização — aceita-o. Se em vez da
depuração por rede a TV oferecer *Depuração sem fios* com um código, emparelha antes
com `adb pair <IP>:<porta mostrada na TV>`.

Com o emulador e a TV ligados ao mesmo tempo, o `adb` precisa sempre de `-s`:
`adb -s 192.168.1.100:5555 install -r ...`.

A app aparece na linha de aplicações da Google TV (o manifesto declara
`LEANBACK_LAUNCHER` e um `banner`, sem os quais o launcher de TV a esconde).

## Estrutura

```
app/src/main/java/pt/caramelo/tv/
├── MainActivity.kt          Activity única + raiz de navegação (SharedTransitionLayout)
├── MainViewModel.kt         Pilha de ecrãs, foco atual, ações, deep links
├── CarameloApp.kt           Application; expõe o repositório
├── nav/Screen.kt            Home · Details · Player · Settings · EditItem
├── data/
│   ├── model/               MenuItem (+Section, MediaOrientation, ItemState), Strain, …
│   ├── DefaultCatalog.kt    Semente: os 16 produtos das três secções
│   └── CatalogRepository.kt DataStore com o catálogo inteiro em JSON
└── ui/
    ├── theme/               Paleta da marca, tipografia 10-foot, overscan, molas
    ├── animation/           Âmbitos da transição partilhada e o modificador `hero`
    ├── components/          TvButton, SelectableRow, ToggleRow, formatarTempo
    ├── home/                HomeScreen (3 secções), TileGrid, ProductTile
    ├── details/             DetailsScreen
    ├── player/              PlayerScreen (ExoPlayer)
    └── settings/            SettingsScreen, EditItemScreen
```

### Decisões que vale a pena conhecer

- **Grelhas não-lazy.** `TileGrid` usa `Row`/`Column` e não `LazyVerticalGrid`. Com 9 e 6
  itens não há ganho em ser lazy, e nenhum cartão sai de composição — a pesquisa de foco 2D
  do Compose fica previsível e o D-pad nunca perde o alvo nas margens.
- **Navegação própria, sem Navigation-Compose.** São 5 ecrãs e uma pilha em
  `MainViewModel`. Evita a dança de foco entre back stacks e deixa-nos restaurar o cartão
  focado à mão quando se volta atrás.
- **Um `FocusRequester` por cartão.** É o que permite devolver o foco ao sítio certo depois
  de Detalhes ou do leitor.
- **Nada de painel de definições sempre à vista.** A informação vive no submenu; as
  definições chegam-se pelo botão discreto do cabeçalho ou pela tecla MENU.
- **A mola do foco ultrapassa 0 e 1 de propósito** — é o que dá o ressalto na escala. Só
  a escala vê o valor cru; paddings, larguras e alfas veem-no limitado, ou rebenta com
  *Padding must be non-negative* no primeiro desfoque.
- **Leitor com controlos próprios.** O controlador do Media3 saiu: era cinzento, genérico
  e não respondia bem ao D-pad. OK pausa, ←/→ saltam 10 s, ↑/↓ mudam de produto dentro
  da secção. O clipe repete por omissão — é uma montra.
- **Duas bibliotecas Material.** `tv-material3` para tudo, e `compose-material3` só para
  os `TextField` do formulário de edição — a lib de TV não tem campos de texto.

## Configurar o conteúdo

Três vias, da mais lenta à mais prática:

1. **Na TV:** painel → *Definições* → escolher um slot → editar. Ou premir OK longo num cartão.
2. **Por ADB** (recomendado para URLs longos — escrever no teclado da TV é penoso).
   `slot` é o número visível: 1–9 principais, 10–15 secundários.

   Os `&` têm de ir escapados: quem os interpreta é a shell da TV, não a tua.

   ```bash
   adb shell am start -a android.intent.action.VIEW \
     -d "caramelotv://set?slot=3\&title=Documentario\&url=https://exemplo/stream.m3u8"

   adb shell am start -a android.intent.action.VIEW -d "caramelotv://play?slot=3"
   adb shell am start -a android.intent.action.VIEW -d "caramelotv://reset"
   ```

   Parâmetros de `set`: `title`, `sub`, `desc`, `url`, `image`, `badge`. Se o valor tiver
   espaços ou `?`/`&`, codifica-o em percent-encoding.
3. **No código:** `data/DefaultCatalog.kt`, para mudar o estado inicial.

## Montra no telemóvel

A mesma montra numa página web, para abrir no telemóvel e mandar por link:

**https://caramelo-montra.vercel.app**

Vive em [web/](web/) — HTML, CSS e JavaScript simples, sem compilação. O catálogo
está em `web/catalogo.json`, **à parte do catálogo da app** (`data/DefaultCatalog.kt`):
mudar um não muda o outro, e convém rever os dois quando um produto mudar.

Os vídeos são versões leves, 640×640, geradas por
[preparar-web.sh](scripts/preparar-web.sh) — 113 MB passam a 38 MB, porque quem abre
o link está muitas vezes em dados móveis.

```bash
./scripts/preparar-web.sh          # versões para telemóvel
cp videos/web-movel/*.mp4 web/media/
cd web && vercel deploy --prod
```

O `playsinline` no `<video>` não é decorativo: sem ele o Safari do iPhone salta
para ecrã inteiro e sai da página.

## Onde continuar

- `ControlPanel.kt` — acrescentar uma entrada em `PanelAction` e tratá-la na `MainActivity`.
- `PlayerScreen.kt` — usa o controlador nativo do Media3; trocar por um overlay em Compose
  se quiseres controlos próprios.
- `TileCard.kt` — todo o visual dos cartões vive aqui, para as duas zonas.
- `MenuItem` — o modelo é propositadamente simples; acrescentar campos implica só mexer no
  `data class`, no formulário e no `TileCard`.
- Falta: testes, ícone/banner definitivos, e assinatura de release.

## Verificação

Validado num emulador Google TV (API 34, arm64), com o D-pad:

- os 15 cartões percorrem-se com as setas; da coluna 3 da esquerda → entra na grelha
  secundária na mesma linha, e ↓ desde essa grelha entra no painel;
- OK abre detalhes, *Reproduzir* arranca o vídeo (MP4 e HLS);
- Back devolve ao ecrã anterior e à Home **com o foco no mesmo cartão**;
- sair a meio de um vídeo grava a posição e *Retomar* reaparece nela;
- Definições e o formulário de edição abrem e gravam;
- o deep link `caramelotv://set` reconfigura um slot em tempo real.

Por testar na TCL real: comportamento do teclado do sistema nos campos de texto e o
desempenho da descodificação de vídeo.

Nota sobre o conteúdo de exemplo: o bucket `gtv-videos-bucket` da Google, muito citado
em tutoriais, devolve **403** desde há algum tempo. A semente usa
`media.w3.org/2010/05/sintel/trailer.mp4` (MP4) e a playlist `bipbop` da Apple (HLS).
