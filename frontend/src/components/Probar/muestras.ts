import type { RespuestaId, RubroId } from '@/types/negocio';

// Números de muestra para «Probá ahora»: son inventados, no salen de ningún negocio real y la
// pantalla los rotula como «Datos de ejemplo». Una por respuesta y por rubro.

export type Muestra = { titular: string; detalle: string };

/** Las tres respuestas que se muestran en cada rubro (la primera suele estar lista desde el inicio). */
export const RESPUESTAS_DE_MUESTRA: Record<RubroId, RespuestaId[]> = {
  comercio: ['te-quedo', 'margen-producto', 'quien-te-debe'],
  gastronomia: ['te-quedo', 'pedidos-pendientes', 'margen-producto'],
  otro: ['te-quedo', 'margen-producto', 'quien-te-debe'],
};

const BASE: Partial<Record<RespuestaId, Muestra>> = {
  'te-quedo': {
    titular: '$ 182.000',
    detalle: 'Vendiste $ 640.000 y gastaste $ 458.000 este mes.',
  },
  'quien-te-debe': { titular: '$ 24.600', detalle: 'Entre 3 clientes que todavía no pagaron.' },
};

const POR_RUBRO: Record<RubroId, Partial<Record<RespuestaId, Muestra>>> = {
  comercio: {
    'margen-producto': {
      titular: 'Yerba 1 kg',
      detalle: 'Es lo que más te deja: $ 800 por cada una que vendés.',
    },
  },
  gastronomia: {
    'te-quedo': {
      titular: '$ 96.000',
      detalle: 'Cobraste $ 310.000 en viandas y gastaste $ 214.000 este mes.',
    },
    'pedidos-pendientes': {
      titular: '5 pedidos',
      detalle: 'Faltan entregar 5 y cobrar $ 42.000 de lo que queda.',
    },
    'margen-producto': {
      titular: 'Vianda de milanesa',
      detalle: 'Te deja $ 2.300 por cada una, la que más ganancia te da.',
    },
  },
  otro: {
    'margen-producto': {
      titular: 'Producto A',
      detalle: 'Es el que más te deja: $ 1.000 por cada unidad.',
    },
  },
};

export function muestraDe(respuesta: RespuestaId, rubro: RubroId): Muestra {
  return (
    POR_RUBRO[rubro][respuesta] ??
    BASE[respuesta] ?? { titular: 'Datos de ejemplo', detalle: 'Un número de muestra.' }
  );
}
