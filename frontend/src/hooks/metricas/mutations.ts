import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  cambiarGraficoDeMetrica,
  crearMetrica,
  eliminarMetrica,
  interpretarPedidoDeMetrica,
  moverMetrica,
  renombrarMetrica,
} from '@/api/metricas';
import type { GraficoDeMetrica, NuevaMetrica } from '@/types/metricas';
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

/** Le cambia el nombre a una métrica del panel y vuelve a pedir la lista para mostrar el nombre guardado. */
export function useRenombrarMetrica(sistemaId: string, metricaId: string) {
  const volverAPedir = useVolverAPedirLasMetricas(sistemaId);
  return useMutation({
    mutationFn: (nombre: string) => renombrarMetrica(sistemaId, metricaId, nombre),
    onSuccess: volverAPedir,
  });
}

/** Sube o baja una métrica en el panel; la lista se vuelve a pedir con el orden que guardó el servidor. */
export function useMoverMetrica(sistemaId: string, metricaId: string) {
  const volverAPedir = useVolverAPedirLasMetricas(sistemaId);
  return useMutation({
    mutationFn: (lugar: number) => moverMetrica(sistemaId, metricaId, lugar),
    onSettled: volverAPedir,
  });
}

/** Elige cómo se dibuja la métrica; mientras el servidor responde ya se ve con el gráfico elegido. */
export function useCambiarGrafico(sistemaId: string, metricaId: string) {
  const volverAPedir = useVolverAPedirLasMetricas(sistemaId);
  return useMutation({
    mutationFn: (grafico: GraficoDeMetrica) =>
      cambiarGraficoDeMetrica(sistemaId, metricaId, grafico),
    onSettled: volverAPedir,
  });
}

function useVolverAPedirLasMetricas(sistemaId: string) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: metricasKeys.cuaderno(sistemaId) });
}

/** Entiende lo que la persona escribió y propone la métrica con su número; no guarda nada hasta que confirme. */
export function useEntenderPedido(sistemaId: string) {
  return useMutation({
    mutationFn: (pedido: string) => interpretarPedidoDeMetrica(sistemaId, pedido),
  });
}
