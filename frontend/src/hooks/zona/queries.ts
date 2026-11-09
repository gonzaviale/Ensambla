import { queryOptions, useMutation, useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { analizarZona, obtenerZona } from '@/api/zona';
import type { VistaDeZona } from '@/types/zona';

export const zonaKeys = {
  all: ['zona'] as const,
  vista: (vista: VistaDeZona) => ['zona', vista] as const,
};

export const zonaQuery = (vista: VistaDeZona = 'precios') =>
  queryOptions({
    queryKey: zonaKeys.vista(vista),
    queryFn: ({ signal }) => obtenerZona(vista, signal),
    staleTime: 5 * 60_000,
  });

/**
 * La zona de la persona, ya agregada y anónima, como la quiere ver «Precios»: primero lo que ya tiene. Es la que se
 * trae al entrar a la app; la de «Qué se vende cerca» se pide recién cuando se abre esa pestaña.
 */
export function useZona() {
  return useSuspenseQuery(zonaQuery()).data;
}

/**
 * La zona como la quiere ver «Qué se vende cerca»: primero lo más vendido que todavía no se tiene, con su propio
 * cupo del plan gratuito. Sólo se pide si `habilitada`, así abrir otra pestaña no gasta este cupo.
 */
export const useZonaDeLoQueSeVende = (habilitada = true) =>
  useQuery({ ...zonaQuery('cerca'), enabled: habilitada });

/** El análisis de otra zona que la persona escribe (un barrio o una dirección). */
export const useAnalisisDeZona = () => useMutation({ mutationFn: analizarZona });
