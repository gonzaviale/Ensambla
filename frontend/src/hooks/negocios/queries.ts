import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { listarNegocios } from '@/api/negocios';

export const negociosKeys = {
  all: ['negocios'] as const,
  lista: ['negocios', 'lista'] as const,
};

export const negociosQuery = () =>
  queryOptions({
    queryKey: negociosKeys.lista,
    queryFn: ({ signal }) => listarNegocios(signal),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

export function useNegociosDeLaCuenta() {
  return useSuspenseQuery(negociosQuery());
}

export function useNegocioActivo() {
  const { data } = useNegociosDeLaCuenta();
  return data.negocios.find((negocio) => negocio.id === data.activoId) ?? data.negocios[0];
}
