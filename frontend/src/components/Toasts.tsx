import { createContext, type ReactNode, useCallback, useContext, useRef, useState } from 'react';

type Tipo = 'ok' | 'erro';
interface Toast { id: number; texto: string; tipo: Tipo }

const Contexto = createContext<(texto: string, tipo?: Tipo) => void>(() => {});

/** Short confirmations in the corner; at most three at a time. */
export function ToastsProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const seq = useRef(0);
  const aviso = useCallback((texto: string, tipo: Tipo = 'ok') => {
    const id = ++seq.current;
    setToasts((t) => [...t, { id, texto, tipo }].slice(-3));
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), tipo === 'erro' ? 7000 : 4000);
  }, []);
  return (
    <Contexto.Provider value={aviso}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => <div key={t.id} className={`toast${t.tipo === 'erro' ? ' erro' : ''}`}>{t.texto}</div>)}
      </div>
    </Contexto.Provider>
  );
}

export const useAviso = () => useContext(Contexto);
