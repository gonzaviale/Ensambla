import { queryOptions, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { listarObjetivos, obtenerPasosObjetivo } from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { useComoVieneTuNegocio } from '@/hooks/objetivos/useComoVieneTuNegocio';
import { conHistorialReal, objetivoDesdeServidor, type Objetivo } from '@/lib/objetivos';

export const objetivosKey = ['objetivos'] as const;

export const objetivosKeys = {
  pasos: [...objetivosKey, 'pasos'] as const,
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
