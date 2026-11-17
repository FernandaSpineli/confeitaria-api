import { get, post, todos } from '../api.js';
import {
    h, trocar, campo, entrada, selecao, lerForm, mostrarErro, ocupado, aviso, dialogo, vazio,
    brl, num, custoUnitario, unidade, dataHora,
} from '../dom.js';
import { UNIDADES_POR_BASE, paginacao } from './cadastros.js';

const MOTIVOS = { COMPRA: 'Compra', PRODUCAO: 'Produção', PERDA: 'Perda', VENCIMENTO: 'Vencimento', INVENTARIO: 'Contagem de estoque', OUTRO: 'Outro' };
const TIPOS = { ENTRADA: 'Entrada', SAIDA: 'Saída', AJUSTE: 'Ajuste' };

/** Purchase dialog. tipo: 'insumo' | 'embalagem'. If item is null, the user picks one. Resolves true when saved. */
export async function dialogoCompra(tipo, item) {
    const opcoes = item ? null : await opcoesItens();
    return dialogo((fechar) => {
        const unidadeSel = h('select', { name: 'unidade' });
        const ajustarUnidades = (base) => trocar(unidadeSel, (UNIDADES_POR_BASE[base] || UNIDADES_POR_BASE.UN).map(([v, r]) => h('option', { value: v }, r)));
        ajustarUnidades(item ? (item.unidadeBase || 'UN') : opcoes[0]?.base);

        const escolha = opcoes ? selecao('item', opcoes.map((o) => [o.chave, o.rotulo]), null, {
            onchange: (ev) => ajustarUnidades(opcoes.find((o) => o.chave === ev.target.value).base),
        }) : null;

        const form = h('form', { class: 'campos', novalidate: true },
            h('h2', null, item ? `Registrar compra: ${item.nome}` : 'Registrar compra'),
            escolha ? campo('Item', escolha) : null,
            h('div', { class: 'grade' },
                campo('Quantidade comprada', entrada('quantidade', { tipo: 'number', min: 0 })),
                campo('Unidade', unidadeSel),
                campo('Valor pago (R$)', entrada('valorPago', { tipo: 'number', min: 0, step: '0.01' }), 'Total da nota para essa quantidade.')),
            campo('Observação', entrada('observacao', { maxlength: 255, placeholder: 'Opcional, ex.: atacadão' })),
            h('p', { class: 'small muted' }, 'O custo do item passa a ser a média ponderada entre o que havia em estoque e esta compra.'),
            h('div', { class: 'dialogo-rodape' },
                h('button', { type: 'button', onclick: () => fechar(false) }, 'Cancelar'),
                h('button', { type: 'submit', class: 'primario' }, 'Registrar compra')));

        if (!item && !opcoes.length) {
            return [h('h2', null, 'Registrar compra'), vazio('Cadastre insumos ou embalagens antes de registrar compras.'),
                h('div', { class: 'dialogo-rodape' }, h('button', { type: 'button', onclick: () => fechar(false) }, 'Fechar'))];
        }

        form.addEventListener('submit', (ev) => {
            ev.preventDefault();
            ocupado(form.querySelector('[type=submit]'), async () => {
                const d = lerForm(form);
                const alvo = item ? { tipo, id: item.id } : separar(d.item);
                const req = { quantidade: d.quantidade, unidade: d.unidade, valorPago: d.valorPago, observacao: d.observacao };
                req[alvo.tipo === 'insumo' ? 'insumoId' : 'embalagemId'] = alvo.id;
                try {
                    const r = await post('/estoque/compras', req);
                    const mudou = Number(r.custoUnitarioAnterior) !== Number(r.custoUnitarioAtual);
                    aviso(`Compra registrada. Saldo: ${num(r.saldoAtual, 3)}.${mudou ? ` Custo médio: ${custoUnitario(r.custoUnitarioAnterior)} → ${custoUnitario(r.custoUnitarioAtual)}.` : ''}`);
                    fechar(true);
                } catch (e) { mostrarErro(form, e); }
            });
        });
        return form;
    });
}

async function opcoesItens() {
    const [ins, embs] = await Promise.all([todos('/insumos'), todos('/embalagens')]);
    return [
        ...ins.map((i) => ({ chave: `insumo:${i.id}`, rotulo: `${i.nome} (insumo, ${unidade(i.unidadeBase)})`, base: i.unidadeBase })),
        ...embs.map((e) => ({ chave: `embalagem:${e.id}`, rotulo: `${e.nome} (embalagem)`, base: 'UN' })),
    ];
}
const separar = (chave) => { const [tipo, id] = chave.split(':'); return { tipo, id }; };

