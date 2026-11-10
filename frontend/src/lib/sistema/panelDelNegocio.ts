import { allBlocks } from './core';
import type { Sistema } from './tipos';

/** Un cuaderno tiene ventas si alguna fila de sus tablas de ventas es de la persona (las de ejemplo no cuentan). */
export const tieneVentas = (sistema: Sistema) =>
  allBlocks(sistema).some(
    (bloque) => bloque.type === 'sales' && bloque.rows.some((fila) => !fila.__example),
  );

/**
 * El cuaderno del que sale el panel de la home: el primero que tiene ventas y, si ninguno las tiene, el
 * primero de la lista (así el panel explica cómo cargarlas). Sin cuadernos no hay panel.
 */
export const sistemaDelPanel = (sistemas: Sistema[]): Sistema | null =>
  sistemas.find(tieneVentas) ?? sistemas[0] ?? null;
