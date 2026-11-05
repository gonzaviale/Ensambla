import { queryOptions, useQuery } from '@tanstack/react-query';
import { consultarReposicion } from '@/api/reposicion';

export const reposicionKeys = {
  de: (sistemaId: string) => ['reposicion', sistemaId] as const,
};

export const reposicionQuery = (sistemaId: string) =>
  queryOptions({
    queryKey: reposicionKeys.de(sistemaId),
    queryFn: ({ signal }) => consultarReposicion(sistemaId, signal),
    // El stock cambia con cada venta y cada entrada: al abrir el pedido siempre se vuelve a mirar.
    staleTime: 0,
    retry: false,
  });

/** Lo que hay que reponer en el sistema abierto, agrupado por proveedor. */
export function useReposicion(sistemaId: string) {
  return useQuery(reposicionQuery(sistemaId));
}
