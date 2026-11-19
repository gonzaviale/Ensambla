import type { Bloque, Campo, Fila } from './tipos';

export const CLASE_DE_STOCK_BAJO = 'low-stock';

/** La celda de la cantidad de un producto del inventario que ya llegó al mínimo. */
export function esCeldaConStockBajo(bloque: Bloque, campo: Campo, fila: Fila): boolean {
  return (
    bloque.type === 'inventory' &&
    campo.key === 'quantity' &&
    Number(fila.quantity) <= Number(fila.minimum)
  );
}

export const claseDeLaCelda = (bloque: Bloque, campo: Campo, fila: Fila): string =>
  esCeldaConStockBajo(bloque, campo, fila) ? CLASE_DE_STOCK_BAJO : '';
