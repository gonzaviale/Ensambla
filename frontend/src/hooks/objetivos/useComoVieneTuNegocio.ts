import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerComoVieneTuNegocio } from '@/api/comoVieneTuNegocio';

export const comoVieneTuNegocioKey = ['objetivos', 'negocio'] as const;

export const comoVieneTuNegocioQuery = () =>
  queryOptions({
    queryKey: comoVieneTuNegocioKey,
    queryFn: ({ signal }) => obtenerComoVieneTuNegocio(signal),
    // Las ventas cambian mientras se cargan los cuadernos: se vuelve a preguntar al volver a Objetivos.
    staleTime: 30_000,
    retry: false,
  });

/**
 * Cómo viene el negocio según el servidor. Mientras carga o si falla no hay datos, y la pantalla no muestra números
 * inventados.
 */
export function useComoVieneTuNegocio() {
  const consulta = useQuery(comoVieneTuNegocioQuery());
  return { negocio: consulta.data, cargando: consulta.isPending, fallo: consulta.isError };
}
