import { queryOptions, useQuery } from '@tanstack/react-query';
import { listarAvisosPendientes } from '@/api/avisos';
import type { AvisoDelServidor } from '@/types/avisos';

export const avisosKeys = {
  all: ['avisos'] as const,
  pendientes: ['avisos', 'pendientes'] as const,
};

export const avisosPendientesQuery = () =>
  queryOptions({
    queryKey: avisosKeys.pendientes,
    queryFn: ({ signal }) => listarAvisosPendientes(signal),
    // El motor corre solo en el servidor: se vuelve a preguntar cada tanto sin que la persona lo pida.
    refetchInterval: 60_000,
    // Si el servidor no responde, los avisos que Sami calcula en el navegador siguen funcionando.
    retry: false,
  });

const SIN_AVISOS: AvisoDelServidor[] = [];

/** Los avisos pendientes del motor del servidor; vacío mientras carga o si falla. */
export function useAvisosDelServidor() {
  return useQuery(avisosPendientesQuery()).data ?? SIN_AVISOS;
}
