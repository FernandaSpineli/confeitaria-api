import { get } from '../api.js';
import { h, brl, num, unidade, dataHora } from '../dom.js';
import { sessao } from '../app.js';

const talvez = (p) => p.catch((e) => { if (e.status === 404) return null; throw e; });

export async function painel() {
    const [config, tarifa, insumos, receitas, minimo, producoes] = await Promise.all([
        get('/configuracao'),
        talvez(get('/configuracao/tarifas/vigente')),
        get('/insumos?size=1'),
        get('/receitas?size=6'),
        get('/estoque/abaixo-minimo'),
        get('/producoes?size=5'),
    ]);

    const passos = [
        [Number(config.salarioDesejadoMes) > 0, '#/configuracao', 'Diga quanto quer ganhar por mês e quantas horas trabalha', 'Isso define o valor da sua hora.'],
        [Boolean(tarifa), '#/configuracao', 'Cadastre a tarifa de energia da sua conta de luz', 'Necessária se a receita usa equipamento elétrico.'],
        [insumos.totalElements > 0, '#/insumos', 'Cadastre os insumos com o preço que você paga', null],
        [receitas.totalElements > 0, '#/receitas/nova', 'Monte sua primeira receita e veja o preço', null],
    ];
    const pendentes = passos.filter(([feito]) => !feito).length;
    const primeiroNome = sessao.usuario.nome.split(' ')[0];

    return h('div', null,
        h('header', { class: 'cabecalho' }, h('div', null,
            h('h1', null, `Olá, ${primeiroNome}`),
            h('p', null, `Sua hora vale ${brl(config.valorHora)}. Bandeira de energia vigente: ${config.bandeiraVigente.replace('_', ' ').toLowerCase()}.`))),

        pendentes ? h('section', { class: 'painel' },
            h('h2', null, pendentes === passos.length ? 'Comece por aqui' : `Faltam ${pendentes} ${pendentes === 1 ? 'passo' : 'passos'} para o primeiro preço`),
            h('ol', { class: 'passos' }, passos.map(([feito, href, texto, dica]) => h('li', { class: feito ? 'feito' : null },
                h('div', null, h('a', { href }, texto), dica ? h('div', { class: 'small muted' }, dica) : null))))) : null,

        h('div', { class: 'colunas bloco' },
            h('section', { class: 'painel' },
                h('h2', null, 'Receitas'),
                receitas.content.length
                    ? h('ul', { class: 'lista-simples' }, receitas.content.map((r) => h('li', null,
                        h('a', { href: `#/receitas/${r.id}` }, r.nome), h('span', { class: 'muted small num' }, `rende ${r.rendimento}`))))
                    : h('p', { class: 'muted' }, 'Nenhuma receita ainda.'),
                h('p', { class: 'bloco' }, h('a', { class: 'botao', href: '#/receitas' }, 'Ver todas'))),
            h('section', { class: 'painel' },
                h('h2', null, 'Comprar logo'),
                minimo.length
                    ? h('ul', { class: 'lista-simples' }, minimo.slice(0, 6).map((i) => h('li', null,
                        h('span', null, i.nome),
                        h('span', { class: 'num' }, `${num(i.saldo, 3)} ${unidade(i.unidade)} `, h('span', { class: 'selo atencao' }, `mín. ${num(i.estoqueMinimo, 3)}`)))))
                    : h('p', { class: 'muted' }, 'Nenhum item abaixo do estoque mínimo.'),
                h('p', { class: 'bloco' }, h('a', { class: 'botao', href: '#/estoque' }, 'Abrir estoque'))),
            h('section', { class: 'painel' },
                h('h2', null, 'Últimas produções'),
                producoes.content.length
                    ? h('ul', { class: 'lista-simples' }, producoes.content.map((p) => h('li', null,
                        h('span', null, p.receita, h('div', { class: 'small muted' }, dataHora(p.criadoEm))),
                        h('span', { class: 'num' }, `${p.unidadesProduzidas} un`))))
                    : h('p', { class: 'muted' }, 'Nenhuma produção registrada.'),
                h('p', { class: 'bloco' }, h('a', { class: 'botao', href: '#/producoes' }, 'Ver produções')))));
}
