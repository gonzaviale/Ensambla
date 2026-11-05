import { useMutation } from '@tanstack/react-query';
import { ApiError } from '@/api/apiError';
import { redactarPromocion } from '@/api/promociones';

/** Pide el texto de una promoción para un canal; el texto llega editable y nada se publica solo. */
export function useRedactarPromocion() {
  const mutacion = useMutation({ mutationFn: redactarPromocion, retry: false });
  return {
    redactar: mutacion.mutate,
    isPending: mutacion.isPending,
    isError: mutacion.isError,
    mensajeError:
      mutacion.error instanceof ApiError
        ? mutacion.error.message
        : 'No pudimos escribir la promoción. Probá nuevamente.',
  };
}
