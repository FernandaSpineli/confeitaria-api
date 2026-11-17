// Brazilian formatting for money, quantities, percentages and dates.

type Valor = number | string | null | undefined;

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const brl = (v: Valor): string => (v == null || v === '' ? '—' : moeda.format(Number(v)));

export function num(v: Valor, max = 2): string {
  if (v == null || v === '') return '—';
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: max }).format(Number(v));
}

/** Unit costs can be fractions of a cent (R$ 0,0060 per gram), so small values get 4 decimals. */
export function custoUnitario(v: Valor): string {
  if (v == null || v === '') return '—';
  const n = Number(v);
  const casas = n !== 0 && Math.abs(n) < 0.1 ? 4 : 2;
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency', currency: 'BRL', minimumFractionDigits: casas, maximumFractionDigits: casas,
  }).format(n);
}

export const pct = (v: Valor): string => (v == null || v === '' ? '—' : `${num(v, 2)}%`);

export function dataHora(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso));
}

/** "2026-01-31" → "31/01/2026" without timezone shifts. */
export function data(isoDate: string | null | undefined): string {
  if (!isoDate) return '—';
  const [a, m, d] = isoDate.split('-');
  return `${d}/${m}/${a}`;
}

export const unidade = (u: string | null | undefined): string => (u ? u.toLowerCase() : '');

/** Today in the browser's timezone as yyyy-mm-dd. */
export const hoje = (): string => new Date().toLocaleDateString('sv-SE');

export function minutos(m: number): string {
  if (m < 60) return `${m} min`;
  const resto = m % 60;
  return `${Math.floor(m / 60)} h${resto ? ` ${resto} min` : ''}`;
}

/** How much of the recipe a size uses, in words: 1 → "a receita inteira", 0,5 → "meia receita". */
export function fracaoReceita(v: Valor): string {
  const n = Number(v);
  const nomes: Record<string, string> = {
    '1': 'a receita inteira', '0.5': 'meia receita', '0.25': '1/4 da receita', '0.75': '3/4 da receita',
    '2': '2 receitas', '3': '3 receitas',
  };
  const exato = nomes[String(n)];
  if (exato) return exato;
  if (Math.abs(n - 1 / 3) < 0.001) return '1/3 da receita';
  if (n > 1) return `${num(n, 2)} receitas`;
  return `${num(n * 100, 1)}% da receita`;
}

export const plural = (n: number, um: string, varios: string): string => `${num(n, 0)} ${n === 1 ? um : varios}`;
