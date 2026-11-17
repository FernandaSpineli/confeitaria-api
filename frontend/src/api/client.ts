// API client: access token in memory, refresh token in localStorage, single-flight refresh on 401.

const BASE = '/api/v1';
const CHAVE_REFRESH = 'confeitaria.refresh';

export interface ItemFaltante {
  item: string;
  unidade: string;
  necessario: number;
  saldo: number;
  faltam: number;
}

export interface Problema {
  title?: string;
  detail?: string;
  erros?: Record<string, string>;
  itensFaltantes?: ItemFaltante[];
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: Problema;

  constructor(status: number, problem: Problema | null) {
    super(problem?.detail || problem?.title || `Erro ${status}`);
    this.status = status;
    this.problem = problem ?? {};
  }
}

interface Tokens {
  accessToken: string;
  refreshToken: string;
}

const armazenamento = {
  ler(): string | null {
    try { return localStorage.getItem(CHAVE_REFRESH); } catch { return null; }
  },
  gravar(v: string) {
    try { localStorage.setItem(CHAVE_REFRESH, v); } catch { /* private mode: session lasts until reload */ }
  },
  apagar() {
    try { localStorage.removeItem(CHAVE_REFRESH); } catch { /* ignore */ }
  },
};

let accessToken: string | null = null;
let refreshEmMemoria: string | null = null;
let renovando: Promise<void> | null = null;
let aoExpirar: () => void = () => {};

export function quandoSessaoExpirar(fn: () => void) { aoExpirar = fn; }
export const temSessaoSalva = () => Boolean(armazenamento.ler() || refreshEmMemoria);

async function bruto<T>(metodo: string, caminho: string, corpo?: unknown, token?: string | null): Promise<T> {
  const headers: Record<string, string> = {};
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  let res: Response;
  try {
    res = await fetch(BASE + caminho, {
      method: metodo, headers, body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    throw new ApiError(0, { detail: 'Sem conexão com o servidor. Verifique se a API está no ar.' });
  }
  if (res.status === 204) return null as T;
  const texto = await res.text();
  let dados: unknown = null;
  if (texto) {
    try { dados = JSON.parse(texto); } catch { dados = { detail: texto.slice(0, 200) }; }
  }
  if (!res.ok) throw new ApiError(res.status, dados as Problema);
  return dados as T;
}

function guardar(t: Tokens) {
  accessToken = t.accessToken;
  refreshEmMemoria = t.refreshToken;
  armazenamento.gravar(t.refreshToken);
}

function limpar() {
  accessToken = null;
  refreshEmMemoria = null;
  armazenamento.apagar();
}

function renovar(): Promise<void> {
  if (!renovando) {
    renovando = (async () => {
      const rt = armazenamento.ler() || refreshEmMemoria;
      if (!rt) throw new ApiError(401, { detail: 'Faça login para continuar.' });
      try {
        guardar(await bruto<Tokens>('POST', '/auth/refresh', { refreshToken: rt }));
      } catch (e) {
        if (e instanceof ApiError && e.status === 0) throw e;
        limpar();
        throw new ApiError(401, { detail: 'Sua sessão expirou. Entre novamente.' });
      }
    })().finally(() => { renovando = null; });
  }
  return renovando;
}

export async function api<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  try {
    if (!accessToken) await renovar();
    try {
      return await bruto<T>(metodo, caminho, corpo, accessToken);
    } catch (e) {
      if (!(e instanceof ApiError) || e.status !== 401) throw e;
      await renovar();
      return await bruto<T>(metodo, caminho, corpo, accessToken);
    }
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) aoExpirar();
    throw e;
  }
}

export const get = <T>(c: string) => api<T>('GET', c);
export const post = <T>(c: string, b?: unknown) => api<T>('POST', c, b ?? {});
export const put = <T>(c: string, b: unknown) => api<T>('PUT', c, b);
export const patch = <T>(c: string, b: unknown) => api<T>('PATCH', c, b);
export const del = (c: string) => api<null>('DELETE', c);

export async function entrar(email: string, senha: string) {
  guardar(await bruto<Tokens>('POST', '/auth/login', { email, senha }));
}

export const cadastrar = (nome: string, email: string, senha: string) =>
  bruto('POST', '/auth/register', { nome, email, senha });

export async function sair() {
  const rt = armazenamento.ler() || refreshEmMemoria;
  try {
    if (rt && accessToken) await bruto('POST', '/auth/logout', { refreshToken: rt }, accessToken);
  } catch { /* still leave locally */ }
  limpar();
}

interface Pagina<T> { content: T[]; totalPages: number }

/** Fetches every page of a list endpoint (catalogs are small; capped at 500 items). */
export async function todos<T>(caminho: string): Promise<T[]> {
  const sep = caminho.includes('?') ? '&' : '?';
  const itens: T[] = [];
  for (let pagina = 0; pagina < 5; pagina++) {
    const r = await get<Pagina<T>>(`${caminho}${sep}size=100&page=${pagina}`);
    itens.push(...r.content);
    if (pagina + 1 >= r.totalPages) break;
  }
  return itens;
}

/** Resolves to null on 404 (e.g. "no tariff yet"). */
export const talvez = <T>(p: Promise<T>): Promise<T | null> =>
  p.catch((e) => { if (e instanceof ApiError && e.status === 404) return null; throw e; });
