import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { ApiError, del, get, post } from '../../api/client';
import type { Capacidade, Preco, Producao, Receita, Tamanho } from '../../api/types';
import { useAviso } from '../../components/Toasts';
import { Cabecalho, Campo, Carregando, Confirmar, ErroCarregar, ProblemaAlerta, erroDe, numero } from '../../components/ui';
import { brl, fracaoReceita, minutos, num, plural, unidade } from '../../lib/format';
import { PrecoPainel, caminhoPreco } from './PrecoPainel';

export function DetalheReceita() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const [confirmando, setConfirmando] = useState(false);
  const [tamanhoId, setTamanhoId] = useState<string | null>(null);
  const q = useQuery({ queryKey: ['receita', id], queryFn: () => get<Receita>(`/receitas/${id}`) });

  if (q.isPending) return <Carregando />;
  if (q.isError) return <ErroCarregar erro={q.error} />;
  const receita = q.data;
  const tamanho = receita.tamanhos.find((t) => t.id === tamanhoId) ?? receita.tamanhos[0] ?? null;

  async function excluir() {
    setConfirmando(false);
    try {
      await del(`/receitas/${id}`);
      aviso('Receita excluída.');
      await queryClient.invalidateQueries();
      navigate('/receitas');
    } catch (e) {
      aviso((e as Error).message, 'erro');
    }
  }

  const descricao = receita.tamanhos.length
    ? `Vendida em ${plural(receita.tamanhos.length, 'tamanho', 'tamanhos')}. A receita inteira leva ${minutos(receita.tempoPreparoMin)} de trabalho${receita.minutosGas ? ` e ${minutos(receita.minutosGas)} de forno a gás` : ''}.`
    : `Rende ${receita.rendimento} un, ${minutos(receita.tempoPreparoMin)} de trabalho${receita.minutosGas ? `, ${minutos(receita.minutosGas)} de forno a gás` : ''}.`;

  return (
    <div>
      <a className="voltar" href="#/receitas">Todas as receitas</a>
      <Cabecalho titulo={receita.nome} descricao={descricao}>
        <a className="botao" href={`#/receitas/${id}/editar`}>Editar receita</a>
        <button type="button" className="perigo" onClick={() => setConfirmando(true)}>Excluir</button>
      </Cabecalho>

      {receita.tamanhos.length ? (
        <SeletorTamanhos receitaId={id} tamanhos={receita.tamanhos} atual={tamanho} onEscolher={setTamanhoId} />
      ) : null}
      <PrecoPainel key={tamanho?.id ?? 'inteira'} receitaId={id} tamanho={tamanho} />

      <div className="colunas bloco">
        <ProducaoPainel key={tamanho?.id ?? 'inteira'} receita={receita} tamanhoInicial={tamanho} />
        <section className="painel">
          <h2>O que vai na receita inteira</h2>
          <ul className="lista-simples">
            {receita.insumos.map((i) => (
              <li key={`i${i.insumoId}${i.base}`}>{i.nome}<span className="num muted">{num(i.quantidade, 3)} {unidade(i.unidade)}{i.base === 'POR_UNIDADE' ? ' por unidade' : ''}</span></li>
            ))}
            {receita.embalagens.map((e) => (
              <li key={`e${e.embalagemId}${e.base}`}>{e.nome}<span className="num muted">{num(e.quantidade, 3)} un {e.base === 'POR_UNIDADE' ? 'por unidade' : 'no lote'}</span></li>
            ))}
            {receita.equipamentos.map((eq) => (
              <li key={`q${eq.equipamentoId}`}>{eq.nome}<span className="num muted">{eq.minutosUso} min</span></li>
            ))}
          </ul>
        </section>
      </div>

      {confirmando ? (
        <Confirmar titulo={`Excluir ${receita.nome}?`} texto="A receita sai da lista. Produções já registradas continuam no histórico."
          onConfirmar={() => void excluir()} onFechar={() => setConfirmando(false)} />
      ) : null}
    </div>
  );
}

