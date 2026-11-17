import { brl, custoUnitario, data, fracaoReceita, minutos, pct, plural } from './format';

// Intl uses a non-breaking space between "R$" and the number.
const semNbsp = (s: string) => s.replace(/ /g, ' ');

describe('format', () => {
  it('formats money in reais', () => {
    expect(semNbsp(brl(110))).toBe('R$ 110,00');
    expect(semNbsp(brl('55.83'))).toBe('R$ 55,83');
    expect(brl(null)).toBe('—');
  });

  it('shows fractions of a cent for small unit costs', () => {
    expect(semNbsp(custoUnitario('0.006'))).toBe('R$ 0,0060');
    expect(semNbsp(custoUnitario('9.125'))).toBe('R$ 9,13');
  });

  it('formats percentages, dates and durations', () => {
    expect(pct(40)).toBe('40%');
    expect(data('2026-01-31')).toBe('31/01/2026');
    expect(minutos(45)).toBe('45 min');
    expect(minutos(90)).toBe('1 h 30 min');
  });

  it('names common recipe fractions', () => {
    expect(fracaoReceita(1)).toBe('a receita inteira');
    expect(fracaoReceita('0.5')).toBe('meia receita');
    expect(fracaoReceita('0.3333')).toBe('1/3 da receita');
    expect(fracaoReceita(0.2)).toBe('20% da receita');
    expect(fracaoReceita(2)).toBe('2 receitas');
  });

  it('pluralises', () => {
    expect(plural(1, 'pote', 'potes')).toBe('1 pote');
    expect(plural(8, 'pote', 'potes')).toBe('8 potes');
  });
});
