import { get, post, put, del } from '../api.js';
import {
    h, trocar, campo, entrada, selecao, lerForm, mostrarErro, ocupado, aviso, dialogo, confirmar, vazio,
    brl, num, custoUnitario, pct, unidade,
} from '../dom.js';
import { dialogoCompra } from './estoque.js';

export const UNIDADES = [['G', 'g (gramas)'], ['KG', 'kg'], ['ML', 'ml'], ['L', 'litro'], ['UN', 'unidade']];
export const UNIDADES_POR_BASE = { G: [['G', 'g'], ['KG', 'kg']], ML: [['ML', 'ml'], ['L', 'L']], UN: [['UN', 'un']] };

/**
 * Generic list + create/edit dialog for a catalog resource.
 * cfg: { titulo, descricao, caminho, singular, colunas: [[rotulo, fn, classe]], campos(item) => nodes,
 *        paraRequest(dados), acoesExtras(item, atualizar) => nodes, busca, rodape(itens) }
 */
function cadastro(cfg) {
    return async () => {
        const estado = { pagina: 0, nome: '' };
        const corpo = h('div');

        const abrirForm = (item) => dialogo((fechar) => {
            const form = h('form', { class: 'campos', novalidate: true },
                h('h2', null, item ? `Editar ${cfg.singular}` : `Novo ${cfg.singular}`),
                cfg.campos(item),
                h('div', { class: 'dialogo-rodape' },
                    h('button', { type: 'button', onclick: () => fechar(false) }, 'Cancelar'),
                    h('button', { type: 'submit', class: 'primario' }, item ? 'Salvar alterações' : `Cadastrar ${cfg.singular}`)));
            form.addEventListener('submit', (ev) => {
                ev.preventDefault();
                ocupado(form.querySelector('[type=submit]'), async () => {
                    try {
                        const req = cfg.paraRequest(lerForm(form), item);
                        if (item) await put(`${cfg.caminho}/${item.id}`, req);
                        else await post(cfg.caminho, req);
                        aviso(item ? 'Alterações salvas.' : `${cap(cfg.singular)} cadastrado.`);
                        fechar(true);
                    } catch (e) { mostrarErro(form, e); }
                });
            });
            return form;
        }).then((salvou) => salvou && atualizar());

        const excluir = async (item) => {
            const nome = item.nome ?? item.descricao;
            if (!(await confirmar(`Excluir ${nome}?`, cfg.avisoExclusao || 'Esta ação não pode ser desfeita.'))) return;
            try {
                await del(`${cfg.caminho}/${item.id}`);
                aviso(`${nome} excluído.`);
                atualizar();
            } catch (e) { aviso(e.message, 'erro'); }
        };

        async function atualizar() {
            const q = new URLSearchParams({ page: estado.pagina, size: 20 });
            if (estado.nome) q.set('nome', estado.nome);
            const r = await get(`${cfg.caminho}?${q}`);
            if (!r.content.length && !estado.nome) {
                trocar(corpo, h('div', { class: 'painel' }, vazio(cfg.vazio,
                    h('button', { class: 'primario', type: 'button', onclick: () => abrirForm() }, `Cadastrar ${cfg.singular}`))));
                return;
            }
            trocar(corpo, h('div', { class: 'painel' },
                cfg.rodapeTopo ? cfg.rodapeTopo(r.content) : null,
                h('div', { class: 'tabela-wrap' }, h('table', null,
                    h('thead', null, h('tr', null, cfg.colunas.map(([rot, , cl]) => h('th', { class: cl }, rot)), h('th', null, h('span', { class: 'sr-only' }, 'Ações')))),
                    h('tbody', null, r.content.length
                        ? r.content.map((item) => h('tr', null,
                            cfg.colunas.map(([, fn, cl]) => h('td', { class: cl }, fn(item))),
                            h('td', { class: 'acoes-linha' },
                                cfg.acoesExtras ? cfg.acoesExtras(item, atualizar) : null,
                                h('button', { type: 'button', class: 'discreto', onclick: () => abrirForm(item) }, 'Editar'),
                                h('button', { type: 'button', class: 'perigo', onclick: () => excluir(item) }, 'Excluir'))))
                        : h('tr', null, h('td', { colspan: cfg.colunas.length + 1, class: 'vazio' }, 'Nada encontrado com esse nome.'))))),
                paginacao(r, (p) => { estado.pagina = p; atualizar(); })));
        }

        await atualizar();

        const busca = cfg.busca ? h('input', {
            type: 'search', class: 'busca', placeholder: 'Buscar pelo nome', 'aria-label': 'Buscar pelo nome',
            oninput: debounce((ev) => { estado.nome = ev.target.value.trim(); estado.pagina = 0; atualizar(); }, 250),
        }) : null;

        return h('div', null,
            h('header', { class: 'cabecalho' },
                h('div', null, h('h1', null, cfg.titulo), h('p', null, cfg.descricao)),
                h('div', { class: 'acoes' }, busca, h('button', { type: 'button', class: 'primario', onclick: () => abrirForm() }, `Cadastrar ${cfg.singular}`))),
            corpo);
    };
}

