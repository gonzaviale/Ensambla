import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerConfiguracionNegocio } from '@/api/negocio';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

export const configuracionNegocioKey = ['negocio', 'configuracion'] as const;

export const configuracionNegocioQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: [...configuracionNegocioKey, usuarioId],
    queryFn: ({ signal }) => obtenerConfiguracionNegocio(signal),
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnMount: 'always',
  });

export function useConfiguracionNegocio() {
  const usuario = useUsuarioOpcional();
  return useQuery({
    ...configuracionNegocioQuery(usuario?.id ?? 'sin-sesion'),
    enabled: !!usuario,
  });
}

export function useFaltaConfiguracionNegocio() {
  const consulta = useConfiguracionNegocio();
  return !!consulta.data && !consulta.data.ubicacion;
}
