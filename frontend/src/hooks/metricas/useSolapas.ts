import { useState } from 'react';
import type { Metrica } from '@/types/metricas';

/** La solapa fija de las ventas y la de agregar; las demás son las métricas, por su id. */
export const SOLAPA_DE_VENTAS = 'ventas';
export const SOLAPA_PARA_AGREGAR = 'agregar';

const existe = (solapa: string, metricas: Metrica[]) =>
  solapa === SOLAPA_DE_VENTAS ||
  solapa === SOLAPA_PARA_AGREGAR ||
  metricas.some((metrica) => metrica.id === solapa);

/**
 * Qué solapa del panel «Tu negocio» se está mirando. Arranca en las ventas y, si la métrica abierta se quita, vuelve
 * a las ventas en lugar de mostrar una solapa vacía.
 */
export function useSolapas(metricas: Metrica[]) {
  const [elegida, setElegida] = useState(SOLAPA_DE_VENTAS);
  const activa = existe(elegida, metricas) ? elegida : SOLAPA_DE_VENTAS;
  return {
    activa,
    elegir: setElegida,
    volverALasVentas: () => setElegida(SOLAPA_DE_VENTAS),
    metricaActiva: metricas.find((metrica) => metrica.id === activa),
  };
}

export type Solapas = ReturnType<typeof useSolapas>;
