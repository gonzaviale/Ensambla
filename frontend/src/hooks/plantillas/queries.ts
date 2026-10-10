import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { listarPlantillas } from '@/api/plantillas';

export const plantillasKeys = {
  all: ['plantillas'] as const,
};

export const plantillasQuery = () =>
  queryOptions({
    queryKey: plantillasKeys.all,
    queryFn: ({ signal }) => listarPlantillas(signal),
    staleTime: 5 * 60_000,
  });

export function usePlantillas() {
  return useSuspenseQuery(plantillasQuery()).data;
}
