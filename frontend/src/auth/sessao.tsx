import { useQueryClient } from '@tanstack/react-query';
import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import * as api from '../api/client';
import type { Usuario } from '../api/types';

interface Sessao {
  usuario: Usuario | null;
  carregando: boolean;
  /** Shown on the login screen after the session expired. */
  aviso: string | null;
  entrar: (email: string, senha: string) => Promise<void>;
  cadastrar: (nome: string, email: string, senha: string) => Promise<void>;
  sair: () => Promise<void>;
}

const Contexto = createContext<Sessao | null>(null);

export function SessaoProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(api.temSessaoSalva());
  const [aviso, setAviso] = useState<string | null>(null);

  useEffect(() => {
    api.quandoSessaoExpirar(() => {
      setUsuario((atual) => {
        if (atual) setAviso('Sua sessão expirou. Entre novamente.');
        return null;
      });
      queryClient.clear();
    });
    if (!api.temSessaoSalva()) return;
    api.get<Usuario>('/me')
      .then(setUsuario)
      .catch(() => setUsuario(null))
      .finally(() => setCarregando(false));
  }, [queryClient]);

  const entrar = useCallback(async (email: string, senha: string) => {
    await api.entrar(email, senha);
    setAviso(null);
    setUsuario(await api.get<Usuario>('/me'));
  }, []);

  const cadastrar = useCallback(async (nome: string, email: string, senha: string) => {
    await api.cadastrar(nome, email, senha);
    await entrar(email, senha);
  }, [entrar]);

  const sair = useCallback(async () => {
    await api.sair();
    queryClient.clear();
    // The next account must not land on a screen (e.g. a recipe) that belonged to this one.
    window.location.hash = '/';
    setUsuario(null);
  }, [queryClient]);

  return (
    <Contexto.Provider value={{ usuario, carregando, aviso, entrar, cadastrar, sair }}>
      {children}
    </Contexto.Provider>
  );
}

export function useSessao(): Sessao {
  const s = useContext(Contexto);
  if (!s) throw new Error('useSessao fora do SessaoProvider');
  return s;
}

/** The logged-in user; only call inside screens that require a session. */
export function useUsuario(): Usuario {
  const { usuario } = useSessao();
  if (!usuario) throw new Error('sem sessão');
  return usuario;
}
