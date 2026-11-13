import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerComoVieneTuNegocio } from '@/api/comoVieneTuNegocio';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

// Los números son del negocio de la cuenta: la clave lleva a la persona para no mostrarle los de otra sesión.
export const comoVieneTuNegocioQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: ['objetivos', 'negocio', usuarioId] as const,
    queryFn: ({ signal }) => obtenerComoVieneTuNegocio(signal),
    // Las ventas cambian mientras se cargan las secciones: se vuelve a preguntar al volver a Objetivos.
    staleTime: 30_000,
    retry: false,
  });

/**
 * Cómo viene el negocio según el servidor. Sin sesión, mientras carga o si falla no hay datos, y la pantalla no
 * muestra números inventados.
 */
export function useComoVieneTuNegocio() {
  const usuario = useUsuarioOpcional();
  const consulta = useQuery({
    ...comoVieneTuNegocioQuery(usuario?.id ?? 'sin-sesion'),
    enabled: !!usuario,
  });
  return { negocio: consulta.data, cargando: consulta.isPending, fallo: consulta.isError };
}
