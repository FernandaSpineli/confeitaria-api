import { useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, useState } from 'react';
import { post, todos } from '../../api/client';
import type { CompraResposta, Embalagem, Insumo, Movimentacao, UnidadeBase } from '../../api/types';
import { useAviso } from '../../components/Toasts';
import { Campo, Carregando, Modal, ProblemaAlerta, Vazio, erroDe, numero } from '../../components/ui';
import { custoUnitario, num, unidade } from '../../lib/format';

export const UNIDADES_POR_BASE: Record<UnidadeBase, [string, string][]> = {
  G: [['G', 'g'], ['KG', 'kg']],
  ML: [['ML', 'ml'], ['L', 'L']],
  UN: [['UN', 'un']],
};

export interface ItemEstoque {
  tipo: 'insumo' | 'embalagem';
  id: string;
  nome: string;
  base: UnidadeBase;
}

const chave = (i: ItemEstoque) => `${i.tipo}:${i.id}`;

/** Every ingredient and packaging, for the "which item?" select. */
function useItensEstoque(ativo: boolean) {
  return useQuery({
    queryKey: ['itens-estoque'],
    enabled: ativo,
    queryFn: async () => {
      const [ins, embs] = await Promise.all([todos<Insumo>('/insumos'), todos<Embalagem>('/embalagens')]);
      return [
        ...ins.map((i): ItemEstoque => ({ tipo: 'insumo', id: i.id, nome: `${i.nome} (insumo, ${unidade(i.unidadeBase)})`, base: i.unidadeBase })),
        ...embs.map((e): ItemEstoque => ({ tipo: 'embalagem', id: e.id, nome: `${e.nome} (embalagem)`, base: 'UN' })),
      ];
    },
  });
}

function corpoComItem(item: ItemEstoque, quantidade: number | null, unidadeSel: string, extra: Record<string, unknown>) {
  return { [item.tipo === 'insumo' ? 'insumoId' : 'embalagemId']: item.id, quantidade, unidade: unidadeSel, ...extra };
}

/** Purchase: adds to the balance and updates the weighted average cost. Without `item`, the user picks one. */
export function DialogoCompra({ item, onFechar }: { item?: ItemEstoque; onFechar: () => void }) {
  const lista = useItensEstoque(!item);
  const opcoes = item ? [item] : lista.data;
  const [escolha, setEscolha] = useState<string>(item ? chave(item) : '');
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const aviso = useAviso();
  const queryClient = useQueryClient();

  if (!opcoes) return <Modal titulo="Registrar compra" onFechar={onFechar}><Carregando /></Modal>;
  if (!opcoes.length) {
    return (
      <Modal titulo="Registrar compra" onFechar={onFechar}>
        <Vazio texto="Cadastre insumos ou embalagens antes de registrar compras." />
        <div className="dialogo-rodape"><button type="button" onClick={onFechar}>Fechar</button></div>
      </Modal>
    );
  }
  const atual = opcoes.find((o) => chave(o) === escolha) ?? opcoes[0];

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const f = Object.fromEntries(new FormData(ev.currentTarget)) as Record<string, string>;
    setEnviando(true);
    try {
      const r = await post<CompraResposta>('/estoque/compras', corpoComItem(atual, numero(f.quantidade), f.unidade,
        { valorPago: numero(f.valorPago), observacao: f.observacao || null }));
      const mudou = Number(r.custoUnitarioAnterior) !== Number(r.custoUnitarioAtual);
      aviso(`Compra registrada. Saldo: ${num(r.saldoAtual, 3)}.${mudou ? ` Custo médio: ${custoUnitario(r.custoUnitarioAnterior)} → ${custoUnitario(r.custoUnitarioAtual)}.` : ''}`);
      await queryClient.invalidateQueries();
      onFechar();
    } catch (e) {
      setErro(e);
      setEnviando(false);
    }
  }

  return (
    <Modal titulo={item ? `Registrar compra: ${item.nome}` : 'Registrar compra'} onFechar={onFechar}>
      <form className="campos" noValidate onSubmit={enviar}>
        <ProblemaAlerta erro={erro} camposNaTela={['quantidade', 'unidade', 'valorPago', 'observacao']} />
        {!item ? (
          <Campo rotulo="Item">
            <select name="item" value={chave(atual)} onChange={(e) => setEscolha(e.target.value)}>
              {opcoes.map((o) => <option key={chave(o)} value={chave(o)}>{o.nome}</option>)}
            </select>
          </Campo>
        ) : null}
        <div className="grade">
          <Campo rotulo="Quantidade comprada" erro={erroDe(erro, 'quantidade')}>
            <input name="quantidade" type="number" step="any" min={0} inputMode="decimal" />
          </Campo>
          <Campo rotulo="Unidade">
            <select name="unidade" key={atual.base}>
              {UNIDADES_POR_BASE[atual.base].map(([v, r]) => <option key={v} value={v}>{r}</option>)}
            </select>
          </Campo>
          <Campo rotulo="Valor pago (R$)" dica="Total da nota para essa quantidade." erro={erroDe(erro, 'valorPago')}>
            <input name="valorPago" type="number" step="0.01" min={0} inputMode="decimal" />
          </Campo>
        </div>
        <Campo rotulo="Observação"><input name="observacao" maxLength={255} placeholder="Opcional, ex.: atacadão" /></Campo>
        <p className="small muted">O custo do item passa a ser a média ponderada entre o que havia em estoque e esta compra.</p>
        <div className="dialogo-rodape">
          <button type="button" onClick={onFechar}>Cancelar</button>
          <button type="submit" className="primario" disabled={enviando}>Registrar compra</button>
        </div>
      </form>
    </Modal>
  );
}

