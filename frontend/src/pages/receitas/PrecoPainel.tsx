import { useQuery } from '@tanstack/react-query';
import { type FormEvent, useState } from 'react';
import { ApiError, get, post } from '../../api/client';
import type { Preco, Tamanho } from '../../api/types';
import { Fatia } from '../../components/Fatia';
import { Campo, Carregando, ErroCarregar, ProblemaAlerta, numero } from '../../components/ui';
import { brl, custoUnitario, fracaoReceita, num, pct, plural } from '../../lib/format';

const COMPONENTE: Record<string, string> = {
  INSUMO: 'Ingrediente', EMBALAGEM: 'Embalagem', MAO_DE_OBRA: 'Trabalho', ENERGIA: 'Energia', GAS: 'Gás', CUSTOS_FIXOS: 'Custos fixos',
};
const BANDEIRAS: [string, string][] = [['VERDE', 'Verde'], ['AMARELA', 'Amarela'], ['VERMELHA_1', 'Vermelha 1'], ['VERMELHA_2', 'Vermelha 2']];

type Simulacao = { margemLucro?: number; bandeira?: string; taxaPercentual?: number };

export const caminhoPreco = (receitaId: string, tamanho: Tamanho | null) =>
  `/receitas/${receitaId}/preco${tamanho ? `?tamanhoId=${tamanho.id}` : ''}`;

