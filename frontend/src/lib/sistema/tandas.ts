import type { Fila } from './tipos';

// Las tablas se dibujan por tandas: una pieza con cientos de registros no tiene que estirar la pantalla.

/** Cuántos registros se dibujan de entrada en un bloque y cuántos más se suman cada vez. */
export const TANDA_DE_REGISTROS = 10;
/** Lo mismo en la tabla a pantalla completa, que tiene más lugar. */
export const TANDA_DE_TABLA_COMPLETA = 50;
/** Cuánto falta para el final del scroll para que se sumen los que siguen. */
export const MARGEN_DEL_SCROLL = 48;

/** Los registros nuevos van primero; el resto conserva su orden. */
export function conNuevasPrimero(filas: Fila[], nuevas: ReadonlySet<string>): Fila[] {
  if (!nuevas.size) return filas;
  return [
    ...filas.filter((fila) => nuevas.has(fila.id)),
    ...filas.filter((fila) => !nuevas.has(fila.id)),
  ];
}

/**
 * Los registros que se cargaron en este armado: los que no son de ejemplo y no estaban guardados en `guardadas`
 * (los ids de las filas del sistema que se está ajustando; vacío si se arma uno nuevo).
 */
export function registrosNuevos(
  bloques: { rows: Fila[] }[],
  guardadas: ReadonlySet<string>,
): Set<string> {
  const nuevas = new Set<string>();
  for (const bloque of bloques) {
    for (const fila of bloque.rows) {
      if (!fila.__example && !guardadas.has(fila.id)) nuevas.add(fila.id);
    }
  }
  return nuevas;
}

const EVENTO_MOSTRAR_FILA = 'ensambla:mostrar-fila';

type PedidoDeFila = { bloqueId: string; filaId: string };

/**
 * Pide que la tabla del bloque dibuje ese registro aunque todavía no le toque (por ejemplo, para llevar la vista a él
 * desde un aviso). La tabla escucha con `alPedirFila`.
 */
export function pedirMostrarFila(bloqueId: string, filaId: string) {
  document.dispatchEvent(
    new CustomEvent<PedidoDeFila>(EVENTO_MOSTRAR_FILA, { detail: { bloqueId, filaId } }),
  );
}

/** Escucha los pedidos de `pedirMostrarFila`; devuelve la función que deja de escuchar. */
export function alPedirFila(escuchar: (pedido: PedidoDeFila) => void) {
  const alEvento = (evento: Event) => escuchar((evento as CustomEvent<PedidoDeFila>).detail);
  document.addEventListener(EVENTO_MOSTRAR_FILA, alEvento);
  return () => document.removeEventListener(EVENTO_MOSTRAR_FILA, alEvento);
}
