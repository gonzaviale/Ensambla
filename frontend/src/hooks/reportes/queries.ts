import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerResumenInicio } from '@/api/resumen';

export const resumenKeys = {
  inicio: (sistemaId: string) => ['resumen', 'inicio', sistemaId] as const,
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
