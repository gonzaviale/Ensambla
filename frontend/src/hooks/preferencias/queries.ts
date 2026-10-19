import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { obtenerPreferencias } from '@/api/sistemas';

export const preferenciasKeys = {
  all: ['preferencias'] as const,
};

export const preferenciasQuery = () =>
  queryOptions({
    queryKey: preferenciasKeys.all,
    queryFn: ({ signal }) => obtenerPreferencias(signal),
    staleTime: Infinity,
  });

export function usePreferencias() {
  return useSuspenseQuery(preferenciasQuery()).data;
}