async function dialogoAjuste() {
    const opcoes = await opcoesItens();
    return dialogo((fechar) => {
        if (!opcoes.length) {
            return [h('h2', null, 'Ajustar estoque'), vazio('Cadastre insumos ou embalagens primeiro.'),
                h('div', { class: 'dialogo-rodape' }, h('button', { type: 'button', onclick: () => fechar(false) }, 'Fechar'))];
        }
        const unidadeSel = h('select', { name: 'unidade' });
        const ajustar = (base) => trocar(unidadeSel, (UNIDADES_POR_BASE[base] || UNIDADES_POR_BASE.UN).map(([v, r]) => h('option', { value: v }, r)));
        ajustar(opcoes[0].base);
        const form = h('form', { class: 'campos', novalidate: true },
            h('h2', null, 'Ajustar estoque'),
            h('p', { class: 'muted' }, 'Use para perdas, itens vencidos ou para acertar o saldo depois de contar o estoque. O histórico nunca é apagado: o ajuste vira uma nova movimentação.'),
            campo('Item', selecao('item', opcoes.map((o) => [o.chave, o.rotulo]), null,
                { onchange: (ev) => ajustar(opcoes.find((o) => o.chave === ev.target.value).base) })),
            h('div', { class: 'grade' },
                campo('Quantidade', entrada('quantidade', { tipo: 'number' }), 'Negativa para retirar (ex.: -200), positiva para somar.'),
                campo('Unidade', unidadeSel),
                campo('Motivo', selecao('motivo', [['PERDA', 'Perda'], ['VENCIMENTO', 'Vencimento'], ['INVENTARIO', 'Contagem de estoque'], ['OUTRO', 'Outro']]))),
            campo('Observação', entrada('observacao', { maxlength: 255 })),
            h('div', { class: 'dialogo-rodape' },
                h('button', { type: 'button', onclick: () => fechar(false) }, 'Cancelar'),
                h('button', { type: 'submit', class: 'primario' }, 'Registrar ajuste')));
        form.addEventListener('submit', (ev) => {
            ev.preventDefault();
            ocupado(form.querySelector('[type=submit]'), async () => {
                const d = lerForm(form);
                const alvo = separar(d.item);
                const req = { quantidade: d.quantidade, unidade: d.unidade, motivo: d.motivo, observacao: d.observacao };
                req[alvo.tipo === 'insumo' ? 'insumoId' : 'embalagemId'] = alvo.id;
                try {
                    const m = await post('/estoque/ajustes', req);
                    aviso(`Ajuste registrado. ${m.item}: saldo ${num(m.saldoApos, 3)}.`);
                    fechar(true);
                } catch (e) { mostrarErro(form, e); }
            });
        });
        return form;
    });
}

