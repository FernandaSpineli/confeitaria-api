# Exemplo passo a passo: bolo de chocolate 20 cm

Os números abaixo são exatamente os verificados por `CalculadoraPrecoTest` (domínio puro) e por
`FluxoBoloIT` (de ponta a ponta pela API, com PostgreSQL real).

## 1. Dados da confeiteira

| Configuração | Valor |
|---|---|
| Quer ganhar por mês | R$ 4.000,00 |
| Horas trabalhadas por mês | 160 h → **valor da hora = 4.000 / 160 = R$ 25,00** |
| Tarifa do kWh (vigente desde 01/01/2020) | R$ 0,80 |
| Bandeira vigente | AMARELA, adicional R$ 0,01885/kWh → **R$ 0,81885/kWh** |
| Gás | botijão R$ 120,00, dura 40 h de forno → **R$ 3,00/h** |
| Custos fixos | aluguel 500 + internet 100 + MEI 80 + água 70 = **R$ 750,00/mês** |
| Rateio dos fixos | por HORAS → 750 / 160 = **R$ 4,6875 por hora de produção** |
| Canais | Balcão (Pix) 0% · Maquininha 4% · iFood 27% |

## 2. Insumos: do preço da embalagem ao custo por unidade base

| Insumo | Embalagem comprada | Custo por unidade base |
|---|---|---|
| Farinha de trigo | 1 kg por R$ 6,00 | 6,00 / 1000 g = R$ 0,006/g |
| Açúcar | 1 kg por R$ 5,00 | R$ 0,005/g |
| Chocolate em pó 50% | 200 g por R$ 9,00 | R$ 0,045/g |
| Ovos | 30 un por R$ 24,00 | R$ 0,80/un |
| Leite | 1 L por R$ 5,00 | 5,00 / 1000 ml = R$ 0,005/ml |
| Óleo | 900 ml por R$ 9,00 | R$ 0,01/ml |
| Fermento | 100 g por R$ 4,00 | R$ 0,04/g |
| Creme de leite | 200 g por R$ 4,00 | R$ 0,02/g |
| Chocolate meio amargo | 1 kg por R$ 50,00 | R$ 0,05/g |

## 3. A receita (um lote = 1 bolo = 10 fatias)

Rendimento 10 · tempo de preparo 150 min (2,5 h) · forno a gás 45 min · margem 30%.

### Insumos (por lote)

| Insumo | Quantidade | × custo | = |
|---|---|---|---|
| Farinha | 300 g | 0,006 | 1,80 |
| Açúcar | 250 g | 0,005 | 1,25 |
| Chocolate em pó | 80 g | 0,045 | 3,60 |
| Ovos | 4 un | 0,80 | 3,20 |
| Leite | 240 ml (enviado como 0,24 L) | 0,005 | 1,20 |
| Óleo | 120 ml | 0,01 | 1,20 |
| Fermento | 15 g | 0,04 | 0,60 |
| Creme de leite | 400 g | 0,02 | 8,00 |
| Chocolate meio amargo | 300 g | 0,05 | 15,00 |
| **Total** | | | **35,85** |

### Embalagens: `POR_LOTE` × `POR_UNIDADE`

| Embalagem | Base | Qtd. informada | Qtd. no lote | × custo | = |
|---|---|---|---|---|---|
| Base de papelão 20 cm | POR_LOTE | 1 | 1 | 1,50 | 1,50 |
| Embalagem de fatia | POR_UNIDADE | 1 | 1 × 10 = 10 | 0,90 | 9,00 |
| Etiqueta | POR_UNIDADE | 1 | 1 × 10 = 10 | 0,20 | 2,00 |
| **Total** | | | | | **12,50** |

> Em um brigadeiro (rendimento 30), a forminha seria `POR_UNIDADE` com quantidade 1, ou seja,
> 30 forminhas por lote. Se o bolo fosse vendido inteiro (rendimento 1), as duas bases dariam o mesmo resultado.

### Mão de obra, energia, gás e custos fixos

| Componente | Conta | = |
|---|---|---|
| Mão de obra | 2,5 h × R$ 25,00 | 62,50 |
| Energia: batedeira 500 W por 15 min | 0,5 kW × 0,25 h = 0,125 kWh | |
| Energia: micro-ondas 1200 W por 5 min | 1,2 kW × 0,0833 h = 0,100 kWh | |
| Energia total | 0,225 kWh × R$ 0,81885 | 0,18 (0,18424) |
| Gás | 0,75 h × R$ 3,00 | 2,25 |
| Custos fixos (rateio por horas) | 2,5 h × R$ 4,6875 | 11,72 (11,71875) |

## 4. Custo total e preço

