#!/usr/bin/env python3
"""Gera o catalogo da montra do telemovel a partir do catalogo da app.

    ./scripts/catalogo-para-web.py

O web/catalogo.json era escrito a mao ao lado do DefaultCatalog.kt, e o README
avisava para "rever os dois quando um produto mudar" — que e' outra maneira de
dizer que mais tarde ou mais cedo iam divergir. Divergiram: em Setembro de 2026 o
JSON ainda tinha onze produtos e um slot livre, quando a app ja tinha dezoito.

Agora sai do Kotlin, que e' onde estao os campos ricos (genetica, tipo, indica,
terpenos) que o site em TypeScript nao carrega. Le-se com expressoes regulares
porque as chamadas sao regulares — `produto(...)` e `emBreve(...)`, uma por
referencia — e porque a alternativa era compilar a app so para exportar JSON.
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
KOTLIN = RAIZ / "app/src/main/java/pt/caramelo/tv/data/DefaultCatalog.kt"
DESTINO = RAIZ / "web/catalogo.json"

SECCOES = [("INFUSAO", "infusao", "Infusão"), ("CBD", "cbd", "CBD"), ("HASH", "hash", "Hash")]


def texto(bloco: str, chave: str, omissao=""):
    m = re.search(rf'{chave}\s*=\s*"([^"]*)"', bloco)
    return m.group(1) if m else omissao


def chamadas(fonte: str, nome: str):
    """Cada chamada ao construtor, com os parenteses equilibrados."""
    for m in re.finditer(rf"\b{nome}\(", fonte):
        i, nivel = m.end(), 1
        while i < len(fonte) and nivel:
            nivel += (fonte[i] == "(") - (fonte[i] == ")")
            i += 1
        yield fonte[m.end():i - 1]


def posicionais(bloco: str):
    """Os argumentos sem nome, pela ordem em que aparecem.

    Tem de conhecer aspas: ha aromas com virgula la dentro ("Terroso e cafe,
    fundo doce"), e um separador ingenuo parte-os a meio.
    """
    fora, nivel, dentro, actual = [], 0, False, ""
    for c in bloco:
        if c == '"':
            dentro = not dentro
        elif not dentro:
            if c in "([": nivel += 1
            elif c in ")]": nivel -= 1
            elif c == "," and nivel == 0:
                fora.append(actual.strip()); actual = ""
                continue
        actual += c
    fora.append(actual.strip())
    return [a for a in fora if a and "=" not in a.split('"')[0]]


def produto(bloco: str, com_video: bool):
    pos = posicionais(bloco)
    literais = [re.fullmatch(r'"(.*)"', a).group(1) for a in pos if re.fullmatch(r'"(.*)"', a)]
    seccao = re.search(r"Section\.(\w+)", pos[0]).group(1)
    slot = int(pos[1])
    # A linha (LINHA_INFUSAO / LINHA_CBD) e' um identificador, nao um literal,
    # por isso nao entra nesta contagem — sai do proprio nome mais abaixo.
    if com_video:
        titulo, slug, formato, cultivo, aroma = literais[:5]
    else:
        # emBreve(section, slot, titulo, linha, aroma, slug = "...")
        titulo, aroma = literais[0], literais[1]
        slug = texto(bloco, "slug") or (literais[2] if len(literais) > 2 else "")
        formato, cultivo = "Flor", ""
    terp = re.search(r"terpenos\s*=\s*listOf\(([^)]*)\)", bloco)
    indica = re.search(r"indica\s*=\s*(-?\d+)", bloco)
    return seccao, {
        "slug": slug,
        "nome": titulo,
        "formato": formato,
        "cultivo": cultivo,
        "infundido": "infundido = true" in bloco or "LINHA_INFUSAO" in bloco,
        "aroma": aroma,
        "genetica": texto(bloco, "genetica"),
        "tipo": texto(bloco, "tipo"),
        "indica": int(indica.group(1)) if indica else -1,
        "terpenos": re.findall(r'"([^"]+)"', terp.group(1)) if terp else [],
        "video": bool(com_video),
        "slot": slot,
    }


def main():
    fonte = KOTLIN.read_text()
    sem_video = set(re.findall(r'"([^"]+)"', re.search(
        r"SEM_VIDEO\s*=\s*(?:setOf|emptySet<String>)\(([^)]*)\)", fonte).group(1)))

    por_seccao = {s: [] for s, _, _ in SECCOES}
    for bloco in chamadas(fonte, "produto"):
        if "section: Section" in bloco:      # a declaracao da funcao, nao uma chamada
            continue
        s, p = produto(bloco, com_video=True)
        p["video"] = p["slug"] not in sem_video
        por_seccao[s].append(p)
    for bloco in chamadas(fonte, "emBreve"):
        if "section: Section" in bloco:
            continue
        s, p = produto(bloco, com_video=False)
        por_seccao[s].append(p)

    saida = {"marca": "Caramelo do Brasil", "seccoes": []}
    total = com = 0
    for chave, id_, nome in SECCOES:
        itens = sorted(por_seccao[chave], key=lambda p: p["slot"])
        for p in itens:
            p.pop("slot")
        saida["seccoes"].append({"id": id_, "nome": nome, "produtos": itens})
        total += len(itens)
        com += sum(1 for p in itens if p["video"])

    DESTINO.write_text(json.dumps(saida, ensure_ascii=False, indent=2) + "\n")
    print(f"  {total} referências, {com} com vídeo -> {DESTINO.relative_to(RAIZ)}")
    for chave, id_, nome in SECCOES:
        itens = [p["nome"] for p in saida["seccoes"][[s[1] for s in SECCOES].index(id_)]["produtos"]]
        print(f"  {nome:<10} {len(itens)}: {', '.join(itens)}")


if __name__ == "__main__":
    sys.exit(main())