export function paginacao(r, irPara) {
    if (r.totalPages <= 1) return null;
    return h('div', { class: 'paginacao' },
        h('button', { type: 'button', disabled: r.page === 0, onclick: () => irPara(r.page - 1) }, 'Anterior'),
        h('span', null, `Página ${r.page + 1} de ${r.totalPages} (${r.totalElements} itens)`),
        h('button', { type: 'button', disabled: r.page + 1 >= r.totalPages, onclick: () => irPara(r.page + 1) }, 'Próxima'));
}

function debounce(fn, ms) {
    let t;
    return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
}
const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1);

function saldo(item, base) {
    const u = unidade(base);
    return h('span', null, h('span', { class: 'num' }, `${num(item.saldo, 3)} ${u}`), ' ',
        item.abaixoDoMinimo ? h('span', { class: 'selo atencao' }, 'abaixo do mínimo') : null);
}

const botaoCompra = (tipo) => (item, atualizar) => h('button', {
    type: 'button', class: 'discreto',
    onclick: async () => { if (await dialogoCompra(tipo, item)) atualizar(); },
}, 'Registrar compra');

export const insumos = cadastro({
    titulo: 'Insumos',
    descricao: 'Ingredientes com o preço da embalagem que você compra. O custo por grama, ml ou unidade é calculado sozinho e atualiza o preço das receitas.',
    caminho: '/insumos',
    singular: 'insumo',
    busca: true,
    vazio: 'Nenhum insumo ainda. Cadastre os ingredientes que você usa, como farinha, leite condensado e ovos.',
    avisoExclusao: 'Insumos usados em receitas ativas não podem ser excluídos.',
    colunas: [
        ['Nome', (i) => i.nome],
        ['Embalagem', (i) => h('span', { class: 'num' }, `${num(i.ultimaQuantidadeEmbalagem, 3)} ${unidade(i.unidadeBase)} por ${brl(i.ultimoPrecoEmbalagem)}`)],
        ['Custo', (i) => h('span', { class: 'num' }, `${custoUnitario(i.custoUnitario)} / ${unidade(i.unidadeBase)}`), 'dir'],
        ['Em estoque', (i) => saldo(i, i.unidadeBase)],
    ],
    campos: (i) => [
        campo('Nome', entrada('nome', { required: true, maxlength: 120, valor: i?.nome })),
        h('div', { class: 'grade' },
            campo('Unidade', selecao('unidade', UNIDADES, i?.unidadeBase ?? 'G')),
            campo('Quantidade na embalagem', entrada('quantidadeEmbalagem', { tipo: 'number', min: 0, valor: i?.ultimaQuantidadeEmbalagem }), 'Ex.: 395 para uma lata de 395 g.'),
            campo('Preço da embalagem (R$)', entrada('precoEmbalagem', { tipo: 'number', min: 0, step: '0.01', valor: i?.ultimoPrecoEmbalagem }))),
        campo('Estoque mínimo', entrada('estoqueMinimo', { tipo: 'number', min: 0, valor: i?.estoqueMinimo ?? 0 }), 'Na mesma unidade escolhida acima. Você recebe um alerta quando o saldo ficar abaixo.'),
    ],
    paraRequest: (d) => d,
    acoesExtras: botaoCompra('insumo'),
});

export const embalagens = cadastro({
    titulo: 'Embalagens',
    descricao: 'Caixas, forminhas, etiquetas e o que mais vai junto com o produto, com o custo de cada unidade.',
    caminho: '/embalagens',
    singular: 'embalagem',
    busca: true,
    vazio: 'Nenhuma embalagem ainda. Cadastre forminhas, caixas e etiquetas para incluí-las no preço.',
    avisoExclusao: 'Embalagens usadas em receitas ativas não podem ser excluídas.',
    colunas: [
        ['Nome', (e) => e.nome],
        ['Custo por unidade', (e) => h('span', { class: 'num' }, custoUnitario(e.custoUnitario)), 'dir'],
        ['Em estoque', (e) => saldo(e, 'un')],
    ],
    campos: (e) => [
        campo('Nome', entrada('nome', { required: true, maxlength: 120, valor: e?.nome })),
        h('div', { class: 'grade' },
            campo('Custo por unidade (R$)', entrada('custoUnitario', { tipo: 'number', min: 0, step: '0.0001', valor: e?.custoUnitario }), 'Pacote de 100 por R$ 6,00 = 0,06.'),
            campo('Estoque mínimo (un)', entrada('estoqueMinimo', { tipo: 'number', min: 0, step: '1', valor: e?.estoqueMinimo ?? 0 }))),
    ],
    paraRequest: (d) => d,
    acoesExtras: botaoCompra('embalagem'),
});

