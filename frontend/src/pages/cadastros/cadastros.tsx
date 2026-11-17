import { useState } from 'react';
import type { CanalVenda, CustoFixo, Embalagem, Equipamento, Insumo } from '../../api/types';
import { brl, custoUnitario, num, pct, unidade } from '../../lib/format';
import { DialogoCompra, type ItemEstoque } from '../estoque/dialogos';
import { Cadastro, type CadastroCfg } from './Cadastro';

export const UNIDADES: [string, string][] = [['G', 'g (gramas)'], ['KG', 'kg'], ['ML', 'ml'], ['L', 'litro'], ['UN', 'unidade']];

const texto = (v: number | null | undefined) => (v == null ? '' : String(v));

function Saldo({ saldo, base, abaixo }: { saldo: number; base: string; abaixo: boolean }) {
  return (
    <span>
      <span className="num">{num(saldo, 3)} {unidade(base)}</span>{' '}
      {abaixo ? <span className="selo atencao">abaixo do mínimo</span> : null}
    </span>
  );
}

function BotaoCompra({ item }: { item: ItemEstoque }) {
  const [aberto, setAberto] = useState(false);
  return (
    <>
      <button type="button" className="discreto" onClick={() => setAberto(true)}>Registrar compra</button>
      {aberto ? <DialogoCompra item={item} onFechar={() => setAberto(false)} /> : null}
    </>
  );
}

const insumos: CadastroCfg<Insumo> = {
  titulo: 'Insumos',
  descricao: 'Ingredientes com o preço da embalagem que você compra. O custo por grama, ml ou unidade é calculado sozinho e atualiza o preço das receitas.',
  caminho: '/insumos',
  singular: 'insumo',
  busca: true,
  vazio: 'Nenhum insumo ainda. Cadastre os ingredientes que você usa, como farinha, leite condensado e ovos.',
  avisoExclusao: 'Insumos usados em receitas ativas não podem ser excluídos.',
  nomeDe: (i) => i.nome,
  colunas: [
    { rotulo: 'Nome', valor: (i) => i.nome },
    { rotulo: 'Embalagem', valor: (i) => <span className="num">{num(i.ultimaQuantidadeEmbalagem, 3)} {unidade(i.unidadeBase)} por {brl(i.ultimoPrecoEmbalagem)}</span> },
    { rotulo: 'Custo', dir: true, valor: (i) => <span className="num">{custoUnitario(i.custoUnitario)} / {unidade(i.unidadeBase)}</span> },
    { rotulo: 'Em estoque', valor: (i) => <Saldo saldo={i.saldo} base={i.unidadeBase} abaixo={i.abaixoDoMinimo} /> },
  ],
  campos: [
    { nome: 'nome', rotulo: 'Nome' },
    { nome: 'unidade', rotulo: 'Unidade', tipo: 'selecao', opcoes: UNIDADES, linha: 'emb' },
    { nome: 'quantidadeEmbalagem', rotulo: 'Quantidade na embalagem', tipo: 'numero', min: 0, dica: 'Ex.: 395 para uma lata de 395 g.', linha: 'emb' },
    { nome: 'precoEmbalagem', rotulo: 'Preço da embalagem (R$)', tipo: 'numero', min: 0, passo: '0.01', linha: 'emb' },
    { nome: 'estoqueMinimo', rotulo: 'Estoque mínimo', tipo: 'numero', min: 0, dica: 'Na mesma unidade escolhida acima. Você recebe um alerta quando o saldo ficar abaixo.' },
  ],
  valores: (i) => ({
    nome: i?.nome ?? '', unidade: i?.unidadeBase ?? 'G', quantidadeEmbalagem: texto(i?.ultimaQuantidadeEmbalagem),
    precoEmbalagem: texto(i?.ultimoPrecoEmbalagem), estoqueMinimo: texto(i?.estoqueMinimo ?? 0),
  }),
  acoesExtras: (i) => <BotaoCompra item={{ tipo: 'insumo', id: i.id, nome: i.nome, base: i.unidadeBase }} />,
};

const embalagens: CadastroCfg<Embalagem> = {
  titulo: 'Embalagens',
  descricao: 'Caixas, potes, forminhas e etiquetas, com o custo de cada unidade.',
  caminho: '/embalagens',
  singular: 'embalagem',
  feminino: true,
  busca: true,
  vazio: 'Nenhuma embalagem ainda. Cadastre caixas, potes e forminhas para incluí-los no preço.',
  avisoExclusao: 'Embalagens usadas em receitas ativas (ou em tamanhos de receita) não podem ser excluídas.',
  nomeDe: (e) => e.nome,
  colunas: [
    { rotulo: 'Nome', valor: (e) => e.nome },
    { rotulo: 'Custo por unidade', dir: true, valor: (e) => <span className="num">{custoUnitario(e.custoUnitario)}</span> },
    { rotulo: 'Em estoque', valor: (e) => <Saldo saldo={e.saldo} base="un" abaixo={e.abaixoDoMinimo} /> },
  ],
  campos: [
    { nome: 'nome', rotulo: 'Nome' },
    { nome: 'custoUnitario', rotulo: 'Custo por unidade (R$)', tipo: 'numero', min: 0, passo: '0.0001', dica: 'Pacote de 100 por R$ 6,00 = 0,06.', linha: 'c' },
    { nome: 'estoqueMinimo', rotulo: 'Estoque mínimo (un)', tipo: 'numero', min: 0, passo: '1', linha: 'c' },
  ],
  valores: (e) => ({ nome: e?.nome ?? '', custoUnitario: texto(e?.custoUnitario), estoqueMinimo: texto(e?.estoqueMinimo ?? 0) }),
  acoesExtras: (e) => <BotaoCompra item={{ tipo: 'embalagem', id: e.id, nome: e.nome, base: 'UN' }} />,
};

