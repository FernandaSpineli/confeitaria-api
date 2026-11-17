import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, type ReactNode, useEffect, useState } from 'react';
import { del, get, post, put } from '../../api/client';
import type { Pagina } from '../../api/types';
import { useAviso } from '../../components/Toasts';
import {
  Cabecalho, Campo, Carregando, Confirmar, ErroCarregar, Modal, Paginacao, ProblemaAlerta, Vazio, erroDe, numero,
} from '../../components/ui';

type Valor = string | boolean;

export interface CampoDef {
  nome: string;
  rotulo: string;
  tipo?: 'texto' | 'numero' | 'selecao' | 'check';
  dica?: string;
  passo?: string;
  min?: number;
  max?: number;
  opcoes?: [string, string][];
  /** Fields sharing a `linha` are laid out side by side. */
  linha?: string;
}

export interface CadastroCfg<T extends { id: string }> {
  titulo: string;
  descricao: string;
  caminho: string;
  singular: string;
  /** "cadastrado" or "cadastrada" */
  feminino?: boolean;
  busca?: boolean;
  vazio: string;
  avisoExclusao?: string;
  colunas: { rotulo: string; valor: (item: T) => ReactNode; dir?: boolean }[];
  campos: CampoDef[];
  valores: (item?: T) => Record<string, Valor>;
  nomeDe: (item: T) => string;
  acoesExtras?: (item: T) => ReactNode;
  topo?: (itens: T[]) => ReactNode;
}

const cap = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);

/** Form values → request body: numbers for numeric fields, booleans for checkboxes, null for blanks. */
function paraRequest(campos: CampoDef[], valores: Record<string, Valor>) {
  const corpo: Record<string, unknown> = {};
  for (const c of campos) {
    const v = valores[c.nome];
    if (c.tipo === 'check') corpo[c.nome] = Boolean(v);
    else if (c.tipo === 'numero') corpo[c.nome] = numero(String(v ?? ''));
    else corpo[c.nome] = String(v ?? '').trim() === '' ? null : String(v).trim();
  }
  return corpo;
}

function Formulario<T extends { id: string }>({ cfg, item, onFechar }: {
  cfg: CadastroCfg<T>; item?: T; onFechar: () => void;
}) {
  const [valores, setValores] = useState<Record<string, Valor>>(() => cfg.valores(item));
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const alterar = (nome: string, v: Valor) => setValores((atual) => ({ ...atual, [nome]: v }));

  async function enviar(ev: FormEvent) {
    ev.preventDefault();
    setEnviando(true);
    try {
      const corpo = paraRequest(cfg.campos, valores);
      if (item) await put(`${cfg.caminho}/${item.id}`, corpo);
      else await post(cfg.caminho, corpo);
      aviso(item ? 'Alterações salvas.' : `${cap(cfg.singular)} ${cfg.feminino ? 'cadastrada' : 'cadastrado'}.`);
      await queryClient.invalidateQueries();
      onFechar();
    } catch (e) {
      setErro(e);
      setEnviando(false);
    }
  }

  const controle = (c: CampoDef) => {
    const v = valores[c.nome];
    if (c.tipo === 'check') {
      return (
        <label className="check" key={c.nome}>
          <input type="checkbox" name={c.nome} checked={Boolean(v)} onChange={(e) => alterar(c.nome, e.target.checked)} />
          {c.rotulo}
        </label>
      );
    }
    const entrada = c.tipo === 'selecao' ? (
      <select name={c.nome} value={String(v ?? '')} onChange={(e) => alterar(c.nome, e.target.value)}>
        {c.opcoes?.map(([o, r]) => <option key={o} value={o}>{r}</option>)}
      </select>
    ) : (
      <input name={c.nome} value={String(v ?? '')} onChange={(e) => alterar(c.nome, e.target.value)}
        type={c.tipo === 'numero' ? 'number' : 'text'} step={c.tipo === 'numero' ? c.passo ?? 'any' : undefined}
        min={c.min} max={c.max} inputMode={c.tipo === 'numero' ? 'decimal' : undefined}
        maxLength={c.tipo === 'numero' ? undefined : 120} aria-invalid={erroDe(erro, c.nome) ? true : undefined} />
    );
    return <Campo key={c.nome} rotulo={c.rotulo} dica={c.dica} erro={erroDe(erro, c.nome)}>{entrada}</Campo>;
  };

  // Group consecutive fields with the same `linha` into a grid row.
  const blocos: ReactNode[] = [];
  for (let i = 0; i < cfg.campos.length;) {
    const c = cfg.campos[i];
    if (!c.linha) { blocos.push(controle(c)); i++; continue; }
    const grupo = [];
    while (i < cfg.campos.length && cfg.campos[i].linha === c.linha) grupo.push(cfg.campos[i++]);
    blocos.push(<div className="grade" key={c.linha}>{grupo.map(controle)}</div>);
  }

  return (
    <Modal titulo={item ? `Editar ${cfg.singular}` : `${cfg.feminino ? 'Nova' : 'Novo'} ${cfg.singular}`} onFechar={onFechar}>
      <form className="campos" noValidate onSubmit={enviar}>
        <ProblemaAlerta erro={erro} camposNaTela={cfg.campos.map((c) => c.nome)} />
        {blocos}
        <div className="dialogo-rodape">
          <button type="button" onClick={onFechar}>Cancelar</button>
          <button type="submit" className="primario" disabled={enviando}>{item ? 'Salvar alterações' : `Cadastrar ${cfg.singular}`}</button>
        </div>
      </form>
    </Modal>
  );
}

