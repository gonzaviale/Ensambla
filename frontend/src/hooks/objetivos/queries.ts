import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerPasosObjetivo } from '@/api/objetivos';

export const objetivosKeys = {
  pasos: ['objetivos', 'pasos'] as const,
};

export const pasosObjetivoQuery = () =>
  queryOptions({
    queryKey: objetivosKeys.pasos,
    queryFn: ({ signal }) => obtenerPasosObjetivo(signal),
    staleTime: 5 * 60_000,
  });

export function usePasosObjetivo() {
  return useQuery(pasosObjetivoQuery());
}
