import { render, screen } from '@testing-library/react';
import { Fatia } from './Fatia';

describe('Fatia', () => {
  const tortaPequena = { insumos: 27.5, embalagens: 1, maoDeObra: 5, energia: 0, gas: 0, custosFixos: 0, total: 33.5 };

  it('draws one layer per cost that is not zero', () => {
    render(<Fatia composicao={tortaPequena} rotuloTotal="Custo da torta pequena" />);
    const legenda = screen.getAllByRole('listitem').map((li) => li.textContent);
    expect(legenda).toHaveLength(3);
    expect(legenda[0]).toMatch(/Ingredientes82%R\$\s27,50/);
    expect(legenda[1]).toMatch(/Embalagens3%/);
    expect(legenda[2]).toMatch(/Seu trabalho15%/);
    expect(screen.getByText('Custo da torta pequena')).toBeInTheDocument();
  });

  it('describes the composition for screen readers', () => {
    render(<Fatia composicao={tortaPequena} rotuloTotal="Total" />);
    expect(screen.getByRole('img').getAttribute('aria-label')).toContain('Seu trabalho');
  });
});
