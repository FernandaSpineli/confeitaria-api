import { get, post, put, del, todos } from '../api.js';
import {
    h, trocar, campo, entrada, selecao, lerForm, mostrarErro, limparErros, blocoProblema, ocupado, aviso, confirmar, vazio,
    brl, num, custoUnitario, pct, unidade,
} from '../dom.js';
import { ir } from '../app.js';
import { UNIDADES_POR_BASE, paginacao } from './cadastros.js';

const CAMADAS = [
    ['insumos', 'Ingredientes', '--c-insumo'],
    ['embalagens', 'Embalagens', '--c-embalagem'],
    ['maoDeObra', 'Seu trabalho', '--c-mao'],
    ['energia', 'Energia', '--c-energia'],
    ['gas', 'Gás', '--c-gas'],
    ['custosFixos', 'Custos fixos', '--c-fixos'],
];
const COMPONENTE = { INSUMO: 'Ingrediente', EMBALAGEM: 'Embalagem', MAO_DE_OBRA: 'Trabalho', ENERGIA: 'Energia', GAS: 'Gás', CUSTOS_FIXOS: 'Custos fixos' };
const BANDEIRAS = [['VERDE', 'Verde'], ['AMARELA', 'Amarela'], ['VERMELHA_1', 'Vermelha 1'], ['VERMELHA_2', 'Vermelha 2']];
const BASES = [['POR_LOTE', 'na receita toda'], ['POR_UNIDADE', 'em cada unidade']];

// ---------- list ----------
export async function listaReceitas() {
    let pagina = 0;
    let nome = '';
    const corpo = h('div');
    async function carregar() {
        const q = new URLSearchParams({ page: pagina, size: 20 });
        if (nome) q.set('nome', nome);
        const r = await get(`/receitas?${q}`);
        if (!r.content.length && !nome) {
            trocar(corpo, h('div', { class: 'painel' }, vazio('Nenhuma receita ainda. Monte a primeira com os insumos e embalagens cadastrados para ver o preço.',
                h('a', { class: 'botao primario', href: '#/receitas/nova' }, 'Criar receita'))));
            return;
        }
        trocar(corpo, h('div', { class: 'painel' },
            h('div', { class: 'tabela-wrap' }, h('table', null,
                h('thead', null, h('tr', null, h('th', null, 'Receita'), h('th', { class: 'dir' }, 'Rende'), h('th', { class: 'dir' }, 'Margem'), h('th', null, h('span', { class: 'sr-only' }, 'Abrir')))),
                h('tbody', null, r.content.length ? r.content.map((x) => h('tr', null,
                    h('td', null, h('a', { href: `#/receitas/${x.id}` }, x.nome)),
                    h('td', { class: 'dir num' }, `${x.rendimento} un`),
                    h('td', { class: 'dir num' }, pct(x.margemLucro)),
                    h('td', { class: 'acoes-linha' }, h('a', { class: 'botao', href: `#/receitas/${x.id}` }, 'Ver preço'))))
                    : h('tr', null, h('td', { colspan: 4, class: 'vazio' }, 'Nenhuma receita com esse nome.'))))),
            paginacao(r, (p) => { pagina = p; carregar(); })));
    }
    await carregar();
    let t;
    return h('div', null,
        h('header', { class: 'cabecalho' },
            h('div', null, h('h1', null, 'Receitas e preços'), h('p', null, 'O preço é recalculado sempre que o custo de um insumo muda.')),
            h('div', { class: 'acoes' },
                h('input', { type: 'search', class: 'busca', placeholder: 'Buscar receita', 'aria-label': 'Buscar receita',
                    oninput: (ev) => { clearTimeout(t); t = setTimeout(() => { nome = ev.target.value.trim(); pagina = 0; carregar(); }, 250); } }),
                h('a', { class: 'botao primario', href: '#/receitas/nova' }, 'Criar receita'))),
        corpo);
}

