import { useMutation, useQueryClient } from '@tanstack/react-query';
import { eliminarRegistro } from '@/api/registros';
import { registrosKeys } from './queries';

export function useEliminarRegistro(bloqueId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: eliminarRegistro,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: registrosKeys.bloque(bloqueId) }),
  });
}
