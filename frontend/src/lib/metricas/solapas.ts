import type { Metrica } from '@/types/metricas';

// Los colores de la paleta: cada métrica lleva siempre el mismo aunque cambie de lugar (sale del orden de los ids, así
// no se repiten entre las que hay). Las ventas, siempre petróleo.
const COLORES = ['naranja', 'coral', 'magenta', 'celeste', 'amarillo'] as const;

export const colorDeLaMetrica = (metricaId: string, metricas: Metrica[]) => {
  const ids = metricas.map((metrica) => metrica.id).sort();
  return COLORES[ids.indexOf(metricaId) % COLORES.length] ?? 'naranja';
};

export const idDeLaSolapa = (solapa: string) => `solapa-${solapa}`;
export const idDelCuerpo = (solapa: string) => `cuerpo-${solapa}`;
