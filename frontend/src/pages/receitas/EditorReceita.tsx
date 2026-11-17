import { useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, type ReactNode, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { get, post, put, todos } from '../../api/client';
import type { Base, Embalagem, Equipamento, Insumo, Receita } from '../../api/types';
import { useAviso } from '../../components/Toasts';
import { Cabecalho, Campo, Carregando, ErroCarregar, ProblemaAlerta, Vazio, erroDe, numero } from '../../components/ui';
import { num } from '../../lib/format';
import { UNIDADES_POR_BASE } from '../estoque/dialogos';

const BASES: [Base, string][] = [['POR_LOTE', 'na receita toda'], ['POR_UNIDADE', 'em cada unidade']];
const FRACOES: [string, string][] = [
  ['1', 'Receita inteira'], ['0.5', 'Meia receita'], ['0.3333', '1/3 da receita'], ['0.25', '1/4 da receita'],
  ['2', 'Receita dobrada'], ['outra', 'Outra quantidade'],
];

interface LinhaInsumo { k: number; insumoId: string; quantidade: string; unidade: string; base: Base }
interface LinhaEmbalagem { k: number; embalagemId: string; quantidade: string; base: Base }
interface LinhaEquipamento { k: number; equipamentoId: string; minutosUso: string }
interface LinhaTamanho { k: number; nome: string; fracao: string; outra: string; rendimento: string; embalagemId: string; embalagensPorPeca: string }

let seq = 0;
const k = () => ++seq;
const txt = (v: number | null | undefined) => (v == null ? '' : String(v));

function fracaoInicial(v: number): Pick<LinhaTamanho, 'fracao' | 'outra'> {
  const preset = FRACOES.find(([f]) => f !== 'outra' && Math.abs(Number(f) - v) < 0.0001);
  return preset ? { fracao: preset[0], outra: '' } : { fracao: 'outra', outra: String(v) };
}

/** Create or edit a recipe: ingredients, packaging, electric equipment and the sizes it is sold in. */
export function EditorReceita() {
  const { id } = useParams();
  const q = useQuery({
    queryKey: ['editor-receita', id ?? 'nova'],
    gcTime: 0,
    queryFn: async () => {
      const [receita, insumos, embalagens, equipamentos] = await Promise.all([
        id ? get<Receita>(`/receitas/${id}`) : Promise.resolve(null),
        todos<Insumo>('/insumos'), todos<Embalagem>('/embalagens'), todos<Equipamento>('/equipamentos'),
      ]);
      return { receita, insumos, embalagens, equipamentos };
    },
  });
  if (q.isPending) return <Carregando />;
  if (q.isError) return <ErroCarregar erro={q.error} />;
  if (!q.data.insumos.length) {
    return (
      <div>
        <Cabecalho titulo="Nova receita" />
        <div className="painel">
          <Vazio texto="Para montar uma receita, cadastre primeiro os insumos que ela usa.">
            <a className="botao primario" href="#/insumos">Cadastrar insumos</a>
          </Vazio>
        </div>
      </div>
    );
  }
  return <Formulario {...q.data} />;
}

function Formulario({ receita, insumos, embalagens, equipamentos }: {
  receita: Receita | null; insumos: Insumo[]; embalagens: Embalagem[]; equipamentos: Equipamento[];
}) {
  const navigate = useNavigate();
  const aviso = useAviso();
  const queryClient = useQueryClient();
  const topo = useRef<HTMLDivElement>(null);
  const [erro, setErro] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  // A save may finish after the user left (e.g. logged out); then it must not navigate anywhere.
  const montado = useRef(true);
  useEffect(() => () => { montado.current = false; }, []);

  const [dados, setDados] = useState({
    nome: receita?.nome ?? '', rendimento: txt(receita?.rendimento), tempoPreparoMin: txt(receita?.tempoPreparoMin),
    minutosGas: txt(receita?.minutosGas ?? 0), margemLucro: txt(receita?.margemLucro ?? 30),
    vendasEstimadasMes: txt(receita?.vendasEstimadasMes),
  });
  const novoInsumo = (): LinhaInsumo => ({ k: k(), insumoId: insumos[0].id, quantidade: '', unidade: insumos[0].unidadeBase, base: 'POR_LOTE' });
  const [linhasInsumo, setLinhasInsumo] = useState<LinhaInsumo[]>(() => (receita?.insumos.length
    ? receita.insumos.map((i) => ({ k: k(), insumoId: i.insumoId, quantidade: String(i.quantidade), unidade: i.unidade, base: i.base }))
    : [novoInsumo()]));
  const [linhasEmbalagem, setLinhasEmbalagem] = useState<LinhaEmbalagem[]>(() =>
    receita?.embalagens.map((e) => ({ k: k(), embalagemId: e.embalagemId, quantidade: String(e.quantidade), base: e.base })) ?? []);
  const [linhasEquip, setLinhasEquip] = useState<LinhaEquipamento[]>(() =>
    receita?.equipamentos.map((e) => ({ k: k(), equipamentoId: e.equipamentoId, minutosUso: String(e.minutosUso) })) ?? []);
  const [tamanhos, setTamanhos] = useState<LinhaTamanho[]>(() =>
    receita?.tamanhos.map((t) => ({
      k: k(), nome: t.nome, ...fracaoInicial(Number(t.fracaoReceita)), rendimento: String(t.rendimento),
      embalagemId: t.embalagemId ?? '', embalagensPorPeca: txt(t.embalagensPorPeca ?? 1),
    })) ?? []);

  const campo = (nome: keyof typeof dados) => ({
    name: nome, value: dados[nome], 'aria-invalid': erroDe(erro, nome) ? true : undefined,
    onChange: (e: { target: { value: string } }) => setDados((d) => ({ ...d, [nome]: e.target.value })),
  });

  function alterar<T extends { k: number }>(set: (f: (l: T[]) => T[]) => void, chave: number, mudanca: Partial<T>) {
    set((lista) => lista.map((l) => (l.k === chave ? { ...l, ...mudanca } : l)));
  }

  async function enviar(ev: FormEvent) {
    ev.preventDefault();
    const corpo = {
      nome: dados.nome.trim(),
      rendimento: numero(dados.rendimento),
      tempoPreparoMin: numero(dados.tempoPreparoMin),
      minutosGas: numero(dados.minutosGas) ?? 0,
      margemLucro: numero(dados.margemLucro),
      vendasEstimadasMes: numero(dados.vendasEstimadasMes),
      insumos: linhasInsumo.map((l) => ({ insumoId: l.insumoId, quantidade: numero(l.quantidade), unidade: l.unidade, base: l.base })),
      embalagens: linhasEmbalagem.map((l) => ({ embalagemId: l.embalagemId, quantidade: numero(l.quantidade), base: l.base })),
      equipamentos: linhasEquip.map((l) => ({ equipamentoId: l.equipamentoId, minutosUso: numero(l.minutosUso) })),
      tamanhos: tamanhos.map((t) => ({
        nome: t.nome.trim(),
        fracaoReceita: numero(t.fracao === 'outra' ? t.outra : t.fracao),
        rendimento: numero(t.rendimento),
        embalagemId: t.embalagemId || null,
        embalagensPorPeca: t.embalagemId ? numero(t.embalagensPorPeca) ?? 1 : null,
      })),
    };
    setEnviando(true);
    try {
      const r = receita ? await put<Receita>(`/receitas/${receita.id}`, corpo) : await post<Receita>('/receitas', corpo);
      aviso(receita ? 'Receita salva.' : 'Receita criada.');
      await queryClient.invalidateQueries();
      if (montado.current) navigate(`/receitas/${r.id}`);
    } catch (e) {
      setErro(e);
      setEnviando(false);
      topo.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }

  const remover = (onClick: () => void) => <button type="button" className="perigo" aria-label="Remover item" onClick={onClick}>Remover</button>;
  const voltarPara = receita ? `#/receitas/${receita.id}` : '#/receitas';

  return (
    <div ref={topo}>
      <a className="voltar" href={voltarPara}>{receita ? 'Voltar para a receita' : 'Todas as receitas'}</a>
      <Cabecalho titulo={receita ? `Editar ${receita.nome}` : 'Nova receita'}
        descricao="Informe as quantidades da receita inteira. O preço aparece assim que você salvar." />
      <form className="campos" noValidate onSubmit={enviar}>
        <ProblemaAlerta erro={erro} camposNaTela={Object.keys(dados)} />

        <fieldset>
          <legend>Sobre a receita</legend>
          <div className="campos">
            <Campo rotulo="Nome" erro={erroDe(erro, 'nome')}><input {...campo('nome')} maxLength={120} placeholder="Ex.: Torta holandesa" /></Campo>
            <div className="grade">
              <Campo rotulo="Rende quantas unidades?" dica="Docinhos, fatias, ou 1 para uma torta inteira." erro={erroDe(erro, 'rendimento')}>
                <input {...campo('rendimento')} type="number" min={1} step="1" inputMode="numeric" />
              </Campo>
              <Campo rotulo="Tempo de trabalho (min)" erro={erroDe(erro, 'tempoPreparoMin')}>
                <input {...campo('tempoPreparoMin')} type="number" min={0} step="1" inputMode="numeric" />
              </Campo>
              <Campo rotulo="Forno a gás (min)" erro={erroDe(erro, 'minutosGas')}>
                <input {...campo('minutosGas')} type="number" min={0} step="1" inputMode="numeric" />
              </Campo>
              <Campo rotulo="Margem de lucro (%)" dica="Parte do preço que é lucro." erro={erroDe(erro, 'margemLucro')}>
                <input {...campo('margemLucro')} type="number" min={0} max={99.99} step="0.5" inputMode="decimal" />
              </Campo>
              <Campo rotulo="Vendas por mês" dica="Só se os custos fixos forem divididos por vendas." erro={erroDe(erro, 'vendasEstimadasMes')}>
                <input {...campo('vendasEstimadasMes')} type="number" min={0} step="1" inputMode="numeric" />
              </Campo>
            </div>
          </div>
        </fieldset>

        <fieldset>
          <legend>Insumos</legend>
          <p className="small muted">"Na receita toda" é a quantidade da receita inteira. "Em cada unidade" é multiplicada pelo rendimento.</p>
          <div className="itens">
            {linhasInsumo.map((l) => {
              const ins = insumos.find((i) => i.id === l.insumoId) ?? insumos[0];
              return (
                <div className="item-linha" key={l.k}>
                  <Campo rotulo="Insumo">
                    <select value={l.insumoId} onChange={(e) => {
                      const novo = insumos.find((i) => i.id === e.target.value)!;
                      alterar(setLinhasInsumo, l.k, { insumoId: novo.id, unidade: novo.unidadeBase });
                    }}>
                      {insumos.map((i) => <option key={i.id} value={i.id}>{i.nome}</option>)}
                    </select>
                  </Campo>
                  <Campo rotulo="Quantidade">
                    <input type="number" step="any" min={0} inputMode="decimal" value={l.quantidade}
                      onChange={(e) => alterar(setLinhasInsumo, l.k, { quantidade: e.target.value })} />
                  </Campo>
                  <Campo rotulo="Unidade">
                    <select value={l.unidade} onChange={(e) => alterar(setLinhasInsumo, l.k, { unidade: e.target.value })}>
                      {UNIDADES_POR_BASE[ins.unidadeBase].map(([v, r]) => <option key={v} value={v}>{r}</option>)}
                    </select>
                  </Campo>
                  <Campo rotulo="Conta">
                    <select value={l.base} onChange={(e) => alterar(setLinhasInsumo, l.k, { base: e.target.value as Base })}>
                      {BASES.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
                    </select>
                  </Campo>
                  {remover(() => setLinhasInsumo((ls) => ls.filter((x) => x.k !== l.k)))}
                </div>
              );
            })}
          </div>
          <p><button type="button" className="discreto" onClick={() => setLinhasInsumo((ls) => [...ls, novoInsumo()])}>Adicionar insumo</button></p>
        </fieldset>

        <fieldset>
          <legend>Embalagens da receita</legend>
          <p className="small muted">O que vai dentro do produto em qualquer tamanho, como forminhas. A caixa ou o pote de cada tamanho fica mais abaixo.</p>
          <div className="itens">
            {linhasEmbalagem.map((l) => (
              <div className="item-linha emb" key={l.k}>
                <Campo rotulo="Embalagem">
                  <select value={l.embalagemId} onChange={(e) => alterar(setLinhasEmbalagem, l.k, { embalagemId: e.target.value })}>
                    {embalagens.map((e) => <option key={e.id} value={e.id}>{e.nome}</option>)}
                  </select>
                </Campo>
                <Campo rotulo="Quantidade">
                  <input type="number" step="any" min={0} inputMode="decimal" value={l.quantidade}
                    onChange={(e) => alterar(setLinhasEmbalagem, l.k, { quantidade: e.target.value })} />
                </Campo>
                <Campo rotulo="Conta">
                  <select value={l.base} onChange={(e) => alterar(setLinhasEmbalagem, l.k, { base: e.target.value as Base })}>
                    {BASES.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
                  </select>
                </Campo>
                {remover(() => setLinhasEmbalagem((ls) => ls.filter((x) => x.k !== l.k)))}
              </div>
            ))}
          </div>
          {embalagens.length ? (
            <p><button type="button" className="discreto" onClick={() => setLinhasEmbalagem((ls) => [...ls, { k: k(), embalagemId: embalagens[0].id, quantidade: '1', base: 'POR_UNIDADE' }])}>Adicionar embalagem</button></p>
          ) : <p className="small muted">Nenhuma embalagem cadastrada. <a href="#/embalagens">Cadastrar embalagens</a></p>}
        </fieldset>

        <fieldset>
          <legend>Equipamentos elétricos</legend>
          <div className="itens">
            {linhasEquip.map((l) => (
              <div className="item-linha eq" key={l.k}>
                <Campo rotulo="Equipamento">
                  <select value={l.equipamentoId} onChange={(e) => alterar(setLinhasEquip, l.k, { equipamentoId: e.target.value })}>
                    {equipamentos.map((e) => <option key={e.id} value={e.id}>{e.nome} ({num(e.potenciaWatts, 0)} W)</option>)}
                  </select>
                </Campo>
                <Campo rotulo="Minutos de uso">
                  <input type="number" step="1" min={1} inputMode="numeric" value={l.minutosUso}
                    onChange={(e) => alterar(setLinhasEquip, l.k, { minutosUso: e.target.value })} />
                </Campo>
                {remover(() => setLinhasEquip((ls) => ls.filter((x) => x.k !== l.k)))}
              </div>
            ))}
          </div>
          {equipamentos.length ? (
            <p><button type="button" className="discreto" onClick={() => setLinhasEquip((ls) => [...ls, { k: k(), equipamentoId: equipamentos[0].id, minutosUso: '' }])}>Adicionar equipamento</button></p>
          ) : <p className="small muted">Nenhum equipamento cadastrado. <a href="#/equipamentos">Cadastrar equipamentos</a></p>}
        </fieldset>

        <fieldset>
          <legend>Tamanhos de venda</legend>
          <p className="small muted">
            Opcional. Use quando a mesma receita vira produtos diferentes: torta grande com a receita inteira, torta pequena com meia receita,
            potes individuais em que uma receita rende 8. Cada tamanho tem preço próprio, e os ingredientes e o trabalho entram na proporção usada.
          </p>
          <div className="itens">
            {tamanhos.map((t, i) => (
              <LinhaTamanhoCampos key={t.k} indice={i} linha={t} embalagens={embalagens}
                onMudar={(m) => alterar(setTamanhos, t.k, m)}
                remover={remover(() => setTamanhos((ls) => ls.filter((x) => x.k !== t.k)))} />
            ))}
          </div>
          <p>
            <button type="button" className="discreto" onClick={() => setTamanhos((ls) => [...ls, {
              k: k(), nome: '', fracao: '1', outra: '', rendimento: '1', embalagemId: '', embalagensPorPeca: '1',
            }])}>Adicionar tamanho</button>
          </p>
        </fieldset>

        <div className="acoes">
          <button type="submit" className="primario" disabled={enviando}>{receita ? 'Salvar receita' : 'Criar receita'}</button>
          <a className="botao" href={voltarPara}>Cancelar</a>
        </div>
      </form>
    </div>
  );
}

function LinhaTamanhoCampos({ indice, linha, embalagens, onMudar, remover }: {
  indice: number; linha: LinhaTamanho; embalagens: Embalagem[]; onMudar: (m: Partial<LinhaTamanho>) => void; remover: ReactNode;
}) {
  return (
    <div className="item-linha tam" aria-label={`Tamanho ${indice + 1}`} role="group">
      <Campo rotulo="Nome do tamanho">
        <input value={linha.nome} maxLength={60} placeholder="Ex.: Torta pequena" onChange={(e) => onMudar({ nome: e.target.value })} />
      </Campo>
      <Campo rotulo="Quanto da receita">
        <select value={linha.fracao} onChange={(e) => onMudar({ fracao: e.target.value })}>
          {FRACOES.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
        </select>
      </Campo>
      {linha.fracao === 'outra' ? (
        <Campo rotulo="Receitas (1 = inteira)">
          <input type="number" step="any" min={0} inputMode="decimal" value={linha.outra} placeholder="Ex.: 0,2"
            onChange={(e) => onMudar({ outra: e.target.value })} />
        </Campo>
      ) : null}
      <Campo rotulo="Rende quantas peças">
        <input type="number" step="1" min={1} inputMode="numeric" value={linha.rendimento} onChange={(e) => onMudar({ rendimento: e.target.value })} />
      </Campo>
      <Campo rotulo="Embalagem de cada peça">
        <select value={linha.embalagemId} onChange={(e) => onMudar({ embalagemId: e.target.value })}>
          <option value="">Sem embalagem</option>
          {embalagens.map((e) => <option key={e.id} value={e.id}>{e.nome}</option>)}
        </select>
      </Campo>
      {remover}
    </div>
  );
}
