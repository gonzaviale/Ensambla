import { useMutation, useQueryClient } from '@tanstack/react-query';
import { registrarMovimiento } from '@/api/stock';
import type { NuevoMovimiento } from '@/types/stock';
import { stockKeys } from './queries';

export function useRegistrarMovimiento(bloqueId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (movimiento: NuevoMovimiento) => registrarMovimiento(bloqueId, movimiento),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: stockKeys.bloque(bloqueId) }),
  });
}
