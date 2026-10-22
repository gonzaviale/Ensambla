import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { iniciarPruebaPremium } from '@/api/planes';
import { ApiError } from '@/api/apiError';
import type { Planes } from '@/types/planes';
import { planesKeys } from './queries';

export const useActivandoPrueba = () => useIsMutating({ mutationKey: planesKeys.prueba }) > 0;

export function useIniciarPrueba() {
  const queryClient = useQueryClient();
  const mutacion = useMutation({
    mutationKey: planesKeys.prueba,
    mutationFn: iniciarPruebaPremium,
    retry: false,
    onSuccess: (prueba) => {
      // Sólo se refleja premium después de que el servidor confirmó la activación.
      queryClient.setQueryData<Planes>(planesKeys.actual, (anterior) =>
        anterior
          ? {
              ...anterior,
              consultadoEn: prueba.inicioEn,
              suscripcion: {
                codigoPlanActual: 'PREMIUM',
                pruebaActiva: true,
                puedeIniciarPrueba: false,
                inicioEn: prueba.inicioEn,
                finEn: prueba.finEn,
              },
              uso: {
                ...anterior.uso,
                paginasIa: prueba.paginasIaUtilizadas,
                limitePaginasIa: prueba.limitePaginasIa,
                periodoIa: 'PRUEBA',
                inicioPeriodoIa: prueba.inicioEn,
                finPeriodoIa: prueba.finEn,
              },
              prueba,
            }
          : anterior,
      );
    },
    // También se vuelve a consultar si una respuesta se perdió o la prueba se activó desde otra pestaña.
    onSettled: () => queryClient.invalidateQueries({ queryKey: planesKeys.all }),
  });
  return {
    ...mutacion,
    mensajeError:
      mutacion.error instanceof ApiError
        ? mutacion.error.message
        : 'No pudimos activar la prueba. Probá nuevamente.',
  };
}
