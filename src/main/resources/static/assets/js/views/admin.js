import { get, patch } from '../api.js';
import { h, trocar, aviso, ocupado, dataHora } from '../dom.js';
import { sessao } from '../app.js';
import { paginacao } from './cadastros.js';

export async function usuarias() {
    const estado = { pagina: 0, email: '' };
    const corpo = h('section', { class: 'painel' });

    async function carregar() {
        const q = new URLSearchParams({ page: estado.pagina, size: 20 });
        if (estado.email) q.set('email', estado.email);
        const r = await get(`/admin/usuarios?${q}`);
        trocar(corpo,
            h('div', { class: 'tabela-wrap' }, h('table', null,
                h('thead', null, h('tr', null, ['Nome', 'E-mail', 'Perfil', 'Criada em', 'Situação', ''].map((t) => h('th', null, t)))),
                h('tbody', null, r.content.map((u) => {
                    const eu = u.id === sessao.usuario.id;
                    return h('tr', null,
                        h('td', null, u.nome),
                        h('td', null, u.email),
                        h('td', null, h('span', { class: 'selo' }, u.perfil === 'ADMIN' ? 'admin' : 'confeiteira')),
                        h('td', { class: 'num' }, dataHora(u.criadoEm)),
                        h('td', null, u.ativo ? h('span', { class: 'selo ok' }, 'ativa') : h('span', { class: 'selo perigo' }, 'desativada')),
                        h('td', { class: 'acoes-linha' }, eu ? h('span', { class: 'small muted' }, 'você') : h('button', {
                            type: 'button', class: u.ativo ? 'perigo' : 'discreto',
                            onclick: (ev) => ocupado(ev.currentTarget, async () => {
                                try {
                                    await patch(`/admin/usuarios/${u.id}`, { ativo: !u.ativo });
                                    aviso(u.ativo ? `${u.nome} desativada. As sessões dela foram encerradas.` : `${u.nome} reativada.`);
                                    carregar();
                                } catch (e) { aviso(e.message, 'erro'); }
                            }),
                        }, u.ativo ? 'Desativar' : 'Reativar')));
                })))),
            paginacao(r, (p) => { estado.pagina = p; carregar(); }));
    }
    await carregar();

    let t;
    return h('div', null,
        h('header', { class: 'cabecalho' },
            h('div', null, h('h1', null, 'Usuárias'), h('p', null, 'Contas da plataforma. A administração não vê receitas, preços nem estoque das confeiteiras.')),
            h('input', { type: 'search', class: 'busca', placeholder: 'Buscar por e-mail', 'aria-label': 'Buscar por e-mail',
                oninput: (ev) => { clearTimeout(t); t = setTimeout(() => { estado.email = ev.target.value.trim(); estado.pagina = 0; carregar(); }, 250); } })),
        corpo);
}
