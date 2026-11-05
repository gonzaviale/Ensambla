import { useMutation } from '@tanstack/react-query';
import { ApiError } from '@/api/apiError';
import { descargarPedidoProveedor } from '@/api/reposicion';
import type { PedidoAlProveedor } from '@/types/reposicion';

/** Pide el PDF del pedido; qué se hace con el archivo (descargarlo o compartirlo) lo decide quien llama. */
export function useArmarPedidoProveedor() {
  const mutacion = useMutation({
    mutationFn: ({ sistemaId, pedido }: { sistemaId: string; pedido: PedidoAlProveedor }) =>
      descargarPedidoProveedor(sistemaId, pedido),
    retry: false,
  });
  // Si falla, el error queda en `isError`/`mensajeError` y el resultado es `null`: quien llama no tiene que atraparlo.
  const armar = (variables: { sistemaId: string; pedido: PedidoAlProveedor }) =>
    mutacion.mutateAsync(variables).catch(() => null);
  return {
    armar,
    isPending: mutacion.isPending,
    isError: mutacion.isError,
    mensajeError:
      mutacion.error instanceof ApiError
        ? mutacion.error.message
        : 'No pudimos armar el pedido. Probá nuevamente.',
  };
}
