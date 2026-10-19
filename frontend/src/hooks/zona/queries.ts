import { queryOptions, useMutation, useSuspenseQuery } from '@tanstack/react-query';
import { analizarZona, obtenerZona } from '@/api/zona';

export const zonaKeys = {
  all: ['zona'] as const,
};

export const zonaQuery = () =>
  queryOptions({
    queryKey: zonaKeys.all,
    queryFn: ({ signal }) => obtenerZona(signal),
    staleTime: 5 * 60_000,
  });

/** La zona de la persona, ya agregada y anónima. */
export function useZona() {
  return useSuspenseQuery(zonaQuery()).data;
}

/** El análisis de otra zona que la persona escribe (un barrio o una dirección). */
export const useAnalisisDeZona = () => useMutation({ mutationFn: analizarZona });