```
custo do lote   = 35,85 + 12,50 + 62,50 + 0,18424125 + 2,25 + 11,71875 = 125,00299125  → R$ 125,00
custo unitário  = 125,00299125 / 10 fatias                              = 12,500299125  → R$ 12,50
```

O cálculo intermediário não arredonda nada (escala 10, HALF_EVEN). Só o resultado vai para 2 casas.

**Fórmula da margem sobre o preço:** `preço = custo unitário / (1 − margem − taxa do canal)`

| Canal | Conta | Preço | Taxa paga | Lucro por fatia |
|---|---|---|---|---|
| Sem taxa / Balcão (Pix) | 12,500299 / (1 − 0,30) | **R$ 17,86** | 0,00 | 17,86 − 12,50 = **R$ 5,36** |
| Maquininha (4%) | 12,500299 / (1 − 0,30 − 0,04) | **R$ 18,94** | 0,76 | 18,94 − 12,50 − 0,76 = **R$ 5,68** |
| iFood (27%) | 12,500299 / (1 − 0,30 − 0,27) | **R$ 29,07** | 7,85 | 29,07 − 12,50 − 7,85 = **R$ 8,72** |

Em todos os canais o lucro é 30% do preço (5,36 / 17,86 = 30%). Com markup simples de 30%
(12,50 × 1,3 = R$ 16,25), a margem real seria só 23%. No iFood, o markup daria prejuízo depois da taxa.

**Simulação** (`POST /receitas/{id}/simulacao` com `{"margemLucro": 40}`): 12,500299 / 0,60 = **R$ 20,83**,
sem alterar a receita. Com `{"margemLucro": 80, "taxaPercentual": 20}`, a soma dá 100% e a resposta é 422 com
`canaisInvalidos`.

## 5. Estoque: compras, produção e custo médio

### Compras iniciais (`POST /estoque/compras`)

Farinha 2 kg (R$ 12,00), açúcar 1 kg, chocolate em pó 200 g, ovos 12 un (R$ 9,60), leite 1 L, óleo 900 ml,
fermento 100 g, creme de leite 1000 g (R$ 20,00), chocolate meio amargo 1 kg, base ×5, embalagem de fatia ×30,
etiqueta ×30. Todas pelo mesmo custo unitário do cadastro, então o preço continua R$ 17,86.

### Quantos lotes consigo produzir? (`GET /receitas/{id}/capacidade-producao`)

Chocolate em pó: 200 g / 80 g = 2,5 → **2 lotes** (20 fatias). Ele é o fator limitante, porque os demais
itens permitem 2 ou mais.

### Produção de 2 lotes (`POST /producoes {"lotes": 2}`)

Uma única transação gera 12 movimentações `SAIDA`:

| Item | Por lote | × 2 | Saldo antes → depois |
|---|---|---|---|
| Farinha | 300 g | 600 g | 2000 → 1400 |
| Açúcar | 250 g | 500 g | 1000 → 500 |
| Chocolate em pó | 80 g | 160 g | 200 → 40 |
| Ovos | 4 | 8 | 12 → 4 |
| Leite | 240 ml | 480 ml | 1000 → 520 |
| Óleo | 120 ml | 240 ml | 900 → 660 |
| Fermento | 15 g | 30 g | 100 → 70 |
| Creme de leite | 400 g | 800 g | 1000 → 200 |
| Chocolate meio amargo | 300 g | 600 g | 1000 → 400 |
| Base de papelão | 1 | 2 | 5 → 3 |
| Embalagem de fatia | 10 | 20 | 30 → 10 |
| Etiqueta | 10 | 20 | 30 → 10 |

`custoMateriais` = 2 × (35,85 + 12,50) = **R$ 96,70**.

### Terceiro lote: bloqueado

```json
{ "title": "Estoque insuficiente", "status": 422,
  "itensFaltantes": [
    { "item": "Chocolate em pó 50%", "necessario": 80,  "saldo": 40,  "faltam": 40,  "unidade": "g" },
    { "item": "Creme de leite",      "necessario": 400, "saldo": 200, "faltam": 200, "unidade": "g" } ] }
```

Nada é baixado: ou a produção inteira passa, ou nenhuma movimentação é gravada. Com o chocolate em pó
(40 g) abaixo do mínimo cadastrado (100 g), ele aparece em `GET /estoque/abaixo-minimo`.

### Compra mais barata: custo médio ponderado

Compra de 5 kg de farinha por R$ 27,50 (R$ 0,0055/g), com saldo de 1400 g a R$ 0,006:

```
novo custo = (1400 × 0,006 + 27,50) / (1400 + 5000) = 35,90 / 6400 = R$ 0,005609375/g
```

O preço é recalculado na hora: farinha no bolo = 300 × 0,005609375 = 1,68 (antes 1,80), e o
**preço base cai de R$ 17,86 para R$ 17,84**, sem nenhuma ação na receita.
