import { keepPreviousData, queryOptions, useQuery } from '@tanstack/react-query';
import { listarRegistros } from '@/api/registros';
import type { FiltroRegistros } from '@/types/registro';

export const registrosKeys = {
  bloque: (bloqueId: string) => ['registros', bloqueId] as const,
  lista: (bloqueId: string, filtro: FiltroRegistros) => ['registros', bloqueId, filtro] as const,
};

// Al buscar, ordenar o paginar se conserva la página anterior mientras llega la nueva.
export const registrosQuery = (bloqueId: string, filtro: FiltroRegistros) =>
  queryOptions({
    queryKey: registrosKeys.lista(bloqueId, filtro),
    queryFn: ({ signal }) => listarRegistros(bloqueId, filtro, signal),
    placeholderData: keepPreviousData,
  });

export function useRegistros(bloqueId: string, filtro: FiltroRegistros) {
  return useQuery(registrosQuery(bloqueId, filtro));
}
