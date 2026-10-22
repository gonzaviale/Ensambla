import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { listarSistemas } from '@/api/sistemas';

export const sistemasKeys = {
  all: ['sistemas'] as const,
};

// Los sistemas se traen una vez por sesión: de ahí en más la pantalla trabaja sobre su copia (el store)
// y la guarda en el servidor. Por eso no se vuelven a pedir solos: pisarían lo que se está editando.
export const sistemasQuery = () =>
  queryOptions({
    queryKey: sistemasKeys.all,
    queryFn: ({ signal }) => listarSistemas(signal),
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnReconnect: false,
  });

export function useSistemasDelServidor() {
  return useSuspenseQuery(sistemasQuery()).data;
}
