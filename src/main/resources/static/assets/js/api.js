// API client: access token in memory, refresh token in localStorage, single-flight refresh on 401.

const BASE = '/api/v1';
const CHAVE_REFRESH = 'confeitaria.refresh';

let accessToken = null;
let renovando = null;
let aoExpirar = () => {};

export class ApiError extends Error {
    constructor(status, problem) {
        super(problem?.detail || problem?.title || `Erro ${status}`);
        this.status = status;
        this.problem = problem || {};
    }
}

const armazenamento = {
    ler() { try { return localStorage.getItem(CHAVE_REFRESH); } catch { return null; } },
    gravar(v) { try { localStorage.setItem(CHAVE_REFRESH, v); } catch { /* private mode: session lasts until reload */ } },
    apagar() { try { localStorage.removeItem(CHAVE_REFRESH); } catch { /* ignore */ } },
};
let refreshEmMemoria = null;

export function quandoSessaoExpirar(fn) { aoExpirar = fn; }
export const temSessaoSalva = () => Boolean(armazenamento.ler() || refreshEmMemoria);

async function bruto(metodo, caminho, corpo, token) {
    const headers = {};
    if (corpo !== undefined) headers['Content-Type'] = 'application/json';
    if (token) headers.Authorization = `Bearer ${token}`;
    let res;
    try {
        res = await fetch(BASE + caminho, { method: metodo, headers, body: corpo !== undefined ? JSON.stringify(corpo) : undefined });
    } catch {
        throw new ApiError(0, { detail: 'Sem conexão com o servidor. Verifique se a API está no ar.' });
    }
    if (res.status === 204) return null;
    const texto = await res.text();
    let dados = null;
    if (texto) { try { dados = JSON.parse(texto); } catch { dados = { detail: texto.slice(0, 200) }; } }
    if (!res.ok) throw new ApiError(res.status, dados);
    return dados;
}

function guardar(tokens) {
    accessToken = tokens.accessToken;
    refreshEmMemoria = tokens.refreshToken;
    armazenamento.gravar(tokens.refreshToken);
}

function limpar() {
    accessToken = null;
    refreshEmMemoria = null;
    armazenamento.apagar();
}

function renovar() {
    if (!renovando) {
        renovando = (async () => {
            const rt = armazenamento.ler() || refreshEmMemoria;
            if (!rt) throw new ApiError(401, { detail: 'Faça login para continuar.' });
            try {
                guardar(await bruto('POST', '/auth/refresh', { refreshToken: rt }));
            } catch (e) {
                if (e.status === 0) throw e;
                limpar();
                throw new ApiError(401, { detail: 'Sua sessão expirou. Entre novamente.' });
            }
        })().finally(() => { renovando = null; });
    }
    return renovando;
}

export async function api(metodo, caminho, corpo) {
    try {
        if (!accessToken) await renovar();
        try {
            return await bruto(metodo, caminho, corpo, accessToken);
        } catch (e) {
            if (e.status !== 401) throw e;
            await renovar();
            return await bruto(metodo, caminho, corpo, accessToken);
        }
    } catch (e) {
        if (e.status === 401) aoExpirar(e);
        throw e;
    }
}

export const get = (c) => api('GET', c);
export const post = (c, b) => api('POST', c, b ?? {});
export const put = (c, b) => api('PUT', c, b);
export const patch = (c, b) => api('PATCH', c, b);
export const del = (c) => api('DELETE', c);

export async function entrar(email, senha) {
    guardar(await bruto('POST', '/auth/login', { email, senha }));
}

export const cadastrar = (nome, email, senha) => bruto('POST', '/auth/register', { nome, email, senha });

export async function sair() {
    const rt = armazenamento.ler() || refreshEmMemoria;
    try { if (rt && accessToken) await bruto('POST', '/auth/logout', { refreshToken: rt }, accessToken); } catch { /* still leave locally */ }
    limpar();
}

/** Fetches every page of a list endpoint (catalogs are small; capped at 500 items). */
export async function todos(caminho) {
    const sep = caminho.includes('?') ? '&' : '?';
    const itens = [];
    for (let pagina = 0; pagina < 5; pagina++) {
        const r = await get(`${caminho}${sep}size=100&page=${pagina}`);
        itens.push(...r.content);
        if (pagina + 1 >= r.totalPages) break;
    }
    return itens;
}
