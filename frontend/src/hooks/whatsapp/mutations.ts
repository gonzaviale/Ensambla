import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  confirmarVerificacionWhatsapp,
  desvincularWhatsapp,
  generarCodigoVinculacion,
  solicitarVerificacionWhatsapp,
} from '@/api/whatsapp';
import { sesionKeys, useUsuarioOpcional } from '@/hooks/auth/queries';
import type { Usuario } from '@/types/auth';
import { vinculacionWhatsappKey } from './queries';

// Sólo se escribe en el caché si la sesión sigue siendo la que hizo el pedido.
function useClaveDelUsuario() {
  const queryClient = useQueryClient();
  const usuario = useUsuarioOpcional();
  return () =>
    usuario && queryClient.getQueryData<Usuario | null>(sesionKeys.actual)?.id === usuario.id
      ? ([...vinculacionWhatsappKey, usuario.id] as const)
      : null;
}

export function useSolicitarVerificacionWhatsapp() {
  return useMutation({ mutationFn: solicitarVerificacionWhatsapp });
}

export function useConfirmarVerificacionWhatsapp() {
  const queryClient = useQueryClient();
  const claveDelUsuario = useClaveDelUsuario();
  return useMutation({
    mutationFn: confirmarVerificacionWhatsapp,
    onSuccess: (vinculo) => {
      const clave = claveDelUsuario();
      if (clave) queryClient.setQueryData(clave, vinculo);
    },
  });
}

export function useGenerarCodigoVinculacion() {
  return useMutation({ mutationFn: generarCodigoVinculacion });
}

export function useDesvincularWhatsapp() {
  const queryClient = useQueryClient();
  const claveDelUsuario = useClaveDelUsuario();
  return useMutation({
    mutationFn: desvincularWhatsapp,
    onSuccess: () => {
      const clave = claveDelUsuario();
      if (clave) queryClient.setQueryData(clave, null);
    },
  });
}