/** Price of the whole recipe or of one size: the cost slice, the price per channel and an "E se...?" simulation. */
export function PrecoPainel({ receitaId, tamanho }: { receitaId: string; tamanho: Tamanho | null }) {
  const [simulacao, setSimulacao] = useState<Simulacao | null>(null);
  const salvo = useQuery({ queryKey: ['preco', receitaId, tamanho?.id ?? null], queryFn: () => get<Preco>(caminhoPreco(receitaId, tamanho)) });
  const simulado = useQuery({
    queryKey: ['simulacao', receitaId, tamanho?.id ?? null, simulacao],
    enabled: simulacao != null,
    queryFn: () => post<Preco>(`/receitas/${receitaId}/simulacao`, { ...simulacao, tamanhoId: tamanho?.id ?? null }),
  });
  const ativo = simulacao ? simulado : salvo;
  const voltar = <button type="button" className="discreto" onClick={() => setSimulacao(null)}>Voltar ao preço salvo</button>;

  if (ativo.isPending) return <section className="painel"><Carregando /></section>;
  if (ativo.isError) {
    if (!(ativo.error instanceof ApiError) || ativo.error.status !== 422) return <ErroCarregar erro={ativo.error} />;
    return (
      <section className="painel">
        <h2>Preço</h2>
        <ProblemaAlerta erro={ativo.error} tom="aviso">
          <p>{simulacao ? 'Ajuste os valores da simulação.' : <a href="#/configuracao">Revisar meus custos</a>}</p>
        </ProblemaAlerta>
        {simulacao ? <p className="bloco">{voltar}</p> : null}
      </section>
    );
  }

  const p = ativo.data;
  const pecas = p.rendimento;
  const linhas = [p.precoBase, ...p.precosPorCanal];
  const titulo = simulacao ? 'Simulação' : tamanho ? tamanho.nome : 'Preço por unidade';
  const loteDescricao = tamanho
    ? `Custo de ${plural(pecas, 'peça', 'peças')} com embalagem (${fracaoReceita(tamanho.fracaoReceita)})`
    : `Custo do lote (${pecas} un)`;

  return (
    <section className="painel" aria-live="polite">
      <div className="cabecalho">
        <h2>{titulo}</h2>
        {simulacao ? <div className="acoes"><span className="selo atencao">nada foi salvo</span>{voltar}</div> : null}
      </div>
      <div className={`preco-grade${simulacao ? ' simulando' : ''}`}>
        <Fatia composicao={p.composicaoLote} rotuloTotal={loteDescricao} />
        <div>
          <p className="muted small">
            {tamanho && pecas > 1 ? `Cada uma das ${pecas} peças, ` : ''}{p.precoBase.canal.toLowerCase()}, com {pct(p.margemLucro)} de margem
          </p>
          <p className="preco-destaque num">{brl(p.precoBase.preco)}</p>
          <p className="preco-sub">
            {tamanho && pecas === 1 ? `Custa ${custoUnitario(p.custoUnitario)}.` : `Custa ${custoUnitario(p.custoUnitario)} por unidade.`}
            {' '}Você lucra <strong className="num">{brl(p.precoBase.lucroUnitario)}</strong> em cada {tamanho && pecas === 1 ? 'venda' : 'uma'}.
          </p>
          <div className="tabela-wrap">
            <table>
              <thead>
                <tr><th>Canal</th><th className="dir">Taxa</th><th className="dir">Preço</th><th className="dir">Taxa paga</th><th className="dir">Seu lucro</th></tr>
              </thead>
              <tbody>
                {linhas.map((l) => (
                  <tr key={`${l.canalId}-${l.canal}`}>
                    <td>{l.canal}</td>
                    <td className="dir num">{pct(l.taxaPercentual)}</td>
                    <td className="dir num"><strong>{brl(l.preco)}</strong></td>
                    <td className="dir num">{brl(l.valorTaxa)}</td>
                    <td className="dir num">{brl(l.lucroUnitario)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {p.precosPorCanal.length ? null : (
            <p className="small muted">Cadastre <a href="#/canais">canais de venda</a> para ver o preço com a taxa de cada um.</p>
          )}
          <FormSimulacao margemAtual={p.margemLucro} simulacao={simulacao} onSimular={setSimulacao} />
        </div>
      </div>
      <details className="bloco">
        <summary>Ver a conta item por item</summary>
        <div className="tabela-wrap">
          <table>
            <thead>
              <tr><th>Tipo</th><th>Item</th><th className="dir">Quantidade</th><th className="dir">Custo unit.</th><th className="dir">Custo</th></tr>
            </thead>
            <tbody>
              {p.detalhamento.map((l, i) => (
                <tr key={i}>
                  <td>{COMPONENTE[l.componente] ?? l.componente}</td>
                  <td>{l.item}</td>
                  <td className="dir num">{num(l.quantidade, 4)} {l.unidade}</td>
                  <td className="dir num">{custoUnitario(l.custoUnitario)}</td>
                  <td className="dir num">{brl(l.custo)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </section>
  );
}

function FormSimulacao({ margemAtual, simulacao, onSimular }: {
  margemAtual: number; simulacao: Simulacao | null; onSimular: (s: Simulacao) => void;
}) {
  function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const f = Object.fromEntries(new FormData(ev.currentTarget)) as Record<string, string>;
    const s: Simulacao = {};
    const margem = numero(f.margemLucro);
    const taxa = numero(f.taxaPercentual);
    if (margem != null) s.margemLucro = margem;
    if (taxa != null) s.taxaPercentual = taxa;
    if (f.bandeira) s.bandeira = f.bandeira;
    onSimular(s);
  }
  return (
    <form className="campos bloco" noValidate onSubmit={enviar}>
      <h3>E se...?</h3>
      <div className="grade">
        <Campo rotulo="Margem (%)">
          <input name="margemLucro" type="number" min={0} max={99.99} step="0.5" inputMode="decimal" defaultValue={simulacao?.margemLucro ?? margemAtual} />
        </Campo>
        <Campo rotulo="Bandeira">
          <select name="bandeira" defaultValue={simulacao?.bandeira ?? ''}>
            <option value="">A vigente</option>
            {BANDEIRAS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </Campo>
        <Campo rotulo="Taxa avulsa (%)">
          <input name="taxaPercentual" type="number" min={0} max={99.99} step="0.5" inputMode="decimal" placeholder="opcional" defaultValue={simulacao?.taxaPercentual ?? ''} />
        </Campo>
      </div>
      <div><button type="submit">Simular preço</button></div>
    </form>
  );
}
