// Shapes returned by the API (BigDecimal values arrive as JSON numbers).

export interface Pagina<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type Perfil = 'ADMIN' | 'CONFEITEIRA';

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
  criadoEm: string;
}

export type UnidadeBase = 'G' | 'ML' | 'UN';
export type UnidadeMedida = 'G' | 'KG' | 'ML' | 'L' | 'UN';
export type Base = 'POR_LOTE' | 'POR_UNIDADE';
export type Bandeira = 'VERDE' | 'AMARELA' | 'VERMELHA_1' | 'VERMELHA_2';

export interface Insumo {
  id: string;
  nome: string;
  unidadeBase: UnidadeBase;
  custoUnitario: number;
  ultimaQuantidadeEmbalagem: number;
  ultimoPrecoEmbalagem: number;
  saldo: number;
  estoqueMinimo: number;
  abaixoDoMinimo: boolean;
  ativo: boolean;
}

export interface Embalagem {
  id: string;
  nome: string;
  custoUnitario: number;
  saldo: number;
  estoqueMinimo: number;
  abaixoDoMinimo: boolean;
  ativo: boolean;
}

export interface Equipamento {
  id: string;
  nome: string;
  potenciaWatts: number;
  ativo: boolean;
}

export interface CustoFixo {
  id: string;
  descricao: string;
  valorMensal: number;
  ativo: boolean;
}

export interface CanalVenda {
  id: string;
  nome: string;
  taxaPercentual: number;
  ativo: boolean;
}

export interface Tamanho {
  id: string;
  nome: string;
  fracaoReceita: number;
  rendimento: number;
  embalagemId: string | null;
  embalagem: string | null;
  embalagensPorPeca: number | null;
}

export interface Receita {
  id: string;
  nome: string;
  rendimento: number;
  tempoPreparoMin: number;
  minutosGas: number;
  margemLucro: number;
  vendasEstimadasMes: number | null;
  ativo: boolean;
  insumos: { insumoId: string; nome: string; quantidade: number; unidade: UnidadeBase; base: Base; quantidadePorLote: number }[];
  embalagens: { embalagemId: string; nome: string; quantidade: number; base: Base; quantidadePorLote: number }[];
  equipamentos: { equipamentoId: string; nome: string; potenciaWatts: number; minutosUso: number }[];
  tamanhos: Tamanho[];
}

export interface ReceitaResumo {
  id: string;
  nome: string;
  rendimento: number;
  margemLucro: number;
  ativo: boolean;
  tamanhos: string[];
}

export interface Composicao {
  insumos: number;
  embalagens: number;
  maoDeObra: number;
  energia: number;
  gas: number;
  custosFixos: number;
  total: number;
}

export interface PrecoCanal {
  canalId: string | null;
  canal: string;
  taxaPercentual: number;
  preco: number;
  valorTaxa: number;
  lucroUnitario: number;
}

export interface Preco {
  receitaId: string;
  receita: string;
  simulacao: boolean;
  bandeira: Bandeira;
  tamanho: { id: string; nome: string; fracaoReceita: number; rendimento: number } | null;
  rendimento: number;
  margemLucro: number;
  composicaoLote: Composicao;
  detalhamento: { componente: string; item: string; quantidade: number; unidade: string; custoUnitario: number; custo: number }[];
  custoUnitario: number;
  precoBase: PrecoCanal;
  precosPorCanal: PrecoCanal[];
}

export interface Capacidade {
  receitaId: string;
  receita: string;
  tamanho: string | null;
  lotesPossiveis: number;
  unidadesPossiveis: number;
  fatorLimitante: { tipo: string; itemId: string; item: string; unidade: string; necessarioPorLote: number; saldo: number } | null;
}

export interface Baixa {
  tipo: 'INSUMO' | 'EMBALAGEM';
  itemId: string;
  item: string;
  unidade: string;
  quantidade: number;
  saldoAtual: number;
}

export interface Producao {
  id: string;
  receitaId: string;
  receita: string;
  tamanho: string | null;
  lotes: number;
  unidadesProduzidas: number;
  custoMateriais: number;
  criadoEm: string;
  baixas: Baixa[];
}

export interface Movimentacao {
  id: string;
  tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE';
  motivo: 'COMPRA' | 'PRODUCAO' | 'PERDA' | 'VENCIMENTO' | 'INVENTARIO' | 'OUTRO';
  tipoItem: 'INSUMO' | 'EMBALAGEM';
  itemId: string;
  item: string;
  quantidade: number;
  saldoApos: number;
  custoUnitario: number;
  valorTotal: number | null;
  observacao: string | null;
  criadoEm: string;
}

export interface ItemAbaixoMinimo {
  tipo: 'INSUMO' | 'EMBALAGEM';
  id: string;
  nome: string;
  unidade: string;
  saldo: number;
  estoqueMinimo: number;
  faltaParaMinimo: number;
}

export interface Configuracao {
  salarioDesejadoMes: number;
  horasTrabalhadasMes: number;
  valorHora: number;
  bandeiraVigente: Bandeira;
  gasPrecoBotijao: number | null;
  gasDuracaoHoras: number | null;
  custoGasPorHora: number | null;
  criterioRateio: 'HORAS' | 'VENDAS';
  permitirSaldoNegativo: boolean;
}

export interface Tarifa {
  id: string;
  tarifaKwh: number;
  vigenciaInicio: string;
}

export interface AdicionalBandeira {
  id: string;
  bandeira: Bandeira;
  adicionalKwh: number;
  vigenciaInicio: string;
}

export interface CompraResposta {
  saldoAtual: number;
  custoUnitarioAnterior: number;
  custoUnitarioAtual: number;
}
