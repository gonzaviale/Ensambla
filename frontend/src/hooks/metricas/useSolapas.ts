import { useCallback, useState } from 'react';
import type { Metrica } from '@/types/metricas';

/** La solapa fija de las ventas; las demás son las métricas, por su id. */
export const SOLAPA_DE_VENTAS = 'ventas';

const existe = (solapa: string, metricas: Metrica[]) =>
  solapa === SOLAPA_DE_VENTAS || metricas.some((metrica) => metrica.id === solapa);

/**
 * Qué solapa del panel «Tu negocio» se está mirando. Arranca en las ventas y, si la métrica abierta se quita, vuelve
 * a las ventas en lugar de mostrar una solapa vacía. También si está abierto el panel para agregar una: al guardarla,
 * el panel se cierra y queda abierta su solapa.
 */
export function useSolapas(metricas: Metrica[]) {
  const [elegida, setElegida] = useState(SOLAPA_DE_VENTAS);
  const [agregando, setAgregando] = useState(false);
  const cerrarElPanel = useCallback(() => setAgregando(false), []);
  const activa = existe(elegida, metricas) ? elegida : SOLAPA_DE_VENTAS;
  return {
    activa,
    elegir: setElegida,
    volverALasVentas: () => setElegida(SOLAPA_DE_VENTAS),
    metricaActiva: metricas.find((metrica) => metrica.id === activa),
    agregando,
    abrirElPanel: () => setAgregando(true),
    cerrarElPanel,
    abrirLaAgregada: (metricaId: string) => {
      setAgregando(false);
      setElegida(metricaId);
    },
  };
}

export type Solapas = ReturnType<typeof useSolapas>;
