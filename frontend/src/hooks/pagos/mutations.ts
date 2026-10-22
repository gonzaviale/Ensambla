import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { actualizarPagos, cancelarRenovacion, contratarPremium } from '@/api/pagos';
import { ApiError } from '@/api/apiError';
import { planesKeys } from '@/hooks/planes/queries';
import { pagosKeys } from './queries';

export const useOperacionPagoPendiente = () => useIsMutating({ mutationKey: pagosKeys.all }) > 0;

export function useOperacionPago(operacion: 'contratar' | 'cancelar' | 'actualizar') {
  const queryClient = useQueryClient();
  const mutacion = useMutation({
    mutationKey: [...pagosKeys.all, operacion],
    mutationFn: async () => {
      if (operacion === 'actualizar') return actualizarPagos();
      return operacion === 'contratar' ? contratarPremium() : cancelarRenovacion();
    },
    retry: false,
    // La lectura posterior usa la sesión actual, incluso si la cuenta cambió durante la operación.
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: pagosKeys.all }),
        queryClient.invalidateQueries({ queryKey: planesKeys.all }),
      ]);
    },
  });
  return {
    mutate: mutacion.mutate,
    isPending: mutacion.isPending,
    isError: mutacion.isError,
    mensajeError:
      mutacion.error instanceof ApiError
        ? mutacion.error.message
        : 'No pudimos confirmar la operación. Actualizá el estado.',
  };
}
