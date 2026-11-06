import { keepPreviousData, queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerMetricas, obtenerResumenInicio } from '@/api/resumen';
import type { PeriodoDeVentas } from '@/lib/sistema/metricasInicio';

const EN_EL_SERVIDOR = { hoy: 'HOY', semana: 'SEMANA', mes: 'MES' } as const;

export const resumenKeys = {
  inicio: (sistemaId: string) => ['resumen', 'inicio', sistemaId] as const,
  metricas: (sistemaId: string, periodo: PeriodoDeVentas) =>
    ['resumen', 'metricas', sistemaId, periodo] as const,
};

export const resumenInicioQuery = (sistemaId: string) =>
  queryOptions({
    queryKey: resumenKeys.inicio(sistemaId),
    queryFn: ({ signal }) => obtenerResumenInicio(sistemaId, signal),
    // Las ventas cambian mientras se carga el cuaderno: se vuelve a preguntar al volver al inicio.
    staleTime: 30_000,
    retry: false,
  });

/** Las métricas del inicio del cuaderno elegido; sin cuaderno no se pide nada. */
export function useResumenInicio(sistemaId: string | undefined) {
  const consulta = useQuery({
    ...resumenInicioQuery(sistemaId ?? ''),
    enabled: sistemaId !== undefined,
  });
  return { resumen: consulta.data, fallo: consulta.isError, reintentar: consulta.refetch };
}

/** Las métricas detalladas del cuaderno y el período elegidos. Al cambiar de período se ven las anteriores hasta que llegan las nuevas. */
export function useMetricas(sistemaId: string | undefined, periodo: PeriodoDeVentas) {
  const consulta = useQuery({
    queryKey: resumenKeys.metricas(sistemaId ?? '', periodo),
    queryFn: ({ signal }) => obtenerMetricas(sistemaId ?? '', EN_EL_SERVIDOR[periodo], signal),
    enabled: sistemaId !== undefined,
    staleTime: 30_000,
    retry: false,
    placeholderData: keepPreviousData,
  });
  return {
    metricas: consulta.data,
    cargando: consulta.isPending && sistemaId !== undefined,
    actualizando: consulta.isPlaceholderData,
    fallo: consulta.isError,
    reintentar: consulta.refetch,
  };
}
