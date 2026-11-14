import { queryOptions, useQuery } from '@tanstack/react-query';
import { listarMetricas, listarMetricasSugeridas } from '@/api/metricas';

export const metricasKeys = {
  /** Todo lo de un cuaderno cuelga de esta clave: al guardar o quitar una se vuelve a pedir junto. */
  cuaderno: (sistemaId: string) => ['metricas', sistemaId] as const,
  lista: (sistemaId: string) => [...metricasKeys.cuaderno(sistemaId), 'lista'] as const,
  sugeridas: (sistemaId: string) => [...metricasKeys.cuaderno(sistemaId), 'sugeridas'] as const,
};

export const metricasQuery = (sistemaId: string) =>
  queryOptions({
    queryKey: metricasKeys.lista(sistemaId),
    queryFn: ({ signal }) => listarMetricas(sistemaId, signal),
    // Los números cambian mientras se carga el cuaderno: se vuelve a preguntar al volver al inicio.
    staleTime: 30_000,
    retry: false,
  });

export const metricasSugeridasQuery = (sistemaId: string) =>
  queryOptions({
    queryKey: metricasKeys.sugeridas(sistemaId),
    queryFn: ({ signal }) => listarMetricasSugeridas(sistemaId, signal),
    staleTime: 30_000,
    retry: false,
  });

/** Las métricas guardadas en el cuaderno elegido, cada una con lo que da hoy. */
export function useMetricasAMedida(sistemaId: string) {
  const consulta = useQuery(metricasQuery(sistemaId));
  return {
    metricas: consulta.data,
    fallo: consulta.isError,
    reintentar: consulta.refetch,
  };
}

/** Las sugeridas del rubro; sólo se piden cuando la persona abre «Agregar métrica». */
export function useMetricasSugeridas(sistemaId: string, habilitado: boolean) {
  const consulta = useQuery({ ...metricasSugeridasQuery(sistemaId), enabled: habilitado });
  return {
    sugeridas: consulta.data,
    cargando: consulta.isPending && habilitado,
    fallo: consulta.isError,
    reintentar: consulta.refetch,
  };
}
