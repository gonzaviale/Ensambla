import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { obtenerUsuarioActual } from '@/api/auth';

export const sesionKeys = {
  actual: ['auth', 'actual'] as const,
};

export const sesionQuery = () =>
  queryOptions({
    queryKey: sesionKeys.actual,
    queryFn: ({ signal }) => obtenerUsuarioActual(signal),
    staleTime: 5 * 60_000,
  });

// Para pantallas que viven detrás del guard de sesión: el usuario siempre existe.
export function useUsuario() {
  const { data } = useSuspenseQuery(sesionQuery());
  if (!data) throw new Error('No hay una sesión activa');
  return data;
}

// Para pantallas públicas: puede no haber sesión.
export function useUsuarioOpcional() {
  return useSuspenseQuery(sesionQuery()).data;
}
