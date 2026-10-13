import { keepPreviousData, queryOptions, useQuery } from '@tanstack/react-query';
import { consultarStock, listarMovimientos } from '@/api/stock';
import type { FiltroStock } from '@/types/stock';

// Todo lo del inventario de una tabla cuelga de la misma clave: al registrar un movimiento se
// refresca el stock y la lista de movimientos juntos.
export const stockKeys = {
  bloque: (bloqueId: string) => ['stock', bloqueId] as const,
  lista: (bloqueId: string, filtro: FiltroStock) => ['stock', bloqueId, 'lista', filtro] as const,
  movimientos: (bloqueId: string) => ['stock', bloqueId, 'movimientos'] as const,
};

export const stockQuery = (bloqueId: string, filtro: FiltroStock) =>
  queryOptions({
    queryKey: stockKeys.lista(bloqueId, filtro),
    queryFn: ({ signal }) => consultarStock(bloqueId, filtro, signal),
    placeholderData: keepPreviousData,
  });

export const movimientosQuery = (bloqueId: string) =>
  queryOptions({
    queryKey: stockKeys.movimientos(bloqueId),
    queryFn: ({ signal }) => listarMovimientos(bloqueId, undefined, signal),
  });

export function useStock(bloqueId: string, filtro: FiltroStock) {
  return useQuery(stockQuery(bloqueId, filtro));
}

export function useMovimientos(bloqueId: string) {
  return useQuery(movimientosQuery(bloqueId));
}
