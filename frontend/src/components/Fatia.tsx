import type { Composicao } from '../api/types';
import { brl } from '../lib/format';

const CAMADAS: [keyof Composicao, string, string][] = [
  ['insumos', 'Ingredientes', 'var(--c-insumo)'],
  ['embalagens', 'Embalagens', 'var(--c-embalagem)'],
  ['maoDeObra', 'Seu trabalho', 'var(--c-mao)'],
  ['energia', 'Energia', 'var(--c-energia)'],
  ['gas', 'Gás', 'var(--c-gas)'],
  ['custosFixos', 'Custos fixos', 'var(--c-fixos)'],
];

/** The cost slice: each cake layer's thickness is its share of the batch cost. */
export function Fatia({ composicao, rotuloTotal }: { composicao: Composicao; rotuloTotal: string }) {
  const total = Number(composicao.total) || 0;
  const visiveis = CAMADAS.filter(([k]) => Number(composicao[k]) > 0);
  const descricao = visiveis.map(([k, r]) => `${r} ${brl(composicao[k])}`).join(', ');
  return (
    <div>
      <div className="fatia">
        <div className="fatia-corpo entra" role="img" aria-label={`Composição do custo: ${descricao}`}>
          {visiveis.map(([k, , cor]) => (
            <div key={k} className="camada" style={{ background: cor, flexGrow: Number(composicao[k]) }} />
          ))}
        </div>
        <ul className="fatia-legenda">
          {visiveis.map(([k, rotulo, cor]) => (
            <li key={k}>
              <span className="cor" style={{ background: cor }} />
              <span>{rotulo}<span className="pct small">{total ? Math.round((Number(composicao[k]) / total) * 100) : 0}%</span></span>
              <span className="num">{brl(composicao[k])}</span>
            </li>
          ))}
        </ul>
      </div>
      <div className="fatia-total"><span>{rotuloTotal}</span><strong className="num">{brl(composicao.total)}</strong></div>
    </div>
  );
}