/** One card per size, each with its own price, so the sizes can be compared at a glance. */
function SeletorTamanhos({ receitaId, tamanhos, atual, onEscolher }: {
  receitaId: string; tamanhos: Tamanho[]; atual: Tamanho | null; onEscolher: (id: string) => void;
}) {
  const precos = useQueries({
    queries: tamanhos.map((t) => ({ queryKey: ['preco', receitaId, t.id], queryFn: () => get<Preco>(caminhoPreco(receitaId, t)) })),
  });
  const maior = Math.max(...tamanhos.map((t) => Number(t.fracaoReceita)));
  return (
    <div className="tamanhos" role="tablist" aria-label="Tamanhos">
      {tamanhos.map((t, i) => {
        const preco = precos[i].data;
        const selecionado = atual?.id === t.id;
        return (
          <button key={t.id} type="button" role="tab" aria-selected={selecionado} className="tamanho" onClick={() => onEscolher(t.id)}>
            <span className="tamanho-nome">{t.nome}</span>
            <span className="tamanho-medida" aria-hidden="true">
              <span style={{ width: `${(Number(t.fracaoReceita) / maior) * 100}%` }} />
            </span>
            <span className="small muted">{fracaoReceita(t.fracaoReceita)}, {plural(t.rendimento, 'peça', 'peças')}</span>
            <span className="tamanho-preco num">
              {preco ? brl(preco.precoBase.preco) : precos[i].isError ? 'sem preço' : '…'}
              {preco && t.rendimento > 1 ? <span className="small"> cada</span> : null}
            </span>
          </button>
        );
      })}
    </div>
  );
}

/** Stock-aware production: shows how many batches the current stock allows and records one. */
function ProducaoPainel({ receita, tamanhoInicial }: { receita: Receita; tamanhoInicial: Tamanho | null }) {
  const [escolha, setEscolha] = useState<string>(tamanhoInicial?.id ?? '');
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const tamanho = receita.tamanhos.find((t) => t.id === escolha) ?? null;
  const cap = useQuery({
    queryKey: ['capacidade', receita.id, tamanho?.id ?? null],
    queryFn: () => get<Capacidade>(`/receitas/${receita.id}/capacidade-producao${tamanho ? `?tamanhoId=${tamanho.id}` : ''}`),
  });

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const f = Object.fromEntries(new FormData(ev.currentTarget)) as Record<string, string>;
    setEnviando(true);
    setErro(null);
    try {
      const r = await post<Producao>('/producoes', { receitaId: receita.id, lotes: numero(f.lotes), tamanhoId: tamanho?.id ?? null });
      aviso(`Produção registrada: ${r.unidadesProduzidas} un${r.tamanho ? ` (${r.tamanho})` : ''}, ${r.baixas.length} itens baixados do estoque.`);
      await queryClient.invalidateQueries();
    } catch (e) {
      setErro(e);
    } finally {
      setEnviando(false);
    }
  }

  const c = cap.data;
  const fl = c?.fatorLimitante;
  return (
    <section className="painel">
      <h2>Produzir</h2>
      {cap.isError && !(cap.error instanceof ApiError && cap.error.status === 422) ? <ErroCarregar erro={cap.error} /> : null}
      {cap.isError && cap.error instanceof ApiError && cap.error.status === 422 ? <p className="muted">{cap.error.message}</p> : null}
      {c ? (
        <p>
          Com o estoque de hoje dá para fazer{' '}
          {tamanho
            ? <><strong>{tamanho.nome} {plural(c.lotesPossiveis, 'vez', 'vezes')}</strong> ({plural(c.unidadesPossiveis, 'peça', 'peças')}).</>
            : <><strong className="num">{plural(c.lotesPossiveis, 'lote', 'lotes')}</strong> ({num(c.unidadesPossiveis, 0)} un).</>}
          {fl ? <> Quem limita é <strong>{fl.item}</strong>: tem {num(fl.saldo, 3)} {unidade(fl.unidade)} e cada {tamanho ? 'vez' : 'lote'} usa {num(fl.necessarioPorLote, 3)}.</> : null}
        </p>
      ) : null}
      <form className="campos" noValidate onSubmit={enviar}>
        <ProblemaAlerta erro={erro} camposNaTela={['lotes']} />
        <div className="filtros">
          {receita.tamanhos.length ? (
            <Campo rotulo="O que produzir">
              <select value={escolha} onChange={(e) => { setEscolha(e.target.value); setErro(null); }}>
                {receita.tamanhos.map((t) => <option key={t.id} value={t.id}>{t.nome}</option>)}
                <option value="">Receita inteira, sem embalagem</option>
              </select>
            </Campo>
          ) : null}
          <Campo rotulo={tamanho ? 'Quantas vezes' : 'Lotes'} erro={erroDe(erro, 'lotes')}
            dica={tamanho ? `Cada vez = ${fracaoReceita(tamanho.fracaoReceita)}, ${plural(tamanho.rendimento, 'peça', 'peças')}.` : undefined}>
            <input name="lotes" type="number" min={1} max={1000} step="1" defaultValue={1} inputMode="numeric" />
          </Campo>
          <button type="submit" className="primario" disabled={enviando}>Registrar produção</button>
        </div>
      </form>
      <p className="small muted">
        A produção baixa todos os itens de uma vez. Se faltar algum, nada é baixado e a lista do que falta aparece aqui.{' '}
        <a href="#/estoque">Registrar compras</a>
      </p>
    </section>
  );
}
