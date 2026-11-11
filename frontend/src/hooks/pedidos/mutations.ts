import { useMutation, useQueryClient } from '@tanstack/react-query';
import { cambiarEstadoDeOrden } from '@/api/pedidos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import type { CambioDeEstado, OrdenDelServidor } from '@/types/pedidos';
import { pedidosKey } from './queries';

/** El dueño lleva una orden a la etapa siguiente; la lista se actualiza con lo que respondió el servidor. */
export function useCambiarEstadoDePedido(id: string) {
  const queryClient = useQueryClient();
  const usuario = useUsuarioOpcional();
  const clave = [...pedidosKey, usuario?.id ?? 'sin-sesion'] as const;
  return useMutation({
    mutationFn: (cambio: CambioDeEstado) => cambiarEstadoDeOrden(id, cambio),
    onSuccess: (actualizada) => {
      queryClient.setQueryData<OrdenDelServidor[]>(clave, (actuales) =>
        actuales?.map((orden) => (orden.id === actualizada.id ? actualizada : orden)),
      );
    },
    // Si el servidor dice que no corresponde (otro paso, sin stock), se vuelve a preguntar para mostrar lo que hay de verdad.
    onError: () => queryClient.invalidateQueries({ queryKey: clave }),
  });
}
