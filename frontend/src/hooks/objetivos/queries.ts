import { queryOptions, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import {
  listarObjetivos,
  obtenerDecisionesObjetivo,
  obtenerMiniObjetivos,
  obtenerPasosObjetivo,
  obtenerProgresoObjetivo,
  obtenerSemanasObjetivo,
} from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { useComoVieneTuNegocio } from '@/hooks/objetivos/useComoVieneTuNegocio';
import { conHistorialReal, objetivoDesdeServidor, type Objetivo } from '@/lib/objetivos';

export const objetivosKey = ['objetivos'] as const;

export const objetivosKeys = {
  pasos: [...objetivosKey, 'pasos'] as const,
  /** Todo lo de un objetivo cuelga de esta clave: al decidir algo se invalida junta. */
  detalle: (objetivoId: string) => [...objetivosKey, 'detalle', objetivoId] as const,
  minis: (objetivoId: string) => [...objetivosKeys.detalle(objetivoId), 'minis'] as const,
  progreso: (objetivoId: string) => [...objetivosKeys.detalle(objetivoId), 'progreso'] as const,
  decisiones: (objetivoId: string) => [...objetivosKeys.detalle(objetivoId), 'decisiones'] as const,
  semanas: (objetivoId: string) => [...objetivosKeys.detalle(objetivoId), 'semanas'] as const,
};

// Los objetivos son de la cuenta: la clave lleva a la persona para no mostrarle los de otra sesión.
export const objetivosQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: [...objetivosKey, usuarioId],
    queryFn: ({ signal }) => listarObjetivos(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

const SIN_OBJETIVOS: Objetivo[] = [];

/**
 * Los objetivos de la persona, del más viejo al más nuevo, con el resultado real de las últimas semanas. Vacío
 * mientras carga o si el servidor falla; sin ventas (o mientras llega cómo viene el negocio) quedan sin historial.
 */
export function useObjetivos() {
  const usuario = useUsuarioOpcional();
  const consulta = useQuery({
    ...objetivosQuery(usuario?.id ?? 'sin-sesion'),
    enabled: !!usuario,
  });
  const { negocio } = useComoVieneTuNegocio();
  const semanas = negocio?.semanas;
  const objetivos = useMemo(
    () =>
      consulta.data
        ? consulta.data.map((guardado) =>
            conHistorialReal(objetivoDesdeServidor(guardado), semanas ?? []),
          )
        : SIN_OBJETIVOS,
    [consulta.data, semanas],
  );
  return { objetivos, cargando: consulta.isPending, fallo: consulta.isError };
}

export const pasosObjetivoQuery = () =>
  queryOptions({
    queryKey: objetivosKeys.pasos,
    queryFn: ({ signal }) => obtenerPasosObjetivo(signal),
    staleTime: 5 * 60_000,
  });

export function usePasosObjetivo() {
  return useQuery(pasosObjetivoQuery());
}

// --- Objetivos que se autogestionan: lo que Sami propone, mide y reajusta cada domingo ---

const DATOS_DEL_OBJETIVO = { retry: false, refetchOnWindowFocus: false } as const;

export function useMiniObjetivos(objetivoId: string) {
  return useQuery({
    queryKey: objetivosKeys.minis(objetivoId),
    queryFn: ({ signal }) => obtenerMiniObjetivos(objetivoId, signal),
    ...DATOS_DEL_OBJETIVO,
  });
}

export function useProgresoObjetivo(objetivoId: string) {
  return useQuery({
    queryKey: objetivosKeys.progreso(objetivoId),
    queryFn: ({ signal }) => obtenerProgresoObjetivo(objetivoId, signal),
    ...DATOS_DEL_OBJETIVO,
  });
}

export function useDecisionesObjetivo(objetivoId: string) {
  return useQuery({
    queryKey: objetivosKeys.decisiones(objetivoId),
    queryFn: ({ signal }) => obtenerDecisionesObjetivo(objetivoId, signal),
    ...DATOS_DEL_OBJETIVO,
  });
}

export function useSemanasObjetivo(objetivoId: string) {
  return useQuery({
    queryKey: objetivosKeys.semanas(objetivoId),
    queryFn: ({ signal }) => obtenerSemanasObjetivo(objetivoId, signal),
    ...DATOS_DEL_OBJETIVO,
  });
}
