import { sesionExpiradaConfirmada } from '@/api/auth';
import { guardarBorradorSesion } from '@/lib/borradorSesion';
import { useSistemaStore } from '@/stores/sistemaStore';
import { useEffect } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { RouterProvider } from '@tanstack/react-router';
import { EVENTO_SESION_EXPIRADA } from '@/api/client';
import { sesionKeys } from '@/hooks/auth/queries';
import { queryClient } from './queryClient';
import { router } from './router';

// Si el backend responde 401 en una pantalla privada, la sesión venció: se limpia y se manda al login.
function useSesionExpirada() {
  useEffect(() => {
    let comprobando = false;
    const alExpirar = async () => {
      if (comprobando) return;
      comprobando = true;
      try {
        // Una respuesta atrasada no invalida una sesión nueva o todavía vigente.
        if (!(await sesionExpiradaConfirmada())) return;
      } catch {
        return;
      } finally {
        comprobando = false;
      }
      guardarBorradorSesion(useSistemaStore.getState());
      queryClient.setQueryData(sesionKeys.actual, null);
      void router
        .navigate({
          to: '/login',
          search: { redirect: router.state.location.href },
        })
        .then(() =>
          queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'auth' }),
        );
    };
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
  }, []);
}

export function Providers() {
  useSesionExpirada();

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      {import.meta.env.DEV && <ReactQueryDevtools buttonPosition="bottom-left" />}
    </QueryClientProvider>
  );
}
