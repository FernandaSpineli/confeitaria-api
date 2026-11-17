import { HashRouter, Navigate, Route, Routes } from 'react-router';
import { useSessao } from './auth/sessao';
import { Layout } from './components/Layout';
import { Carregando } from './components/ui';
import { Usuarias } from './pages/admin/Usuarias';
import { Canais, CustosFixos, Embalagens, Equipamentos, Insumos } from './pages/cadastros/cadastros';
import { Configuracao } from './pages/Configuracao';
import { Entrada } from './pages/Entrada';
import { Estoque } from './pages/estoque/Estoque';
import { Producoes } from './pages/estoque/Producoes';
import { Painel } from './pages/Painel';
import { DetalheReceita } from './pages/receitas/DetalheReceita';
import { EditorReceita } from './pages/receitas/EditorReceita';
import { ListaReceitas } from './pages/receitas/ListaReceitas';

export function App() {
  const { usuario, carregando } = useSessao();
  if (carregando) return <Carregando />;
  if (!usuario) return <Entrada />;

  return (
    <HashRouter>
      {usuario.perfil === 'ADMIN' ? (
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Usuarias />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      ) : (
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Painel />} />
            <Route path="receitas" element={<ListaReceitas />} />
            <Route path="receitas/nova" element={<EditorReceita />} />
            <Route path="receitas/:id" element={<DetalheReceita />} />
            <Route path="receitas/:id/editar" element={<EditorReceita />} />
            <Route path="insumos" element={<Insumos />} />
            <Route path="embalagens" element={<Embalagens />} />
            <Route path="equipamentos" element={<Equipamentos />} />
            <Route path="custos-fixos" element={<CustosFixos />} />
            <Route path="canais" element={<Canais />} />
            <Route path="estoque" element={<Estoque />} />
            <Route path="producoes" element={<Producoes />} />
            <Route path="configuracao" element={<Configuracao />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      )}
    </HashRouter>
  );
}
