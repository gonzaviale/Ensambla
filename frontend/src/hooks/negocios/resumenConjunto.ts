import { queryOptions, useQuery } from '@tanstack/react-query';
import { DIAS_DEL_RESUMEN_CONJUNTO, obtenerResumenConjunto } from '@/api/negocios';
import { negociosKeys } from './queries';

/** Lo vendido, gastado y pendiente de todos los negocios de la cuenta en los últimos 30 días. */
export const resumenConjuntoQuery = () =>
  queryOptions({
    queryKey: [...negociosKeys.resumen, DIAS_DEL_RESUMEN_CONJUNTO] as const,
    queryFn: ({ signal }) => obtenerResumenConjunto(DIAS_DEL_RESUMEN_CONJUNTO, signal),
    retry: false,
  });

export const useResumenConjunto = () => useQuery(resumenConjuntoQuery());
