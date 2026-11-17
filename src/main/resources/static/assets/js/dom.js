// Tiny DOM helpers. Text is always set as text (never innerHTML), so API data cannot inject markup.

const PROPRIEDADES = new Set(['value', 'checked', 'disabled', 'selected', 'hidden', 'open']);

export function h(tag, attrs, ...filhos) {
    const el = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs || {})) {
        if (v == null || v === false) continue;
        if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2).toLowerCase(), v);
        else if (k === 'class') el.className = v;
        else if (k === 'style') Object.assign(el.style, v);
        else if (PROPRIEDADES.has(k)) el[k] = v;
        else el.setAttribute(k, v === true ? '' : String(v));
    }
    anexar(el, filhos);
    return el;
}

function anexar(el, filhos) {
    for (const f of filhos.flat(Infinity)) {
        if (f == null || f === false) continue;
        el.append(f instanceof Node ? f : document.createTextNode(String(f)));
    }
}

export function trocar(el, ...filhos) {
    el.replaceChildren();
    anexar(el, filhos);
    return el;
}

// ---------- formatting ----------
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const brl = (v) => (v == null ? '—' : moeda.format(Number(v)));
export function num(v, max = 2) {
    if (v == null) return '—';
    return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: max }).format(Number(v));
}
export function custoUnitario(v) {
    if (v == null) return '—';
    const n = Number(v);
    const casas = n !== 0 && Math.abs(n) < 0.1 ? 4 : 2;
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: casas, maximumFractionDigits: casas }).format(n);
}
export const pct = (v) => (v == null ? '—' : `${num(v, 2)}%`);
export function dataHora(iso) {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso));
}
export function data(isoDate) {
    if (!isoDate) return '—';
    const [a, m, d] = isoDate.split('-');
    return `${d}/${m}/${a}`;
}
export const unidade = (u) => (u ? String(u).toLowerCase() : '');
export const hoje = () => new Date().toLocaleDateString('sv-SE');

// ---------- forms ----------
export function campo(rotulo, controle, dica) {
    return h('label', { class: 'campo' }, rotulo, dica ? h('span', { class: 'dica' }, dica) : null, controle,
        h('span', { class: 'erro-campo', 'data-erro-de': controle.name || '' }));
}

export function entrada(name, opts = {}) {
    const { tipo = 'text', valor, ...resto } = opts;
    const attrs = { name, type: tipo, ...resto };
    if (tipo === 'number') { attrs.step = resto.step ?? 'any'; attrs.inputmode = 'decimal'; }
    const el = h('input', attrs);
    if (valor != null) el.value = valor;
    return el;
}

export function selecao(name, opcoes, valor, attrs = {}) {
    const el = h('select', { name, ...attrs },
        opcoes.map(([v, rotulo]) => h('option', { value: v }, rotulo)));
    if (valor != null) el.value = valor;
    return el;
}

/** Reads a form into an object: numbers for type=number, booleans for checkboxes, null for empty. */
export function lerForm(form) {
    const dados = {};
    for (const el of form.elements) {
        if (!el.name || el.disabled) continue;
        if (el.type === 'checkbox') dados[el.name] = el.checked;
        else if (el.type === 'number') dados[el.name] = el.value === '' ? null : Number(el.value);
        else dados[el.name] = el.value === '' ? null : el.value;
    }
    return dados;
}

export function limparErros(raiz) {
    raiz.querySelectorAll('.erro-campo').forEach((e) => (e.textContent = ''));
    raiz.querySelectorAll('[aria-invalid]').forEach((e) => e.removeAttribute('aria-invalid'));
    raiz.querySelectorAll('.alerta.form').forEach((e) => e.remove());
}

/** Shows a ProblemDetail inside a form: field errors next to the fields, the rest on top. */
export function mostrarErro(raiz, erro) {
    limparErros(raiz);
    const problema = erro.problem || {};
    const restantes = [];
    for (const [nome, msg] of Object.entries(problema.erros || {})) {
        const alvo = raiz.querySelector(`[data-erro-de="${CSS.escape(nome)}"]`);
        if (alvo) {
            alvo.textContent = msg;
            raiz.querySelector(`[name="${CSS.escape(nome)}"]`)?.setAttribute('aria-invalid', 'true');
        } else restantes.push(`${nome}: ${msg}`);
    }
    if (!problema.erros || restantes.length) {
        raiz.prepend(h('div', { class: 'alerta form', role: 'alert' }, blocoProblema(erro, restantes)));
    }
}

export function blocoProblema(erro, extras = []) {
    const p = erro.problem || {};
    const linhas = [...extras];
    for (const f of p.itensFaltantes || []) {
        linhas.push(`${f.item}: precisa de ${num(f.necessario, 3)} ${unidade(f.unidade)}, tem ${num(f.saldo, 3)} (faltam ${num(f.faltam, 3)})`);
    }
    return [h('p', null, h('strong', null, erro.message)), linhas.length ? h('ul', null, linhas.map((l) => h('li', null, l))) : null];
}

// ---------- feedback ----------
export function aviso(texto, tipo = 'ok') {
    const t = h('div', { class: `toast ${tipo === 'erro' ? 'erro' : ''}` }, texto);
    const pilha = document.getElementById('toasts');
    pilha.append(t);
    while (pilha.children.length > 3) pilha.firstChild.remove();
    setTimeout(() => t.remove(), tipo === 'erro' ? 7000 : 4000);
}

/** Opens a modal dialog. `montar(fechar)` returns the body; resolves with whatever is passed to fechar. */
export function dialogo(montar) {
    return new Promise((resolve) => {
        const d = h('dialog');
        const fechar = (valor) => { d.close(); d.remove(); resolve(valor); };
        d.addEventListener('cancel', (e) => { e.preventDefault(); fechar(undefined); });
        d.append(h('div', { class: 'dialogo-corpo' }, montar(fechar)));
        document.body.append(d);
        d.showModal();
        d.querySelector('input, select, textarea')?.focus();
    });
}

export function confirmar(titulo, texto, rotuloAcao = 'Excluir') {
    return dialogo((fechar) => [
        h('h2', null, titulo),
        h('p', null, texto),
        h('div', { class: 'dialogo-rodape' },
            h('button', { type: 'button', onclick: () => fechar(false) }, 'Cancelar'),
            h('button', { type: 'button', class: 'primario', onclick: () => fechar(true) }, rotuloAcao)),
    ]);
}

/** Disables a button while the promise runs. */
export async function ocupado(botao, fn) {
    botao.disabled = true;
    try { return await fn(); } finally { botao.disabled = false; }
}

export function vazio(texto, acao) {
    return h('div', { class: 'vazio' }, h('p', null, texto), acao || null);
}
