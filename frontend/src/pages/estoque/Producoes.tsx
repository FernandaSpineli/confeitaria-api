import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { get } from '../../api/client';
import type { Pagina, Producao } from '../../api/types';
import { Cabecalho, Carregando, ErroCarregar, Paginacao, Vazio } from '../../components/ui';
import { brl, dataHora, num, plural, unidade } from '../../lib/format';

/** Item list of one production, fetched when the row is opened. */
function Baixas({ id }: { id: string }) {
  const q = useQuery({ queryKey: ['producao', id], queryFn: () => get<Producao>(`/producoes/${id}`) });
  if (q.isPending) return <Carregando />;
  if (q.isError) return <ErroCarregar erro={q.error} />;
  return (
    <div className="tabela-wrap">
      <table>
        <thead><tr><th>Item baixado</th><th className="dir">Quantidade</th><th className="dir">Saldo atual</th></tr></thead>
        <tbody>
          {q.data.baixas.map((b) => (
            <tr key={`${b.tipo}${b.itemId}`}>
              <td>{b.item}</td>
              <td className="dir num">{num(b.quantidade, 3)} {unidade(b.unidade)}</td>
              <td className="dir num">{num(b.saldoAtual, 3)} {unidade(b.unidade)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Producoes() {
  const [pagina, setPagina] = useState(0);
  const [abertas, setAbertas] = useState<Set<string>>(new Set());
  const q = useQuery({
    queryKey: ['producoes', pagina],
    placeholderData: keepPreviousData,
    queryFn: () => get<Pagina<Producao>>(`/producoes?page=${pagina}&size=20`),
  });

  let corpo;
  if (q.isPending) corpo = <Carregando />;
  else if (q.isError) corpo = <ErroCarregar erro={q.error} />;
  else if (!q.data.content.length) {
    corpo = (
      <Vazio texto='Nenhuma produção registrada. Abra uma receita e use "Registrar produção" para baixar os ingredientes do estoque.'>
        <a className="botao primario" href="#/receitas">Ver receitas</a>
      </Vazio>
    );
  } else {
    corpo = (
      <>
        {q.data.content.map((p) => (
          <details key={p.id} className="bloco" onToggle={(e) => {
            const aberto = (e.currentTarget as HTMLDetailsElement).open;
            setAbertas((s) => { const n = new Set(s); if (aberto) n.add(p.id); else n.delete(p.id); return n; });
          }}>
            <summary>
              {p.receita}{p.tamanho ? ` (${p.tamanho})` : ''}: {p.tamanho ? plural(p.lotes, 'vez', 'vezes') : plural(p.lotes, 'lote', 'lotes')} ({p.unidadesProduzidas} un){' '}
              <span className="muted small">{dataHora(p.criadoEm)}, materiais {brl(p.custoMateriais)}</span>
            </summary>
            {abertas.has(p.id) ? <Baixas id={p.id} /> : null}
          </details>
        ))}
        <Paginacao pagina={q.data} irPara={setPagina} />
      </>
    );
  }

  return (
    <div>
      <Cabecalho titulo="Produções" descricao="Cada produção baixa todos os ingredientes e embalagens de uma vez. Se faltar algo, nada é baixado." />
      <section className="painel">{corpo}</section>
    </div>
  );
}
