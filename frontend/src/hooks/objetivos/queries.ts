import { queryOptions, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { listarObjetivos } from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { objetivoDesdeServidor, type Objetivo } from '@/lib/objetivos';

export const objetivosKey = ['objetivos'] as const;

// Los objetivos son de la cuenta: la clave lleva a la persona para no mostrarle los de otra sesión.
export const objetivosQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: [...objetivosKey, usuarioId],
    queryFn: ({ signal }) => listarObjetivos(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

const SIN_OBJETIVOS: Objetivo[] = [];

/** Los objetivos de la persona, del más viejo al más nuevo. Vacío mientras carga o si el servidor falla. */
export function useObjetivos() {
  const usuario = useUsuarioOpcional();
  const consulta = useQuery({
    ...objetivosQuery(usuario?.id ?? 'sin-sesion'),
    enabled: !!usuario,
  });
  const objetivos = useMemo(
    () => (consulta.data ? consulta.data.map(objetivoDesdeServidor) : SIN_OBJETIVOS),
    [consulta.data],
  );
  return { objetivos, cargando: consulta.isPending, fallo: consulta.isError };
}
