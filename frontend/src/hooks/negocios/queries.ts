import { queryOptions } from '@tanstack/react-query';
import { listarNegocios } from '@/api/negocios';

export const negociosKeys = {
  all: ['negocios'] as const,
  lista: ['negocios', 'lista'] as const,
  resumen: ['negocios', 'resumen'] as const,
};

/** Los negocios de la cuenta, cuál está activo y cuántos permite el plan. */
export const negociosQuery = () =>
  queryOptions({
    queryKey: negociosKeys.lista,
    queryFn: ({ signal }) => listarNegocios(signal),
    retry: false,
  });
