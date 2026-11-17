import { type ReactNode, useEffect, useRef } from 'react';
import { ApiError } from '../api/client';
import type { Pagina } from '../api/types';
import { num, unidade } from '../lib/format';

/** A labelled form control with optional hint and the API's field error. */
export function Campo({ rotulo, dica, erro, children }: {
  rotulo: string; dica?: ReactNode; erro?: string; children: ReactNode;
}) {
  return (
    <label className="campo">
      {rotulo}
      {dica ? <span className="dica">{dica}</span> : null}
      {children}
      {erro ? <span className="erro-campo" role="alert">{erro}</span> : null}
    </label>
  );
}

/** Field error from a ProblemDetail (400 validation), if any. */
export const erroDe = (erro: unknown, campo: string): string | undefined =>
  erro instanceof ApiError ? erro.problem.erros?.[campo] : undefined;

/**
 * The error box shown above a form: the message, missing stock items, and validation errors that do not
 * belong to any field on screen.
 */
export function ProblemaAlerta({ erro, camposNaTela = [], tom = 'erro', children }: {
  erro: unknown; camposNaTela?: string[]; tom?: 'erro' | 'aviso'; children?: ReactNode;
}) {
  if (!erro) return null;
  const e = erro instanceof ApiError ? erro : new ApiError(0, { detail: String((erro as Error)?.message ?? erro) });
  const soltos = Object.entries(e.problem.erros ?? {}).filter(([k]) => !camposNaTela.includes(k));
  if (e.problem.erros && !soltos.length) return null;
  const linhas = [
    ...soltos.map(([k, v]) => `${k}: ${v}`),
    ...(e.problem.itensFaltantes ?? []).map((f) =>
      `${f.item}: precisa de ${num(f.necessario, 3)} ${unidade(f.unidade)}, tem ${num(f.saldo, 3)} (faltam ${num(f.faltam, 3)})`),
  ];
  return (
    <div className={`alerta${tom === 'aviso' ? ' aviso' : ''}`} role="alert">
      <p><strong>{e.message}</strong></p>
      {linhas.length ? <ul>{linhas.map((l) => <li key={l}>{l}</li>)}</ul> : null}
      {children}
    </div>
  );
}

export function Vazio({ texto, children }: { texto: string; children?: ReactNode }) {
  return <div className="vazio"><p>{texto}</p>{children}</div>;
}

export function Carregando() {
  return <p className="carregando">Carregando…</p>;
}

export function ErroCarregar({ erro }: { erro: unknown }) {
  const e = erro as ApiError;
  return (
    <div className="alerta" role="alert">
      <p><strong>{e?.status === 404 ? 'Não encontramos este item.' : e?.message ?? 'Não foi possível carregar.'}</strong></p>
      <p><a href="#/">Voltar para o início</a></p>
    </div>
  );
}

export function Paginacao<T>({ pagina, irPara }: { pagina: Pagina<T>; irPara: (p: number) => void }) {
  if (pagina.totalPages <= 1) return null;
  return (
    <div className="paginacao">
      <button type="button" disabled={pagina.page === 0} onClick={() => irPara(pagina.page - 1)}>Anterior</button>
      <span>Página {pagina.page + 1} de {pagina.totalPages} ({pagina.totalElements} itens)</span>
      <button type="button" disabled={pagina.page + 1 >= pagina.totalPages} onClick={() => irPara(pagina.page + 1)}>Próxima</button>
    </div>
  );
}

export function Cabecalho({ titulo, descricao, children }: { titulo: string; descricao?: ReactNode; children?: ReactNode }) {
  useEffect(() => { document.title = `${titulo} | Confeitaria`; }, [titulo]);
  return (
    <header className="cabecalho">
      <div>
        <h1>{titulo}</h1>
        {descricao ? <p>{descricao}</p> : null}
      </div>
      {children ? <div className="acoes">{children}</div> : null}
    </header>
  );
}

/** Native <dialog> opened as a modal; Esc and "Cancelar" both call onFechar. */
export function Modal({ titulo, onFechar, children }: { titulo: string; onFechar: () => void; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current;
    if (d && !d.open) d.showModal?.();
    d?.querySelector<HTMLElement>('input, select, textarea')?.focus();
    return () => { if (d?.open) d.close(); };
  }, []);
  return (
    <dialog ref={ref} aria-label={titulo} onCancel={(e) => { e.preventDefault(); onFechar(); }}>
      <div className="dialogo-corpo">
        <h2>{titulo}</h2>
        {children}
      </div>
    </dialog>
  );
}

export function Confirmar({ titulo, texto, acao = 'Excluir', onConfirmar, onFechar }: {
  titulo: string; texto: string; acao?: string; onConfirmar: () => void; onFechar: () => void;
}) {
  return (
    <Modal titulo={titulo} onFechar={onFechar}>
      <p>{texto}</p>
      <div className="dialogo-rodape">
        <button type="button" onClick={onFechar}>Cancelar</button>
        <button type="button" className="primario" onClick={onConfirmar}>{acao}</button>
      </div>
    </Modal>
  );
}

/** Reads a numeric input: '' → null. */
export const numero = (v: string): number | null => (v.trim() === '' ? null : Number(v));