function useAtraso<T>(valor: T, ms: number): T {
  const [atual, setAtual] = useState(valor);
  useEffect(() => {
    const t = setTimeout(() => setAtual(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);
  return atual;
}

/** Generic list + create/edit/delete screen for a catalog resource. */
export function Cadastro<T extends { id: string }>({ cfg }: { cfg: CadastroCfg<T> }) {
  const [pagina, setPagina] = useState(0);
  const [busca, setBusca] = useState('');
  const nome = useAtraso(busca.trim(), 250);
  const [editando, setEditando] = useState<T | 'novo' | null>(null);
  const [excluindo, setExcluindo] = useState<T | null>(null);
  const aviso = useAviso();
  const queryClient = useQueryClient();

  useEffect(() => { setPagina(0); }, [nome]);

  const q = useQuery({
    queryKey: ['cadastro', cfg.caminho, pagina, nome],
    queryFn: () => {
      const p = new URLSearchParams({ page: String(pagina), size: '20' });
      if (nome) p.set('nome', nome);
      return get<Pagina<T>>(`${cfg.caminho}?${p}`);
    },
    placeholderData: keepPreviousData,
  });

  async function excluir(item: T) {
    setExcluindo(null);
    try {
      await del(`${cfg.caminho}/${item.id}`);
      aviso(`${cfg.nomeDe(item)} excluído.`);
      await queryClient.invalidateQueries();
    } catch (e) {
      aviso((e as Error).message, 'erro');
    }
  }

  const botaoNovo = <button type="button" className="primario" onClick={() => setEditando('novo')}>Cadastrar {cfg.singular}</button>;

  let corpo: ReactNode;
  if (q.isPending) corpo = <Carregando />;
  else if (q.isError) corpo = <ErroCarregar erro={q.error} />;
  else if (!q.data.content.length && !nome) corpo = <div className="painel"><Vazio texto={cfg.vazio}>{botaoNovo}</Vazio></div>;
  else {
    corpo = (
      <div className="painel">
        {cfg.topo?.(q.data.content)}
        <div className="tabela-wrap">
          <table>
            <thead>
              <tr>
                {cfg.colunas.map((c) => <th key={c.rotulo} className={c.dir ? 'dir' : undefined}>{c.rotulo}</th>)}
                <th><span className="sr-only">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {q.data.content.length ? q.data.content.map((item) => (
                <tr key={item.id}>
                  {cfg.colunas.map((c) => <td key={c.rotulo} className={c.dir ? 'dir' : undefined}>{c.valor(item)}</td>)}
                  <td className="acoes-linha">
                    {cfg.acoesExtras?.(item)}
                    <button type="button" className="discreto" onClick={() => setEditando(item)}>Editar</button>
                    <button type="button" className="perigo" onClick={() => setExcluindo(item)}>Excluir</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={cfg.colunas.length + 1} className="vazio">Nada encontrado com esse nome.</td></tr>
              )}
            </tbody>
          </table>
        </div>
        <Paginacao pagina={q.data} irPara={setPagina} />
      </div>
    );
  }

  return (
    <div>
      <Cabecalho titulo={cfg.titulo} descricao={cfg.descricao}>
        {cfg.busca ? (
          <input type="search" className="busca" placeholder="Buscar pelo nome" aria-label="Buscar pelo nome"
            value={busca} onChange={(e) => setBusca(e.target.value)} />
        ) : null}
        {botaoNovo}
      </Cabecalho>
      {corpo}
      {editando ? <Formulario cfg={cfg} item={editando === 'novo' ? undefined : editando} onFechar={() => setEditando(null)} /> : null}
      {excluindo ? (
        <Confirmar titulo={`Excluir ${cfg.nomeDe(excluindo)}?`} texto={cfg.avisoExclusao ?? 'Esta ação não pode ser desfeita.'}
          onConfirmar={() => void excluir(excluindo)} onFechar={() => setExcluindo(null)} />
      ) : null}
    </div>
  );
}
