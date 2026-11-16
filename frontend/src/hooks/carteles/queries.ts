import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerPromocionesSugeridas } from '@/api/carteles';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

export const promocionesSugeridasKey = ['carteles', 'promociones-sugeridas'] as const;

export const promocionesSugeridasQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: [...promocionesSugeridasKey, usuarioId],
    queryFn: ({ signal }) => obtenerPromocionesSugeridas(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

export function usePromocionesSugeridas() {
  const usuario = useUsuarioOpcional();
  return useQuery({
    ...promocionesSugeridasQuery(usuario?.id ?? 'sin-sesion'),
    enabled: !!usuario,
  });
}
