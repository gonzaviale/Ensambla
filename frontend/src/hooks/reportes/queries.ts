import { keepPreviousData, queryOptions, useQuery } from '@tanstack/react-query';
import { generarReporte } from '@/api/reportes';
import type { FiltroReporte } from '@/types/reporte';

export const reportesKeys = {
  bloque: (bloqueId: string) => ['reportes', bloqueId] as const,
  filtro: (bloqueId: string, filtro: FiltroReporte) => ['reportes', bloqueId, filtro] as const,
};

// Un reporte es una foto de los datos en el momento de pedirlo: cada vez que se abre se vuelve a calcular.
export const reporteQuery = (bloqueId: string, filtro: FiltroReporte) =>
  queryOptions({
    queryKey: reportesKeys.filtro(bloqueId, filtro),
    queryFn: ({ signal }) => generarReporte(bloqueId, filtro, signal),
    placeholderData: keepPreviousData,
    staleTime: 0,
  });

/** El reporte de una tabla con los filtros ya aplicados; sin tabla no pide nada. */
export function useReporte(bloqueId: string, filtro: FiltroReporte) {
  return useQuery({ ...reporteQuery(bloqueId, filtro), enabled: Boolean(bloqueId) });
}
