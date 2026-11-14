import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearMetrica, eliminarMetrica, previsualizarMetrica } from '@/api/metricas';
import type { DefinicionDeMetrica, NuevaMetrica } from '@/types/metricas';
import { metricasKeys } from './queries';

/** Guarda la métrica en el cuaderno; la lista y las sugeridas (ya no incluyen la guardada) se vuelven a pedir. */
export function useGuardarMetrica(sistemaId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (metrica: NuevaMetrica) => crearMetrica(sistemaId, metrica),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: metricasKeys.cuaderno(sistemaId) }),
  });
}

export function useQuitarMetrica(sistemaId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (metricaId: string) => eliminarMetrica(sistemaId, metricaId),
    // También si el servidor no la tenía (404): se vuelve a preguntar para mostrar lo que hay de verdad.
    onSettled: () => queryClient.invalidateQueries({ queryKey: metricasKeys.cuaderno(sistemaId) }),
  });
}

/** La vista previa de una receta armada a mano: calcula con los datos del cuaderno y no guarda nada. */
export function useVerComoQueda(sistemaId: string) {
  return useMutation({
    mutationFn: (definicion: DefinicionDeMetrica) => previsualizarMetrica(sistemaId, definicion),
  });
}
