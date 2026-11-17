import { get, entrar, cadastrar, sair, temSessaoSalva, quandoSessaoExpirar } from './api.js';
import { h, trocar, campo, entrada, mostrarErro, ocupado, aviso } from './dom.js';
import { painel } from './views/painel.js';
import { listaReceitas, detalheReceita, editorReceita, fatia } from './views/receitas.js';
import { insumos, embalagens, equipamentos, custosFixos, canais } from './views/cadastros.js';
import { estoque, producoes } from './views/estoque.js';
import { configuracao } from './views/configuracao.js';
import { usuarias } from './views/admin.js';

const raiz = document.getElementById('app');
export const sessao = { usuario: null };

const ROTAS_CONFEITEIRA = [
    ['/', painel],
    ['/receitas', listaReceitas],
    ['/receitas/nova', editorReceita],
    ['/receitas/:id', detalheReceita],
    ['/receitas/:id/editar', editorReceita],
    ['/insumos', insumos],
    ['/embalagens', embalagens],
    ['/equipamentos', equipamentos],
    ['/custos-fixos', custosFixos],
    ['/canais', canais],
    ['/estoque', estoque],
    ['/producoes', producoes],
    ['/configuracao', configuracao],
];
const ROTAS_ADMIN = [['/', usuarias]];

const MENU_CONFEITEIRA = [
    ['/', 'Início'],
    ['/receitas', 'Receitas e preços'],
    ['/producoes', 'Produções'],
    ['/estoque', 'Estoque'],
    { grupo: 'Cadastros' },
    ['/insumos', 'Insumos'],
    ['/embalagens', 'Embalagens'],
    ['/equipamentos', 'Equipamentos'],
    ['/custos-fixos', 'Custos fixos'],
    ['/canais', 'Canais de venda'],
    { grupo: 'Ajustes' },
    ['/configuracao', 'Meus custos'],
];
const MENU_ADMIN = [['/', 'Usuárias']];