/** Loss, expiry or stock count: a new signed movement, never an edit of history. */
export function DialogoAjuste({ onFechar }: { onFechar: () => void }) {
  const lista = useItensEstoque(true);
  const [escolha, setEscolha] = useState('');
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const aviso = useAviso();
  const queryClient = useQueryClient();

  if (!lista.data) return <Modal titulo="Ajustar estoque" onFechar={onFechar}><Carregando /></Modal>;
  if (!lista.data.length) {
    return (
      <Modal titulo="Ajustar estoque" onFechar={onFechar}>
        <Vazio texto="Cadastre insumos ou embalagens primeiro." />
        <div className="dialogo-rodape"><button type="button" onClick={onFechar}>Fechar</button></div>
      </Modal>
    );
  }
  const atual = lista.data.find((o) => chave(o) === escolha) ?? lista.data[0];

  async function enviar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    const f = Object.fromEntries(new FormData(ev.currentTarget)) as Record<string, string>;
    setEnviando(true);
    try {
      const m = await post<Movimentacao>('/estoque/ajustes', corpoComItem(atual, numero(f.quantidade), f.unidade,
        { motivo: f.motivo, observacao: f.observacao || null }));
      aviso(`Ajuste registrado. ${m.item}: saldo ${num(m.saldoApos, 3)}.`);
      await queryClient.invalidateQueries();
      onFechar();
    } catch (e) {
      setErro(e);
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Ajustar estoque" onFechar={onFechar}>
      <form className="campos" noValidate onSubmit={enviar}>
        <p className="muted">Use para perdas, itens vencidos ou para acertar o saldo depois de contar o estoque. O histórico nunca é apagado: o ajuste vira uma nova movimentação.</p>
        <ProblemaAlerta erro={erro} camposNaTela={['quantidade', 'motivo', 'observacao']} />
        <Campo rotulo="Item">
          <select name="item" value={chave(atual)} onChange={(e) => setEscolha(e.target.value)}>
            {lista.data.map((o) => <option key={chave(o)} value={chave(o)}>{o.nome}</option>)}
          </select>
        </Campo>
        <div className="grade">
          <Campo rotulo="Quantidade" dica="Negativa para retirar (ex.: -200), positiva para somar." erro={erroDe(erro, 'quantidade')}>
            <input name="quantidade" type="number" step="any" inputMode="decimal" />
          </Campo>
          <Campo rotulo="Unidade">
            <select name="unidade" key={atual.base}>
              {UNIDADES_POR_BASE[atual.base].map(([v, r]) => <option key={v} value={v}>{r}</option>)}
            </select>
          </Campo>
          <Campo rotulo="Motivo" erro={erroDe(erro, 'motivo')}>
            <select name="motivo">
              <option value="PERDA">Perda</option>
              <option value="VENCIMENTO">Vencimento</option>
              <option value="INVENTARIO">Contagem de estoque</option>
              <option value="OUTRO">Outro</option>
            </select>
          </Campo>
        </div>
        <Campo rotulo="Observação"><input name="observacao" maxLength={255} /></Campo>
        <div className="dialogo-rodape">
          <button type="button" onClick={onFechar}>Cancelar</button>
          <button type="submit" className="primario" disabled={enviando}>Registrar ajuste</button>
        </div>
      </form>
    </Modal>
  );
}