export const equipamentos = cadastro({
    titulo: 'Equipamentos',
    descricao: 'Aparelhos elétricos e a potência de cada um. O consumo entra no preço pelos minutos de uso em cada receita.',
    caminho: '/equipamentos',
    singular: 'equipamento',
    vazio: 'Nenhum equipamento ainda. Cadastre batedeira, forno elétrico ou micro-ondas com a potência da etiqueta.',
    avisoExclusao: 'Equipamentos usados em receitas ativas não podem ser excluídos.',
    colunas: [
        ['Nome', (e) => e.nome],
        ['Potência', (e) => h('span', { class: 'num' }, `${num(e.potenciaWatts, 0)} W`), 'dir'],
    ],
    campos: (e) => [
        campo('Nome', entrada('nome', { required: true, maxlength: 120, valor: e?.nome })),
        campo('Potência (watts)', entrada('potenciaWatts', { tipo: 'number', min: 1, valor: e?.potenciaWatts }), 'Está na etiqueta ou no manual. Ex.: batedeira 500 W.'),
    ],
    paraRequest: (d) => d,
});

export const custosFixos = cadastro({
    titulo: 'Custos fixos',
    descricao: 'Contas do mês que existem mesmo sem produzir: aluguel, internet, MEI, água. São divididas entre as receitas.',
    caminho: '/custos-fixos',
    singular: 'custo fixo',
    vazio: 'Nenhum custo fixo ainda. Cadastre as contas mensais para que elas entrem no preço.',
    colunas: [
        ['Descrição', (c) => c.descricao],
        ['Valor por mês', (c) => h('span', { class: 'num' }, brl(c.valorMensal)), 'dir'],
        ['Situação', (c) => (c.ativo ? h('span', { class: 'selo ok' }, 'entra no preço') : h('span', { class: 'selo' }, 'pausado'))],
    ],
    rodapeTopo: (itens) => {
        const total = itens.filter((c) => c.ativo).reduce((s, c) => s + Number(c.valorMensal), 0);
        return h('p', null, 'Total ativo nesta página: ', h('strong', { class: 'num' }, brl(total)), ' por mês.');
    },
    campos: (c) => [
        campo('Descrição', entrada('descricao', { required: true, maxlength: 120, valor: c?.descricao })),
        campo('Valor por mês (R$)', entrada('valorMensal', { tipo: 'number', min: 0, step: '0.01', valor: c?.valorMensal })),
        h('label', { class: 'check' }, h('input', { type: 'checkbox', name: 'ativo', checked: c ? c.ativo : true }), 'Incluir no preço'),
    ],
    paraRequest: (d) => d,
});

export const canais = cadastro({
    titulo: 'Canais de venda',
    descricao: 'Onde você vende e a taxa de cada lugar. Cada receita mostra um preço por canal, com a mesma margem de lucro.',
    caminho: '/canais-venda',
    singular: 'canal',
    vazio: 'Nenhum canal ainda. Cadastre, por exemplo, Maquininha (4%) e iFood (27%).',
    colunas: [
        ['Canal', (c) => c.nome],
        ['Taxa', (c) => h('span', { class: 'num' }, pct(c.taxaPercentual)), 'dir'],
        ['Situação', (c) => (c.ativo ? h('span', { class: 'selo ok' }, 'ativo') : h('span', { class: 'selo' }, 'pausado'))],
    ],
    campos: (c) => [
        campo('Nome', entrada('nome', { required: true, maxlength: 120, valor: c?.nome })),
        campo('Taxa (%)', entrada('taxaPercentual', { tipo: 'number', min: 0, max: 99.99, step: '0.01', valor: c?.taxaPercentual ?? 0 }), 'Digite 27 para 27%.'),
        h('label', { class: 'check' }, h('input', { type: 'checkbox', name: 'ativo', checked: c ? c.ativo : true }), 'Mostrar preço neste canal'),
    ],
    paraRequest: (d) => d,
});
