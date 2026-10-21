import { useMutation, useQueryClient } from '@tanstack/react-query';
import { actualizarNegocio } from '@/api/negocio';
import { configuracionNegocioKey } from './queries';
import { useUsuarioOpcional, sesionKeys } from '@/hooks/auth/queries';
import type { Usuario } from '@/types/auth';

export function useActualizarNegocio() {
  const queryClient = useQueryClient();
  const usuario = useUsuarioOpcional();
  return useMutation({
    mutationFn: actualizarNegocio,
    onSuccess: (configuracion) => {
      if (usuario && queryClient.getQueryData<Usuario | null>(sesionKeys.actual)?.id === usuario.id)
        queryClient.setQueryData([...configuracionNegocioKey, usuario.id], configuracion);
    },
  });
}