export async function estoque() {
    const filtros = { tipo: '', de: '', ate: '', pagina: 0 };
    const blocoMinimo = h('section', { class: 'painel' });
    const blocoMov = h('div');

    async function carregarMinimo() {
        const itens = await get('/estoque/abaixo-minimo');
        trocar(blocoMinimo, h('h2', null, 'Abaixo do mínimo'),
            itens.length
                ? h('ul', { class: 'lista-simples' }, itens.map((i) => h('li', null,
                    h('span', null, i.nome, ' ', h('span', { class: 'selo' }, i.tipo === 'INSUMO' ? 'insumo' : 'embalagem')),
                    h('span', { class: 'num' }, `${num(i.saldo, 3)} de ${num(i.estoqueMinimo, 3)} ${unidade(i.unidade)} `,
                        h('span', { class: 'selo atencao' }, `faltam ${num(i.faltaParaMinimo, 3)}`)))))
                : h('p', { class: 'muted' }, 'Tudo acima do estoque mínimo.'));
    }

    async function carregarMov() {
        const q = new URLSearchParams({ page: filtros.pagina, size: 20 });
        for (const k of ['tipo', 'de', 'ate']) if (filtros[k]) q.set(k, filtros[k]);
        const r = await get(`/estoque/movimentacoes?${q}`);
        trocar(blocoMov, r.content.length
            ? [h('div', { class: 'tabela-wrap' }, h('table', null,
                h('thead', null, h('tr', null, ['Quando', 'Item', 'Movimento', 'Quantidade', 'Saldo depois', 'Custo unit.', 'Valor'].map((t, i) => h('th', { class: i >= 3 ? 'dir' : null }, t)))),
                h('tbody', null, r.content.map((m) => h('tr', null,
                    h('td', { class: 'num' }, dataHora(m.criadoEm)),
                    h('td', null, m.item, m.observacao ? h('div', { class: 'small muted' }, m.observacao) : null),
                    h('td', null, h('span', { class: `selo ${m.tipo === 'ENTRADA' ? 'ok' : m.tipo === 'SAIDA' ? 'rosa' : 'atencao'}` }, TIPOS[m.tipo]), ' ', h('span', { class: 'small muted' }, MOTIVOS[m.motivo])),
                    h('td', { class: 'dir num' }, `${Number(m.quantidade) > 0 ? '+' : ''}${num(m.quantidade, 3)}`),
                    h('td', { class: 'dir num' }, num(m.saldoApos, 3)),
                    h('td', { class: 'dir num' }, custoUnitario(m.custoUnitario)),
                    h('td', { class: 'dir num' }, brl(m.valorTotal))))))),
            paginacao(r, (p) => { filtros.pagina = p; carregarMov(); })]
            : h('p', { class: 'vazio' }, 'Nenhuma movimentação com esses filtros.'));
    }

    const aoFiltrar = (ev) => { filtros[ev.target.name] = ev.target.value; filtros.pagina = 0; carregarMov(); };
    const tudo = () => Promise.all([carregarMinimo(), carregarMov()]);
    await tudo();

    return h('div', null,
        h('header', { class: 'cabecalho' },
            h('div', null, h('h1', null, 'Estoque'),
                h('p', null, 'O saldo só muda por compras, produções e ajustes. Cada movimento fica registrado e não pode ser editado.')),
            h('div', { class: 'acoes' },
                h('button', { type: 'button', onclick: async () => { if (await dialogoAjuste()) tudo(); } }, 'Ajustar estoque'),
                h('button', { type: 'button', class: 'primario', onclick: async () => { if (await dialogoCompra(null, null)) tudo(); } }, 'Registrar compra'))),
        blocoMinimo,
        h('section', { class: 'painel' },
            h('h2', null, 'Movimentações'),
            h('div', { class: 'filtros' },
                campo('Tipo', selecao('tipo', [['', 'Todos'], ['ENTRADA', 'Entradas'], ['SAIDA', 'Saídas'], ['AJUSTE', 'Ajustes']], '', { onchange: aoFiltrar })),
                campo('De', h('input', { type: 'date', name: 'de', onchange: aoFiltrar })),
                campo('Até', h('input', { type: 'date', name: 'ate', onchange: aoFiltrar }))),
            blocoMov));
}

export async function producoes() {
    let pagina = 0;
    const corpo = h('section', { class: 'painel' });
    async function carregar() {
        const r = await get(`/producoes?page=${pagina}&size=20`);
        if (!r.content.length) {
            trocar(corpo, vazio('Nenhuma produção registrada. Abra uma receita e use "Registrar produção" para baixar os ingredientes do estoque.',
                h('a', { class: 'botao primario', href: '#/receitas' }, 'Ver receitas')));
            return;
        }
        trocar(corpo,
            r.content.map((p) => h('details', { class: 'bloco' },
                h('summary', null,
                    `${p.receita}: ${p.lotes} ${p.lotes === 1 ? 'lote' : 'lotes'} (${p.unidadesProduzidas} un) `,
                    h('span', { class: 'muted small' }, `${dataHora(p.criadoEm)}, materiais ${brl(p.custoMateriais)}`)),
                h('div', { class: 'tabela-wrap' }, h('table', null,
                    h('thead', null, h('tr', null, h('th', null, 'Item baixado'), h('th', { class: 'dir' }, 'Quantidade'), h('th', { class: 'dir' }, 'Saldo atual'))),
                    h('tbody', null, p.baixas.map((b) => h('tr', null,
                        h('td', null, b.item),
                        h('td', { class: 'dir num' }, `${num(b.quantidade, 3)} ${unidade(b.unidade)}`),
                        h('td', { class: 'dir num' }, `${num(b.saldoAtual, 3)} ${unidade(b.unidade)}`)))))))),
            paginacao(r, (p) => { pagina = p; carregar(); }));
    }
    await carregar();
    return h('div', null,
        h('header', { class: 'cabecalho' }, h('div', null, h('h1', null, 'Produções'),
            h('p', null, 'Cada produção baixa todos os ingredientes e embalagens de uma vez. Se faltar algo, nada é baixado.'))),
        corpo);
}
