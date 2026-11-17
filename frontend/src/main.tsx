import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ApiError } from './api/client';
import { App } from './App';
import { SessaoProvider } from './auth/sessao';
import { ToastsProvider } from './components/Toasts';
import './styles.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 10_000,
      // 4xx answers will not change by retrying; network hiccups and 5xx get one more try.
      retry: (falhas, erro) => !(erro instanceof ApiError && erro.status >= 400 && erro.status < 500) && falhas < 1,
    },
  },
});

createRoot(document.getElementById('app')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ToastsProvider>
        <SessaoProvider>
          <App />
        </SessaoProvider>
      </ToastsProvider>
    </QueryClientProvider>
  </StrictMode>,
);
