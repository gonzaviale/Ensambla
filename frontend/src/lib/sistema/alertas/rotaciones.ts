import { allBlocks } from '../core';
import type { Fila, Sistema } from '../tipos';
import { diasEntre, numero } from './medidas';

export type Rotacion = { u30: number; ultima?: string };

export type Rotaciones = { porProducto: Map<string, Rotacion>; hayVentas: boolean };

export const ventasDe = (sistema: Sistema): Fila[] =>
  allBlocks(sistema)
    .filter((bloque) => bloque.type === 'sales')
    .flatMap((bloque) => bloque.rows);

const esVentaFechada = (venta: Fila) => Boolean(String(venta.product ?? '') && venta.date);

/** Lo que se vendió de cada producto en los últimos 30 días, y cuándo fue la última venta. */
export function rotaciones(sistema: Sistema, hoy: string): Rotaciones {
  const porProducto = new Map<string, Rotacion>();
  const fechadas = ventasDe(sistema).filter(esVentaFechada);
  for (const venta of fechadas) sumarLaVenta(porProducto, venta, hoy);
  return { porProducto, hayVentas: fechadas.length > 0 };
}

function sumarLaVenta(porProducto: Map<string, Rotacion>, venta: Fila, hoy: string) {
  const id = String(venta.product);
  const fecha = String(venta.date);
  const dias = diasEntre(hoy, fecha);
  const actual = porProducto.get(id) ?? { u30: 0 };
  if (dias >= 0 && dias < 30) actual.u30 += numero(venta.quantity);
  if (!actual.ultima || fecha > actual.ultima) actual.ultima = fecha;
  porProducto.set(id, actual);
}
