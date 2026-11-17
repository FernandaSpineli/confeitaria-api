import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { useUsuario, useSessao } from '../auth/sessao';

type ItemMenu = { para: string; rotulo: string } | { grupo: string };

const MENU_CONFEITEIRA: ItemMenu[] = [
  { para: '/', rotulo: 'Início' },
  { para: '/receitas', rotulo: 'Receitas e preços' },
  { para: '/producoes', rotulo: 'Produções' },
  { para: '/estoque', rotulo: 'Estoque' },
  { grupo: 'Cadastros' },
  { para: '/insumos', rotulo: 'Insumos' },
  { para: '/embalagens', rotulo: 'Embalagens' },
  { para: '/equipamentos', rotulo: 'Equipamentos' },
  { para: '/custos-fixos', rotulo: 'Custos fixos' },
  { para: '/canais', rotulo: 'Canais de venda' },
  { grupo: 'Ajustes' },
  { para: '/configuracao', rotulo: 'Meus custos' },
];
const MENU_ADMIN: ItemMenu[] = [{ para: '/', rotulo: 'Usuárias' }];

function Marca() {
  return <a className="marca" href="#/"><img src="/assets/favicon.svg" alt="" />Confeitaria</a>;
}

export function Layout() {
  const usuario = useUsuario();
  const { sair } = useSessao();
  const [menuAberto, setMenuAberto] = useState(false);
  const local = useLocation();
  const conteudo = useRef<HTMLElement>(null);
  const menu = usuario.perfil === 'ADMIN' ? MENU_ADMIN : MENU_CONFEITEIRA;

  // New screen: close the drawer, go to the top and move focus to the content for screen readers.
  useEffect(() => {
    setMenuAberto(false);
    window.scrollTo(0, 0);
    conteudo.current?.focus({ preventScroll: true });
  }, [local.pathname]);

  return (
    <div className={`shell${menuAberto ? ' menu-aberto' : ''}`}>
      <header className="topo-movel">
        <Marca />
        <button type="button" aria-label="Abrir menu" aria-expanded={menuAberto} onClick={() => setMenuAberto((v) => !v)}>Menu</button>
      </header>
      <nav className="lateral" aria-label="Principal">
        <Marca />
        <ul className="menu">
          {menu.map((item) => ('grupo' in item
            ? <li key={item.grupo} className="grupo">{item.grupo}</li>
            : <li key={item.para}><NavLink to={item.para} end={item.para === '/'}>{item.rotulo}</NavLink></li>))}
        </ul>
        <div className="conta">
          <strong>{usuario.nome}</strong>
          {usuario.email}
          <div><button type="button" onClick={() => void sair()}>Sair</button></div>
        </div>
      </nav>
      <main className="conteudo" id="conteudo" tabIndex={-1} ref={conteudo}>
        <Outlet />
      </main>
    </div>
  );
}