export const ir = (caminho) => { location.hash = `#${caminho}`; };
const caminhoAtual = () => location.hash.replace(/^#/, '') || '/';

function casar(rotas, caminho) {
    const partes = caminho.split('?')[0].split('/').filter(Boolean);
    for (const [modelo, view] of rotas) {
        const m = modelo.split('/').filter(Boolean);
        if (m.length !== partes.length) continue;
        const params = {};
        if (m.every((seg, i) => (seg.startsWith(':') ? (params[seg.slice(1)] = decodeURIComponent(partes[i])) : seg === partes[i]))) {
            return { view, params, modelo };
        }
    }
    return null;
}

let conteudo = null;
let shell = null;

function montarShell() {
    const admin = sessao.usuario.perfil === 'ADMIN';
    const menu = admin ? MENU_ADMIN : MENU_CONFEITEIRA;
    const alternar = () => shell.classList.toggle('menu-aberto');
    conteudo = h('main', { class: 'conteudo', id: 'conteudo', tabindex: '-1' });
    shell = h('div', { class: 'shell' },
        h('header', { class: 'topo-movel' },
            h('a', { class: 'marca', href: '#/' }, h('img', { src: '/assets/favicon.svg', alt: '' }), 'Confeitaria'),
            h('button', { type: 'button', 'aria-label': 'Abrir menu', onclick: alternar }, 'Menu')),
        h('nav', { class: 'lateral', 'aria-label': 'Principal' },
            h('a', { class: 'marca', href: '#/' }, h('img', { src: '/assets/favicon.svg', alt: '' }), 'Confeitaria'),
            h('ul', { class: 'menu' }, menu.map((item) => (item.grupo
                ? h('li', { class: 'grupo' }, item.grupo)
                : h('li', null, h('a', { href: `#${item[0]}`, 'data-rota': item[0], onclick: () => shell.classList.remove('menu-aberto') }, item[1]))))),
            h('div', { class: 'conta' },
                h('strong', null, sessao.usuario.nome),
                sessao.usuario.email,
                h('div', null, h('button', { type: 'button', onclick: async () => { await sair(); sessao.usuario = null; telaEntrada(); } }, 'Sair')))),
        conteudo);
    trocar(raiz, shell);
}

function marcarMenu(modelo) {
    const raizRota = '/' + (modelo.split('/')[1] || '');
    shell.querySelectorAll('.menu a').forEach((a) => {
        if (a.dataset.rota === raizRota) a.setAttribute('aria-current', 'page');
        else a.removeAttribute('aria-current');
    });
}

let navegacao = 0;
async function renderizar() {
    if (!sessao.usuario) return;
    const rotas = sessao.usuario.perfil === 'ADMIN' ? ROTAS_ADMIN : ROTAS_CONFEITEIRA;
    const achou = casar(rotas, caminhoAtual());
    if (!achou) { ir('/'); return; }
    const minha = ++navegacao;
    marcarMenu(achou.modelo);
    trocar(conteudo, h('p', { class: 'carregando' }, 'Carregando…'));
    try {
        const tela = await achou.view(achou.params);
        if (minha !== navegacao) return;
        trocar(conteudo, tela);
        const titulo = conteudo.querySelector('h1')?.textContent;
        document.title = titulo ? `${titulo} | Confeitaria` : 'Confeitaria';
        conteudo.focus({ preventScroll: true });
        window.scrollTo(0, 0);
    } catch (e) {
        if (minha !== navegacao || e.status === 401) return;
        trocar(conteudo, h('div', { class: 'alerta', role: 'alert' },
            h('p', null, h('strong', null, e.status === 404 ? 'Não encontramos este item.' : e.message)),
            h('p', null, h('a', { href: '#/' }, 'Voltar para o início'))));
    }
}

/** Re-renders the current route (used after saving something). */
export const recarregar = () => renderizar();

async function iniciarSessao() {
    sessao.usuario = await get('/me');
    montarShell();
    await renderizar();
}

function telaEntrada(mensagem) {
    let modo = 'entrar';
    const caixa = h('div', { class: 'caixa' });

    const desenhar = () => {
        const cadastro = modo === 'cadastrar';
        const form = h('form', { class: 'campos', novalidate: true },
            cadastro ? campo('Seu nome', entrada('nome', { autocomplete: 'name', required: true, maxlength: 120 })) : null,
            campo('E-mail', entrada('email', { tipo: 'email', autocomplete: 'email', required: true })),
            campo('Senha', entrada('senha', { tipo: 'password', autocomplete: cadastro ? 'new-password' : 'current-password', required: true }),
                cadastro ? 'Pelo menos 8 caracteres.' : null),
            h('button', { type: 'submit', class: 'primario' }, cadastro ? 'Criar conta' : 'Entrar'));
        form.addEventListener('submit', (ev) => {
            ev.preventDefault();
            const botao = form.querySelector('button[type=submit]');
            const f = Object.fromEntries(new FormData(form));
            ocupado(botao, async () => {
                try {
                    if (cadastro) await cadastrar(f.nome, f.email, f.senha);
                    await entrar(f.email, f.senha);
                    if (!location.hash) ir('/');
                    await iniciarSessao();
                    if (cadastro) aviso('Conta criada. Comece configurando seus custos.');
                } catch (e) {
                    mostrarErro(form, e.status === 401 ? Object.assign(e, { message: 'E-mail ou senha incorretos.' }) : e);
                }
            });
        });
        const aba = (id, rotulo) => h('button', {
            type: 'button', role: 'tab', 'aria-selected': String(modo === id),
            onclick: () => { modo = id; desenhar(); },
        }, rotulo);
        trocar(caixa,
            h('div', { class: 'abas', role: 'tablist' }, aba('entrar', 'Entrar'), aba('cadastrar', 'Criar conta')),
            mensagem && modo === 'entrar' ? h('div', { class: 'alerta aviso', role: 'status' }, h('p', null, mensagem)) : null,
            form);
        form.querySelector('input')?.focus();
    };

    trocar(raiz, h('div', { class: 'entrada' },
        h('section', { class: 'entrada-arte' },
            h('h1', null, 'Quanto custa cada fatia?'),
            h('p', null, 'Some ingredientes, embalagem, seu tempo, energia e contas do mês. Veja o preço certo para o balcão e para cada aplicativo, e acompanhe o estoque a cada produção.'),
            fatiaArte()),
        h('section', { class: 'entrada-form' }, caixa)));
    desenhar();
}

function fatiaArte() {
    // A real example: one batch of 25 brigadeiros (see README).
    const exemplo = { insumos: 8.8, embalagens: 1.5, maoDeObra: 25, energia: 0.1, gas: 0.5, custosFixos: 3.75, total: 39.65 };
    return h('figure', { class: 'fatia-arte' },
        fatia(exemplo, 25),
        h('figcaption', { class: 'exemplo' }, 'Exemplo: 25 brigadeiros custam R$ 1,59 cada e saem a R$ 2,64 com 40% de margem.'));
}

quandoSessaoExpirar(() => {
    if (!sessao.usuario) return;
    sessao.usuario = null;
    telaEntrada('Sua sessão expirou. Entre novamente.');
});

window.addEventListener('hashchange', renderizar);

(async () => {
    if (temSessaoSalva()) {
        try { await iniciarSessao(); return; } catch { /* fall through to login */ }
    }
    telaEntrada();
})();
