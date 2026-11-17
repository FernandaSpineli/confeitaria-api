import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { get, patch } from '../../api/client';
import type { Pagina, Usuario } from '../../api/types';
import { useUsuario } from '../../auth/sessao';
import { useAviso } from '../../components/Toasts';
import { Cabecalho, Carregando, ErroCarregar, Paginacao } from '../../components/ui';
import { dataHora } from '../../lib/format';

export function Usuarias() {
  const eu = useUsuario();
  const [pagina, setPagina] = useState(0);
  const [busca, setBusca] = useState('');
  const [email, setEmail] = useState('');
  const [mudando, setMudando] = useState<string | null>(null);
  const aviso = useAviso();
  const queryClient = useQueryClient();
  useEffect(() => {
    const t = setTimeout(() => { setEmail(busca.trim()); setPagina(0); }, 250);
    return () => clearTimeout(t);
  }, [busca]);

  const q = useQuery({
    queryKey: ['usuarias', pagina, email],
    placeholderData: keepPreviousData,
    queryFn: () => {
      const p = new URLSearchParams({ page: String(pagina), size: '20' });
      if (email) p.set('email', email);
      return get<Pagina<Usuario>>(`/admin/usuarios?${p}`);
    },
  });

  async function alternar(u: Usuario) {
    setMudando(u.id);
    try {
      await patch(`/admin/usuarios/${u.id}`, { ativo: !u.ativo });
      aviso(u.ativo ? `${u.nome} desativada. As sessões dela foram encerradas.` : `${u.nome} reativada.`);
      await queryClient.invalidateQueries({ queryKey: ['usuarias'] });
    } catch (e) {
      aviso((e as Error).message, 'erro');
    } finally {
      setMudando(null);
    }
  }

  return (
    <div>
      <Cabecalho titulo="Usuárias" descricao="Contas da plataforma. A administração não vê receitas, preços nem estoque das confeiteiras.">
        <input type="search" className="busca" placeholder="Buscar por e-mail" aria-label="Buscar por e-mail" value={busca} onChange={(e) => setBusca(e.target.value)} />
      </Cabecalho>
      <section className="painel">
        {q.isPending ? <Carregando /> : q.isError ? <ErroCarregar erro={q.error} /> : (
          <>
            <div className="tabela-wrap">
              <table>
                <thead><tr>{['Nome', 'E-mail', 'Perfil', 'Criada em', 'Situação'].map((t) => <th key={t}>{t}</th>)}<th><span className="sr-only">Ações</span></th></tr></thead>
                <tbody>
                  {q.data.content.map((u) => (
                    <tr key={u.id}>
                      <td>{u.nome}</td>
                      <td>{u.email}</td>
                      <td><span className="selo">{u.perfil === 'ADMIN' ? 'admin' : 'confeiteira'}</span></td>
                      <td className="num">{dataHora(u.criadoEm)}</td>
                      <td>{u.ativo ? <span className="selo ok">ativa</span> : <span className="selo perigo">desativada</span>}</td>
                      <td className="acoes-linha">
                        {u.id === eu.id ? <span className="small muted">você</span> : (
                          <button type="button" className={u.ativo ? 'perigo' : 'discreto'} disabled={mudando === u.id} onClick={() => void alternar(u)}>
                            {u.ativo ? 'Desativar' : 'Reativar'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Paginacao pagina={q.data} irPara={setPagina} />
          </>
        )}
      </section>
    </div>
  );
}
