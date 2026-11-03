import { useMutation } from '@tanstack/react-query';
import { ApiError } from '@/api/apiError';
import { descargarResumen, type PeriodoDelResumen } from '@/api/resumen';
import { guardarArchivo } from '@/lib/descargas';

export function useDescargarResumen() {
  const mutacion = useMutation({
    mutationFn: ({ sistemaId, periodo }: { sistemaId: string; periodo: PeriodoDelResumen }) =>
      descargarResumen(sistemaId, periodo),
    retry: false,
    onSuccess: guardarArchivo,
  });
  return {
    descargar: mutacion.mutate,
    isPending: mutacion.isPending,
    isError: mutacion.isError,
    mensajeError:
      mutacion.error instanceof ApiError
        ? mutacion.error.message
        : 'No pudimos armar el resumen. Probá nuevamente.',
  };
}
