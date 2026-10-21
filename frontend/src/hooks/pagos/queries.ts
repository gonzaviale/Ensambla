import { useQuery } from '@tanstack/react-query';
import { consultarRenovacion } from '@/api/pagos';
import { ApiError } from '@/api/apiError';

export const pagosKeys = { all: ['pagos'] as const, renovacion: ['pagos', 'renovacion'] as const };

export function useRenovacion() {
  const consulta = useQuery({
    queryKey: pagosKeys.renovacion,
    queryFn: ({ signal }) => consultarRenovacion(signal),
    staleTime: 0,
    retry: false,
    refetchOnWindowFocus: 'always',
    refetchInterval: 60_000,
  });
  return {
    data: consulta.data,
    isPending: consulta.isPending,
    isError: consulta.isError,
    isFetching: consulta.isFetching,
    refetch: consulta.refetch,
    mensajeError:
      consulta.error instanceof ApiError
        ? consulta.error.message
        : 'No pudimos consultar la renovación.',
  };
}