// ---------- detail: price, simulation, production ----------
export async function detalheReceita({ id }) {
    const receita = await get(`/receitas/${id}`);
    const blocoPreco = h('section', { class: 'painel' });
    const blocoProducao = h('section', { class: 'painel' });

    let simulacao = null;
    async function carregarPreco() {
        try {
            const preco = simulacao ? await post(`/receitas/${id}/simulacao`, simulacao) : await get(`/receitas/${id}/preco`);
            trocar(blocoPreco, painelPreco(preco, Boolean(simulacao)));
        } catch (e) {
            if (e.status !== 422) throw e;
            trocar(blocoPreco, h('h2', null, 'Preço'),
                h('div', { class: 'alerta aviso' }, blocoProblema(e),
                    h('p', null, simulacao ? 'Ajuste os valores da simulação.' : h('a', { href: '#/configuracao' }, 'Revisar meus custos'))),
                simulacao ? h('p', null, h('button', { type: 'button', onclick: () => { simulacao = null; carregarPreco(); } }, 'Voltar ao preço salvo')) : null);
        }
    }

    function painelPreco(p, simulando) {
        const linhas = [p.precoBase, ...p.precosPorCanal];
        return [
            h('div', { class: 'cabecalho' },
                h('h2', null, simulando ? 'Simulação' : 'Preço por unidade'),
                simulando ? h('div', { class: 'acoes' }, h('span', { class: 'selo atencao' }, 'nada foi salvo'),
                    h('button', { type: 'button', class: 'discreto', onclick: () => { simulacao = null; carregarPreco(); } }, 'Voltar ao preço salvo')) : null),
            h('div', { class: `preco-grade ${simulando ? 'simulando' : ''}` },
                fatia(p.composicaoLote, p.rendimento),
                h('div', null,
                    h('p', { class: 'muted small' }, `${p.precoBase.canal}, com ${pct(p.margemLucro)} de margem`),
                    h('p', { class: 'preco-destaque num' }, brl(p.precoBase.preco)),
                    h('p', { class: 'preco-sub' }, `Custa ${brl(p.custoUnitario)} por unidade. Você lucra `, h('strong', { class: 'num' }, brl(p.precoBase.lucroUnitario)), ' em cada uma.'),
                    h('div', { class: 'tabela-wrap' }, h('table', null,
                        h('thead', null, h('tr', null, h('th', null, 'Canal'), h('th', { class: 'dir' }, 'Taxa'), h('th', { class: 'dir' }, 'Preço'), h('th', { class: 'dir' }, 'Taxa paga'), h('th', { class: 'dir' }, 'Seu lucro'))),
                        h('tbody', null, linhas.map((l) => h('tr', null,
                            h('td', null, l.canal),
                            h('td', { class: 'dir num' }, pct(l.taxaPercentual)),
                            h('td', { class: 'dir num' }, h('strong', null, brl(l.preco))),
                            h('td', { class: 'dir num' }, brl(l.valorTaxa)),
                            h('td', { class: 'dir num' }, brl(l.lucroUnitario))))))),
                    p.precosPorCanal.length ? null : h('p', { class: 'small muted' }, 'Cadastre ', h('a', { href: '#/canais' }, 'canais de venda'), ' para ver o preço com a taxa de cada um.'),
                    formSimulacao(p))),
            h('details', { class: 'bloco' },
                h('summary', null, 'Ver a conta item por item'),
                h('div', { class: 'tabela-wrap' }, h('table', null,
                    h('thead', null, h('tr', null, h('th', null, 'Tipo'), h('th', null, 'Item'), h('th', { class: 'dir' }, 'Quantidade no lote'), h('th', { class: 'dir' }, 'Custo unit.'), h('th', { class: 'dir' }, 'Custo'))),
                    h('tbody', null, p.detalhamento.map((l) => h('tr', null,
                        h('td', null, COMPONENTE[l.componente] ?? l.componente),
                        h('td', null, l.item),
                        h('td', { class: 'dir num' }, `${num(l.quantidade, 4)} ${l.unidade}`),
                        h('td', { class: 'dir num' }, custoUnitario(l.custoUnitario)),
                        h('td', { class: 'dir num' }, brl(l.custo)))))))),
        ];
    }

    function formSimulacao(p) {
        const form = h('form', { class: 'campos bloco', novalidate: true },
            h('h3', null, 'E se...?'),
            h('div', { class: 'grade' },
                campo('Margem (%)', entrada('margemLucro', { tipo: 'number', min: 0, max: 99.99, step: '0.5', valor: simulacao?.margemLucro ?? p.margemLucro })),
                campo('Bandeira', selecao('bandeira', [['', 'A vigente'], ...BANDEIRAS], simulacao?.bandeira ?? '')),
                campo('Taxa avulsa (%)', entrada('taxaPercentual', { tipo: 'number', min: 0, max: 99.99, step: '0.5', valor: simulacao?.taxaPercentual, placeholder: 'opcional' }))),
            h('div', null, h('button', { type: 'submit' }, 'Simular preço')));
        form.addEventListener('submit', (ev) => {
            ev.preventDefault();
            const d = lerForm(form);
            simulacao = Object.fromEntries(Object.entries(d).filter(([, v]) => v != null));
            ocupado(form.querySelector('[type=submit]'), carregarPreco);
        });
        return form;
    }

    async function carregarProducao() {
        const cap = await get(`/receitas/${id}/capacidade-producao`);
        const form = h('form', { class: 'campos', novalidate: true },
            h('div', { class: 'filtros' },
                campo('Lotes', entrada('lotes', { tipo: 'number', min: 1, max: 1000, step: '1', valor: 1 })),
                h('button', { type: 'submit', class: 'primario' }, 'Registrar produção')));
        form.addEventListener('submit', (ev) => {
            ev.preventDefault();
            ocupado(form.querySelector('[type=submit]'), async () => {
                try {
                    const r = await post('/producoes', { receitaId: id, lotes: lerForm(form).lotes });
                    aviso(`Produção registrada: ${r.unidadesProduzidas} un, ${r.baixas.length} itens baixados do estoque.`);
                    carregarProducao();
                } catch (e) { mostrarErro(form, e); }
            });
        });
        const fl = cap.fatorLimitante;
        trocar(blocoProducao,
            h('h2', null, 'Produzir'),
            h('p', null, 'Com o estoque de hoje dá para fazer ', h('strong', { class: 'num' }, `${cap.lotesPossiveis} ${cap.lotesPossiveis === 1 ? 'lote' : 'lotes'}`),
                ` (${cap.unidadesPossiveis} un).`,
                fl ? [' Quem limita é ', h('strong', null, fl.item), `: tem ${num(fl.saldo, 3)} ${unidade(fl.unidade)} e cada lote usa ${num(fl.necessarioPorLote, 3)}.`] : null),
            form,
            h('p', { class: 'small muted' }, 'A produção baixa todos os itens de uma vez. Se faltar algum, nada é baixado e a lista do que falta aparece aqui. ',
                h('a', { href: '#/estoque' }, 'Registrar compras')));
    }

    const excluir = async () => {
        if (!(await confirmar(`Excluir ${receita.nome}?`, 'A receita sai da lista. Produções já registradas continuam no histórico.'))) return;
        try { await del(`/receitas/${id}`); aviso('Receita excluída.'); ir('/receitas'); } catch (e) { aviso(e.message, 'erro'); }
    };

    await Promise.all([carregarPreco(), carregarProducao()]);

    const minutos = (m) => (m >= 60 ? `${Math.floor(m / 60)} h${m % 60 ? ` ${m % 60} min` : ''}` : `${m} min`);
    return h('div', null,
        h('a', { class: 'voltar', href: '#/receitas' }, 'Todas as receitas'),
        h('header', { class: 'cabecalho' },
            h('div', null, h('h1', null, receita.nome),
                h('p', null, `Rende ${receita.rendimento} un, ${minutos(receita.tempoPreparoMin)} de trabalho${receita.minutosGas ? `, ${minutos(receita.minutosGas)} de forno a gás` : ''}.`)),
            h('div', { class: 'acoes' },
                h('a', { class: 'botao', href: `#/receitas/${id}/editar` }, 'Editar receita'),
                h('button', { type: 'button', class: 'perigo', onclick: excluir }, 'Excluir'))),
        blocoPreco,
        h('div', { class: 'colunas bloco' },
            blocoProducao,
            h('section', { class: 'painel' },
                h('h2', null, 'O que vai na receita'),
                h('ul', { class: 'lista-simples' },
                    receita.insumos.map((i) => h('li', null, i.nome, h('span', { class: 'num muted' }, `${num(i.quantidade, 3)} ${unidade(i.unidade)} ${i.base === 'POR_UNIDADE' ? 'por unidade' : ''}`))),
                    receita.embalagens.map((e) => h('li', null, e.nome, h('span', { class: 'num muted' }, `${num(e.quantidade, 3)} un ${e.base === 'POR_UNIDADE' ? 'por unidade' : 'no lote'}`))),
                    receita.equipamentos.map((q) => h('li', null, q.nome, h('span', { class: 'num muted' }, `${q.minutosUso} min`)))))));
}

