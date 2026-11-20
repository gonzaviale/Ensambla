import { claseDeSeleccion, tonoDe, type ConceptoDeColor } from '@/lib/sistema/tonos';

export type EstadoDeLaVenta = 'pagada' | 'parcial' | 'fiada';

type OpcionDeEstado = { valor: EstadoDeLaVenta; texto: string; concepto: ConceptoDeColor };

export const ESTADOS_DE_LA_VENTA: readonly OpcionDeEstado[] = [
  { valor: 'pagada', texto: 'Pagada', concepto: 'ventas' },
  { valor: 'parcial', texto: 'A medias', concepto: 'ventas' },
  { valor: 'fiada', texto: 'Fiada', concepto: 'fiado' },
];

export function aspectoDelEstado(estado: OpcionDeEstado, elegido: boolean) {
  return {
    className: claseDeSeleccion(elegido),
    'data-tono': tonoDe(estado.concepto),
  };
}
