import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { get } from '../../api/client';
import type { ItemAbaixoMinimo, Movimentacao, Pagina } from '../../api/types';
import { Cabecalho, Campo, Carregando, ErroCarregar, Paginacao } from '../../components/ui';
import { brl, custoUnitario, dataHora, num, unidade } from '../../lib/format';
import { DialogoAjuste, DialogoCompra } from './dialogos';

const MOTIVOS: Record<Movimentacao['motivo'], string> = {
  COMPRA: 'Compra', PRODUCAO: 'Produção', PERDA: 'Perda', VENCIMENTO: 'Vencimento', INVENTARIO: 'Contagem de estoque', OUTRO: 'Outro',
};
const TIPOS: Record<Movimentacao['tipo'], [string, string]> = {
  ENTRADA: ['Entrada', 'ok'], SAIDA: ['Saída', 'rosa'], AJUSTE: ['Ajuste', 'atencao'],
};

export function Estoque() {
  const [dialogo, setDialogo] = useState<'compra' | 'ajuste' | null>(null);
  const [filtros, setFiltros] = useState({ tipo: '', de: '', ate: '' });
  const [pagina, setPagina] = useState(0);
  const minimo = useQuery({ queryKey: ['abaixo-minimo'], queryFn: () => get<ItemAbaixoMinimo[]>('/estoque/abaixo-minimo') });
  const movs = useQuery({
    queryKey: ['movimentacoes', filtros, pagina],
    placeholderData: keepPreviousData,
    queryFn: () => {
      const p = new URLSearchParams({ page: String(pagina), size: '20' });
      for (const [k, v] of Object.entries(filtros)) if (v) p.set(k, v);
      return get<Pagina<Movimentacao>>(`/estoque/movimentacoes?${p}`);
    },
  });
  const filtrar = (k: keyof typeof filtros, v: string) => { setFiltros((f) => ({ ...f, [k]: v })); setPagina(0); };

  return (
    <div>
      <Cabecalho titulo="Estoque" descricao="O saldo só muda por compras, produções e ajustes. Cada movimento fica registrado e não pode ser editado.">
        <button type="button" onClick={() => setDialogo('ajuste')}>Ajustar estoque</button>
        <button type="button" className="primario" onClick={() => setDialogo('compra')}>Registrar compra</button>
      </Cabecalho>

      <section className="painel">
        <h2>Abaixo do mínimo</h2>
        {minimo.isPending ? <Carregando /> : minimo.isError ? <ErroCarregar erro={minimo.error} /> : minimo.data.length ? (
          <ul className="lista-simples">
            {minimo.data.map((i) => (
              <li key={i.id}>
                <span>{i.nome} <span className="selo">{i.tipo === 'INSUMO' ? 'insumo' : 'embalagem'}</span></span>
                <span className="num">{num(i.saldo, 3)} de {num(i.estoqueMinimo, 3)} {unidade(i.unidade)}{' '}
                  <span className="selo atencao">faltam {num(i.faltaParaMinimo, 3)}</span></span>
              </li>
            ))}
          </ul>
        ) : <p className="muted">Tudo acima do estoque mínimo.</p>}
      </section>

      <section className="painel">
        <h2>Movimentações</h2>
        <div className="filtros">
          <Campo rotulo="Tipo">
            <select value={filtros.tipo} onChange={(e) => filtrar('tipo', e.target.value)}>
              <option value="">Todos</option><option value="ENTRADA">Entradas</option><option value="SAIDA">Saídas</option><option value="AJUSTE">Ajustes</option>
            </select>
          </Campo>
          <Campo rotulo="De"><input type="date" value={filtros.de} onChange={(e) => filtrar('de', e.target.value)} /></Campo>
          <Campo rotulo="Até"><input type="date" value={filtros.ate} onChange={(e) => filtrar('ate', e.target.value)} /></Campo>
        </div>
        {movs.isPending ? <Carregando /> : movs.isError ? <ErroCarregar erro={movs.error} /> : movs.data.content.length ? (
          <>
            <div className="tabela-wrap">
              <table>
                <thead>
                  <tr>{['Quando', 'Item', 'Movimento', 'Quantidade', 'Saldo depois', 'Custo unit.', 'Valor'].map((t, i) => <th key={t} className={i >= 3 ? 'dir' : undefined}>{t}</th>)}</tr>
                </thead>
                <tbody>
                  {movs.data.content.map((m) => (
                    <tr key={m.id}>
                      <td className="num">{dataHora(m.criadoEm)}</td>
                      <td>{m.item}{m.observacao ? <div className="small muted">{m.observacao}</div> : null}</td>
                      <td><span className={`selo ${TIPOS[m.tipo][1]}`}>{TIPOS[m.tipo][0]}</span> <span className="small muted">{MOTIVOS[m.motivo]}</span></td>
                      <td className="dir num">{Number(m.quantidade) > 0 ? '+' : ''}{num(m.quantidade, 3)}</td>
                      <td className="dir num">{num(m.saldoApos, 3)}</td>
                      <td className="dir num">{custoUnitario(m.custoUnitario)}</td>
                      <td className="dir num">{brl(m.valorTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Paginacao pagina={movs.data} irPara={setPagina} />
          </>
        ) : <p className="vazio">Nenhuma movimentação com esses filtros.</p>}
      </section>

      {dialogo === 'compra' ? <DialogoCompra onFechar={() => setDialogo(null)} /> : null}
      {dialogo === 'ajuste' ? <DialogoAjuste onFechar={() => setDialogo(null)} /> : null}
    </div>
  );
}
