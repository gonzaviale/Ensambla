import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerVinculacionWhatsapp } from '@/api/whatsapp';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

export const vinculacionWhatsappKey = ['whatsapp', 'vinculacion'] as const;

// El número vinculado es de cada persona: la clave lleva el usuario para no mostrar el de otra sesión.
export const vinculacionWhatsappQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: [...vinculacionWhatsappKey, usuarioId],
    queryFn: ({ signal }) => obtenerVinculacionWhatsapp(signal),
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnMount: 'always',
  });

/** `null` si todavía no vinculó ningún número. */
export function useVinculacionWhatsapp() {
  const usuario = useUsuarioOpcional();
  return useQuery({
    ...vinculacionWhatsappQuery(usuario?.id ?? 'sin-sesion'),
    enabled: !!usuario,
  });
}
