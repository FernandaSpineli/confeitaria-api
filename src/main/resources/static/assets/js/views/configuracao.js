import { get, put, post, del } from '../api.js';
import {
    h, trocar, campo, entrada, selecao, lerForm, mostrarErro, ocupado, aviso, confirmar,
    brl, num, data, hoje,
} from '../dom.js';

const BANDEIRAS = [['VERDE', 'Verde'], ['AMARELA', 'Amarela'], ['VERMELHA_1', 'Vermelha patamar 1'], ['VERMELHA_2', 'Vermelha patamar 2']];
const nomeBandeira = (b) => (BANDEIRAS.find(([v]) => v === b) || [b, b])[1];
const reais6 = (v) => `R$ ${num(v, 6)}`;

export async function configuracao() {
    const blocoTarifas = h('section', { class: 'painel' });
    const blocoBandeiras = h('section', { class: 'painel' });
    const config = await get('/configuracao');

    // ---------- monthly costs ----------
    const valorHora = h('strong', { class: 'num' }, brl(config.valorHora));
    const form = h('form', { class: 'campos', novalidate: true },
        h('div', { class: 'grade' },
            campo('Quanto quer ganhar por mês (R$)', entrada('salarioDesejadoMes', { tipo: 'number', min: 0, step: '0.01', valor: config.salarioDesejadoMes })),
            campo('Horas de trabalho por mês', entrada('horasTrabalhadasMes', { tipo: 'number', min: 1, step: '0.5', valor: config.horasTrabalhadasMes }), '8 h por dia, 20 dias = 160 h.')),
        h('p', null, 'Valor da sua hora: ', valorHora),
        h('div', { class: 'grade' },
            campo('Preço do botijão de gás (R$)', entrada('gasPrecoBotijao', { tipo: 'number', min: 0, step: '0.01', valor: config.gasPrecoBotijao, placeholder: 'Deixe vazio se não usa' })),
            campo('Horas de forno por botijão', entrada('gasDuracaoHoras', { tipo: 'number', min: 0, step: '0.5', valor: config.gasDuracaoHoras }))),
        h('div', { class: 'grade' },
            campo('Bandeira de energia deste mês', selecao('bandeiraVigente', BANDEIRAS, config.bandeiraVigente), 'Aparece na conta de luz.'),
            campo('Como dividir os custos fixos', selecao('criterioRateio', [['HORAS', 'Pelas horas de trabalho de cada receita'], ['VENDAS', 'Pelas vendas estimadas no mês']], config.criterioRateio))),
        h('label', { class: 'check' }, h('input', { type: 'checkbox', name: 'permitirSaldoNegativo', checked: config.permitirSaldoNegativo }),
            'Permitir produzir mesmo sem estoque suficiente (o saldo fica negativo)'),
        h('div', null, h('button', { type: 'submit', class: 'primario' }, 'Salvar custos')));
    form.addEventListener('submit', (ev) => {
        ev.preventDefault();
        ocupado(form.querySelector('[type=submit]'), async () => {
            try {
                const r = await put('/configuracao', lerForm(form));
                valorHora.textContent = brl(r.valorHora);
                aviso('Custos salvos. Os preços das receitas já foram atualizados.');
            } catch (e) { mostrarErro(form, e); }
        });
    });

    // ---------- tariff history ----------
    async function carregarTarifas() {
        const [lista, vigente] = await Promise.all([
            get('/configuracao/tarifas?size=50'),
            get('/configuracao/tarifas/vigente').catch((e) => { if (e.status === 404) return null; throw e; }),
        ]);
        const novo = h('form', { class: 'filtros', novalidate: true },
            campo('Tarifa por kWh (R$)', entrada('tarifaKwh', { tipo: 'number', min: 0, step: '0.000001', placeholder: 'Ex.: 0,80' })),
            campo('Vale a partir de', entrada('vigenciaInicio', { tipo: 'date', valor: hoje() })),
            h('button', { type: 'submit', class: 'primario' }, 'Adicionar tarifa'));
        novo.addEventListener('submit', (ev) => {
            ev.preventDefault();
            ocupado(novo.querySelector('[type=submit]'), async () => {
                try { await post('/configuracao/tarifas', lerForm(novo)); aviso('Tarifa adicionada.'); carregarTarifas(); } catch (e) { mostrarErro(novo, e); }
            });
        });
        trocar(blocoTarifas,
            h('h2', null, 'Tarifa de energia'),
            h('p', { class: 'muted' }, vigente
                ? ['Vigente hoje: ', h('strong', { class: 'num' }, reais6(vigente.tarifaKwh)), ` por kWh, desde ${data(vigente.vigenciaInicio)}.`]
                : 'Nenhuma tarifa vigente. Receitas com equipamento elétrico não terão preço até você cadastrar uma.'),
            novo,
            lista.content.length ? h('ul', { class: 'lista-simples' }, lista.content.map((t) => h('li', null,
                h('span', null, `Desde ${data(t.vigenciaInicio)}`),
                h('span', null, h('span', { class: 'num' }, `${reais6(t.tarifaKwh)}/kWh `),
                    h('button', { type: 'button', class: 'perigo', onclick: () => remover(`/configuracao/tarifas/${t.id}`, 'esta tarifa', carregarTarifas) }, 'Excluir'))))) : null);
    }

    // ---------- flag surcharge history ----------
    async function carregarBandeiras() {
        const [lista, vigentes] = await Promise.all([get('/configuracao/bandeiras?size=50'), get('/configuracao/bandeiras/vigentes')]);
        const novo = h('form', { class: 'filtros', novalidate: true },
            campo('Bandeira', selecao('bandeira', BANDEIRAS.filter(([v]) => v !== 'VERDE').concat([['VERDE', 'Verde']]))),
            campo('Adicional por kWh (R$)', entrada('adicionalKwh', { tipo: 'number', min: 0, step: '0.000001', placeholder: 'Ex.: 0,01885' })),
            campo('Vale a partir de', entrada('vigenciaInicio', { tipo: 'date', valor: hoje() })),
            h('button', { type: 'submit', class: 'primario' }, 'Adicionar valor'));
        novo.addEventListener('submit', (ev) => {
            ev.preventDefault();
            ocupado(novo.querySelector('[type=submit]'), async () => {
                try { await post('/configuracao/bandeiras', lerForm(novo)); aviso('Valor da bandeira adicionado.'); carregarBandeiras(); } catch (e) { mostrarErro(novo, e); }
            });
        });
        trocar(blocoBandeiras,
            h('h2', null, 'Adicional das bandeiras'),
            h('p', { class: 'muted' }, 'Valor extra por kWh cobrado em cada bandeira. A verde sem valor cadastrado conta como zero.'),
            vigentes.length ? h('p', null, 'Vigentes hoje: ', vigentes.map((v, i) => [i ? ', ' : '', h('strong', null, nomeBandeira(v.bandeira)), ` ${reais6(v.adicionalKwh)}`])) : null,
            novo,
            lista.content.length ? h('ul', { class: 'lista-simples' }, lista.content.map((a) => h('li', null,
                h('span', null, `${nomeBandeira(a.bandeira)}, desde ${data(a.vigenciaInicio)}`),
                h('span', null, h('span', { class: 'num' }, `${reais6(a.adicionalKwh)}/kWh `),
                    h('button', { type: 'button', class: 'perigo', onclick: () => remover(`/configuracao/bandeiras/${a.id}`, 'este valor', carregarBandeiras) }, 'Excluir'))))) : null);
    }

    async function remover(caminho, oQue, depois) {
        if (!(await confirmar(`Excluir ${oQue}?`, 'O histórico serve para calcular preços em datas diferentes. Exclua só se foi cadastrado errado.'))) return;
        try { await del(caminho); aviso('Excluído.'); depois(); } catch (e) { aviso(e.message, 'erro'); }
    }

    await Promise.all([carregarTarifas(), carregarBandeiras()]);

    return h('div', null,
        h('header', { class: 'cabecalho' }, h('div', null, h('h1', null, 'Meus custos'),
            h('p', null, 'Valores que entram no preço de todas as receitas: seu trabalho, gás, energia e a divisão das contas do mês.'))),
        h('section', { class: 'painel' }, h('h2', null, 'Trabalho, gás e custos fixos'), form),
        h('div', { class: 'colunas bloco' }, blocoTarifas, blocoBandeiras));
}
