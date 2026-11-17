import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { get } from '../../api/client';
import type { Pagina, ReceitaResumo } from '../../api/types';
import { Cabecalho, Carregando, ErroCarregar, Paginacao, Vazio } from '../../components/ui';
import { pct } from '../../lib/format';

export function ListaReceitas() {
  const [pagina, setPagina] = useState(0);
  const [busca, setBusca] = useState('');
  const [nome, setNome] = useState('');
  useEffect(() => {
    const t = setTimeout(() => { setNome(busca.trim()); setPagina(0); }, 250);
    return () => clearTimeout(t);
  }, [busca]);

  const q = useQuery({
    queryKey: ['receitas', pagina, nome],
    queryFn: () => {
      const p = new URLSearchParams({ page: String(pagina), size: '20' });
      if (nome) p.set('nome', nome);
      return get<Pagina<ReceitaResumo>>(`/receitas?${p}`);
    },
    placeholderData: keepPreviousData,
  });

  let corpo;
  if (q.isPending) corpo = <Carregando />;
  else if (q.isError) corpo = <ErroCarregar erro={q.error} />;
  else if (!q.data.content.length && !nome) {
    corpo = (
      <div className="painel">
        <Vazio texto="Nenhuma receita ainda. Monte a primeira com os insumos e embalagens cadastrados para ver o preço.">
          <a className="botao primario" href="#/receitas/nova">Criar receita</a>
        </Vazio>
      </div>
    );
  } else {
    corpo = (
      <div className="painel">
        <div className="tabela-wrap">
          <table>
            <thead>
              <tr>
                <th>Receita</th><th>Rende</th><th className="dir">Margem</th>
                <th><span className="sr-only">Abrir</span></th>
              </tr>
            </thead>
            <tbody>
              {q.data.content.length ? q.data.content.map((r) => (
                <tr key={r.id}>
                  <td><a href={`#/receitas/${r.id}`}>{r.nome}</a></td>
                  <td>{r.tamanhos.length ? <span className="tamanhos-lista">{r.tamanhos.join(', ')}</span> : <span className="num">{r.rendimento} un</span>}</td>
                  <td className="dir num">{pct(r.margemLucro)}</td>
                  <td className="acoes-linha"><a className="botao" href={`#/receitas/${r.id}`}>Ver preço</a></td>
                </tr>
              )) : <tr><td colSpan={4} className="vazio">Nenhuma receita com esse nome.</td></tr>}
            </tbody>
          </table>
        </div>
        <Paginacao pagina={q.data} irPara={setPagina} />
      </div>
    );
  }

  return (
    <div>
      <Cabecalho titulo="Receitas e preços" descricao="O preço é recalculado sempre que o custo de um insumo muda.">
        <input type="search" className="busca" placeholder="Buscar receita" aria-label="Buscar receita"
          value={busca} onChange={(e) => setBusca(e.target.value)} />
        <a className="botao primario" href="#/receitas/nova">Criar receita</a>
      </Cabecalho>
      {corpo}
    </div>
  );
}
