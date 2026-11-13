import { allBlocks } from './core';
import type { Sistema } from './tipos';

/** Una sección tiene ventas si alguna fila de sus tablas de ventas es de la persona (las de ejemplo no cuentan). */
export const tieneVentas = (sistema: Sistema) =>
  allBlocks(sistema).some(
    (bloque) => bloque.type === 'sales' && bloque.rows.some((fila) => !fila.__example),
  );

/**
 * La sección de la que sale el panel de la home: la primera que tiene ventas y, si ninguna las tiene, la
 * primera de la lista (así el panel explica cómo cargarlas). Sin secciones no hay panel.
 */
export const sistemaDelPanel = (sistemas: Sistema[]): Sistema | null =>
  sistemas.find(tieneVentas) ?? sistemas[0] ?? null;

/** La tabla de ventas de una sección, para llevar a la persona justo ahí. */
export const bloqueDeVentas = (sistema: Sistema) =>
  allBlocks(sistema).find((bloque) => bloque.type === 'sales');

const enPesos = new Intl.NumberFormat('es-AR');
const conUnDecimal = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 });

/** «$839.750»; un importe negativo lleva el signo adelante del peso: «-$12.000». */
export const pesos = (valor: number) =>
  `${valor < 0 ? '-' : ''}$${enPesos.format(Math.round(Math.abs(valor)))}`;

/** Como `pesos`, pero lo que sobra lleva «+»: así se ve de un vistazo si una semana dejó plata o la sacó. */
export const pesosConSigno = (valor: number) => (valor > 0 ? `+${pesos(valor)}` : pesos(valor));

/** «25,9 %»: en es-AR el decimal es una coma. */
export const porcentaje = (valor: number) => `${conUnDecimal.format(valor)} %`;