const equipamentos: CadastroCfg<Equipamento> = {
  titulo: 'Equipamentos',
  descricao: 'Aparelhos elétricos e a potência de cada um. O consumo entra no preço pelos minutos de uso em cada receita.',
  caminho: '/equipamentos',
  singular: 'equipamento',
  vazio: 'Nenhum equipamento ainda. Cadastre batedeira, forno elétrico ou micro-ondas com a potência da etiqueta.',
  avisoExclusao: 'Equipamentos usados em receitas ativas não podem ser excluídos.',
  nomeDe: (e) => e.nome,
  colunas: [
    { rotulo: 'Nome', valor: (e) => e.nome },
    { rotulo: 'Potência', dir: true, valor: (e) => <span className="num">{num(e.potenciaWatts, 0)} W</span> },
  ],
  campos: [
    { nome: 'nome', rotulo: 'Nome' },
    { nome: 'potenciaWatts', rotulo: 'Potência (watts)', tipo: 'numero', min: 1, dica: 'Está na etiqueta ou no manual. Ex.: batedeira 500 W.' },
  ],
  valores: (e) => ({ nome: e?.nome ?? '', potenciaWatts: texto(e?.potenciaWatts) }),
};

const custosFixos: CadastroCfg<CustoFixo> = {
  titulo: 'Custos fixos',
  descricao: 'Contas do mês que existem mesmo sem produzir: aluguel, internet, MEI, água. São divididas entre as receitas.',
  caminho: '/custos-fixos',
  singular: 'custo fixo',
  vazio: 'Nenhum custo fixo ainda. Cadastre as contas mensais para que elas entrem no preço.',
  nomeDe: (c) => c.descricao,
  colunas: [
    { rotulo: 'Descrição', valor: (c) => c.descricao },
    { rotulo: 'Valor por mês', dir: true, valor: (c) => <span className="num">{brl(c.valorMensal)}</span> },
    { rotulo: 'Situação', valor: (c) => (c.ativo ? <span className="selo ok">entra no preço</span> : <span className="selo">pausado</span>) },
  ],
  topo: (itens) => {
    const total = itens.filter((c) => c.ativo).reduce((s, c) => s + Number(c.valorMensal), 0);
    return <p>Total ativo nesta página: <strong className="num">{brl(total)}</strong> por mês.</p>;
  },
  campos: [
    { nome: 'descricao', rotulo: 'Descrição' },
    { nome: 'valorMensal', rotulo: 'Valor por mês (R$)', tipo: 'numero', min: 0, passo: '0.01' },
    { nome: 'ativo', rotulo: 'Incluir no preço', tipo: 'check' },
  ],
  valores: (c) => ({ descricao: c?.descricao ?? '', valorMensal: texto(c?.valorMensal), ativo: c ? c.ativo : true }),
};

const canais: CadastroCfg<CanalVenda> = {
  titulo: 'Canais de venda',
  descricao: 'Onde você vende e a taxa de cada lugar. Cada receita mostra um preço por canal, com a mesma margem de lucro.',
  caminho: '/canais-venda',
  singular: 'canal',
  vazio: 'Nenhum canal ainda. Cadastre, por exemplo, Maquininha (4%) e iFood (27%).',
  nomeDe: (c) => c.nome,
  colunas: [
    { rotulo: 'Canal', valor: (c) => c.nome },
    { rotulo: 'Taxa', dir: true, valor: (c) => <span className="num">{pct(c.taxaPercentual)}</span> },
    { rotulo: 'Situação', valor: (c) => (c.ativo ? <span className="selo ok">ativo</span> : <span className="selo">pausado</span>) },
  ],
  campos: [
    { nome: 'nome', rotulo: 'Nome' },
    { nome: 'taxaPercentual', rotulo: 'Taxa (%)', tipo: 'numero', min: 0, max: 99.99, passo: '0.01', dica: 'Digite 27 para 27%.' },
    { nome: 'ativo', rotulo: 'Mostrar preço neste canal', tipo: 'check' },
  ],
  valores: (c) => ({ nome: c?.nome ?? '', taxaPercentual: texto(c?.taxaPercentual ?? 0), ativo: c ? c.ativo : true }),
};

export const Insumos = () => <Cadastro key={insumos.caminho} cfg={insumos} />;
export const Embalagens = () => <Cadastro key={embalagens.caminho} cfg={embalagens} />;
export const Equipamentos = () => <Cadastro key={equipamentos.caminho} cfg={equipamentos} />;
export const CustosFixos = () => <Cadastro key={custosFixos.caminho} cfg={custosFixos} />;
export const Canais = () => <Cadastro key={canais.caminho} cfg={canais} />;