/** The cost slice: each layer's thickness is its share of the batch cost. */
export function fatia(c, rendimento) {
    const total = Number(c.total) || 0;
    const visiveis = CAMADAS.filter(([k]) => Number(c[k]) > 0);
    return h('div', null,
        h('div', { class: 'fatia' },
            h('div', { class: 'fatia-corpo entra', role: 'img', 'aria-label': `Composição do custo do lote: ${visiveis.map(([k, r]) => `${r} ${brl(c[k])}`).join(', ')}` },
                visiveis.map(([k, , cor]) => h('div', { class: 'camada', style: { background: `var(${cor})`, flexGrow: String(Number(c[k])) } }))),
            h('ul', { class: 'fatia-legenda' }, visiveis.map(([k, rotulo, cor]) => h('li', null,
                h('span', { class: 'cor', style: { background: `var(${cor})` } }),
                h('span', null, rotulo, h('span', { class: 'pct small' }, `${total ? Math.round((Number(c[k]) / total) * 100) : 0}%`)),
                h('span', { class: 'num' }, brl(c[k])))))),
        h('div', { class: 'fatia-total' }, h('span', null, `Custo do lote (${rendimento} un)`), h('strong', { class: 'num' }, brl(c.total))));
}

// ---------- editor ----------
export async function editorReceita({ id }) {
    const [receita, insumos, embalagens, equipamentos] = await Promise.all([
        id ? get(`/receitas/${id}`) : null, todos('/insumos'), todos('/embalagens'), todos('/equipamentos'),
    ]);

    if (!insumos.length) {
        return h('div', null, h('header', { class: 'cabecalho' }, h('h1', null, 'Nova receita')),
            h('div', { class: 'painel' }, vazio('Para montar uma receita, cadastre primeiro os insumos que ela usa.',
                h('a', { class: 'botao primario', href: '#/insumos' }, 'Cadastrar insumos'))));
    }

    const listaIns = h('div', { class: 'itens' });
    const listaEmb = h('div', { class: 'itens' });
    const listaEq = h('div', { class: 'itens' });

    const remover = (linha) => h('button', { type: 'button', class: 'perigo', 'aria-label': 'Remover item', onclick: () => linha.remove() }, 'Remover');

    function linhaInsumo(item) {
        const unidadeSel = h('select', { 'data-k': 'unidade', 'aria-label': 'Unidade' });
        const ajustar = (insumoId) => {
            const ins = insumos.find((i) => i.id === insumoId) || insumos[0];
            trocar(unidadeSel, UNIDADES_POR_BASE[ins.unidadeBase].map(([v, r]) => h('option', { value: v }, r)));
        };
        const sel = selecao('', insumos.map((i) => [i.id, i.nome]), item?.insumoId, { 'data-k': 'insumoId', 'aria-label': 'Insumo', onchange: (ev) => ajustar(ev.target.value) });
        ajustar(sel.value);
        if (item) unidadeSel.value = item.unidade;
        const linha = h('div', { class: 'item-linha' },
            campo('Insumo', sel),
            campo('Quantidade', h('input', { type: 'number', step: 'any', min: 0, 'data-k': 'quantidade', value: item?.quantidade ?? '' })),
            campo('Unidade', unidadeSel),
            campo('Conta', selecao('', BASES, item?.base ?? 'POR_LOTE', { 'data-k': 'base', 'aria-label': 'Base da quantidade' })));
        linha.append(remover(linha));
        return linha;
    }
    function linhaEmbalagem(item) {
        const linha = h('div', { class: 'item-linha emb' },
            campo('Embalagem', selecao('', embalagens.map((e) => [e.id, e.nome]), item?.embalagemId, { 'data-k': 'embalagemId' })),
            campo('Quantidade', h('input', { type: 'number', step: 'any', min: 0, 'data-k': 'quantidade', value: item?.quantidade ?? 1 })),
            campo('Conta', selecao('', BASES, item?.base ?? 'POR_UNIDADE', { 'data-k': 'base' })));
        linha.append(remover(linha));
        return linha;
    }
    function linhaEquip(item) {
        const linha = h('div', { class: 'item-linha eq' },
            campo('Equipamento', selecao('', equipamentos.map((e) => [e.id, `${e.nome} (${num(e.potenciaWatts, 0)} W)`]), item?.equipamentoId, { 'data-k': 'equipamentoId' })),
            campo('Minutos de uso', h('input', { type: 'number', step: '1', min: 1, 'data-k': 'minutosUso', value: item?.minutosUso ?? '' })));
        linha.append(remover(linha));
        return linha;
    }

    (receita?.insumos.length ? receita.insumos : [null]).forEach((i) => listaIns.append(linhaInsumo(i)));
    receita?.embalagens.forEach((e) => listaEmb.append(linhaEmbalagem(e)));
    receita?.equipamentos.forEach((e) => listaEq.append(linhaEquip(e)));

    const ler = (lista, numericos) => [...lista.children].map((linha) => {
        const o = {};
        linha.querySelectorAll('[data-k]').forEach((el) => {
            o[el.dataset.k] = numericos.includes(el.dataset.k) ? (el.value === '' ? null : Number(el.value)) : el.value;
        });
        return o;
    });

    const form = h('form', { class: 'campos', novalidate: true },
        h('fieldset', null, h('legend', null, 'Sobre a receita'),
            h('div', { class: 'campos' },
                campo('Nome', entrada('nome', { required: true, maxlength: 120, valor: receita?.nome, placeholder: 'Ex.: Brigadeiro gourmet' })),
                h('div', { class: 'grade' },
                    campo('Rende quantas unidades?', entrada('rendimento', { tipo: 'number', min: 1, step: '1', valor: receita?.rendimento }), 'Fatias, docinhos ou 1 para o bolo inteiro.'),
                    campo('Tempo de trabalho (min)', entrada('tempoPreparoMin', { tipo: 'number', min: 0, step: '1', valor: receita?.tempoPreparoMin })),
                    campo('Forno a gás (min)', entrada('minutosGas', { tipo: 'number', min: 0, step: '1', valor: receita?.minutosGas ?? 0 })),
                    campo('Margem de lucro (%)', entrada('margemLucro', { tipo: 'number', min: 0, max: 99.99, step: '0.5', valor: receita?.margemLucro ?? 30 }), 'Parte do preço que é lucro.'),
                    campo('Vendas por mês', entrada('vendasEstimadasMes', { tipo: 'number', min: 0, step: '1', valor: receita?.vendasEstimadasMes }), 'Só se os custos fixos forem divididos por vendas.')))),
        h('fieldset', null, h('legend', null, 'Insumos'),
            h('p', { class: 'small muted' }, '"Na receita toda" é a quantidade do lote inteiro. "Em cada unidade" é multiplicada pelo rendimento.'),
            listaIns,
            h('p', null, h('button', { type: 'button', class: 'discreto', onclick: () => listaIns.append(linhaInsumo()) }, 'Adicionar insumo'))),
        h('fieldset', null, h('legend', null, 'Embalagens'),
            listaEmb,
            embalagens.length
                ? h('p', null, h('button', { type: 'button', class: 'discreto', onclick: () => listaEmb.append(linhaEmbalagem()) }, 'Adicionar embalagem'))
                : h('p', { class: 'small muted' }, 'Nenhuma embalagem cadastrada. ', h('a', { href: '#/embalagens' }, 'Cadastrar embalagens'))),
        h('fieldset', null, h('legend', null, 'Equipamentos elétricos'),
            listaEq,
            equipamentos.length
                ? h('p', null, h('button', { type: 'button', class: 'discreto', onclick: () => listaEq.append(linhaEquip()) }, 'Adicionar equipamento'))
                : h('p', { class: 'small muted' }, 'Nenhum equipamento cadastrado. ', h('a', { href: '#/equipamentos' }, 'Cadastrar equipamentos'))),
        h('div', { class: 'acoes' },
            h('button', { type: 'submit', class: 'primario' }, id ? 'Salvar receita' : 'Criar receita'),
            h('a', { class: 'botao', href: id ? `#/receitas/${id}` : '#/receitas' }, 'Cancelar')));

    form.addEventListener('submit', (ev) => {
        ev.preventDefault();
        limparErros(form);
        const d = lerForm(form);
        delete d['']; // unnamed item selects
        const req = {
            ...d,
            minutosGas: d.minutosGas ?? 0,
            insumos: ler(listaIns, ['quantidade']),
            embalagens: ler(listaEmb, ['quantidade']),
            equipamentos: ler(listaEq, ['minutosUso']),
        };
        ocupado(form.querySelector('[type=submit]'), async () => {
            try {
                const r = id ? await put(`/receitas/${id}`, req) : await post('/receitas', req);
                aviso(id ? 'Receita salva.' : 'Receita criada.');
                ir(`/receitas/${r.id}`);
            } catch (e) { mostrarErro(form, e); form.scrollIntoView({ behavior: 'smooth' }); }
        });
    });

    return h('div', null,
        h('a', { class: 'voltar', href: id ? `#/receitas/${id}` : '#/receitas' }, id ? 'Voltar para a receita' : 'Todas as receitas'),
        h('header', { class: 'cabecalho' }, h('div', null, h('h1', null, id ? `Editar ${receita.nome}` : 'Nova receita'),
            h('p', null, 'Informe as quantidades como na sua receita. O preço aparece assim que você salvar.'))),
        form);
}
