import { allBlocks, findBlock, numero } from '../core';
import type { Bloque, Fila, Sistema, Valor } from '../tipos';
import { datoPrincipal } from './piezasDelSistema';
import type { SinStock } from './tipos';

// El stock de las ventas, salidas y entradas que carga Sami. A diferencia del editor (`upsertRow`), Sami carga las
// filas de una y sus vínculos se resuelven después (o al confirmar un producto nuevo), así que cada movimiento suyo
// recuerda de qué producto movió el stock (`__stockDe`) y cuánto de verdad (`__stockDelta`, con signo): al cambiar la
// cantidad o el producto se revierte lo aplicado y se vuelve a aplicar. Una salida nunca deja el stock en negativo: se
// descuenta hasta 0 y se avisa. Las marcas no se guardan (`descartarEjemplos`).
const STOCK_DE = '__stockDe';
const STOCK_DELTA = '__stockDelta';

type MovimientoDeSami = { bloque: Bloque; fila: Fila; direccion: number };

/** La pieza de productos con la que se vincula un movimiento. */
const productosDe = (sistema: Sistema, bloque: Bloque) =>
  findBlock(sistema, bloque.fields.find((item) => item.key === 'product')?.target);

/** El producto vinculado de un movimiento, si el vínculo apunta a uno que existe. */
const productoDe = (sistema: Sistema, bloque: Bloque, productoId: Valor) =>
  productosDe(sistema, bloque)?.rows.find((fila) => fila.id === productoId);

const movioStock = (fila: Fila) => STOCK_DE in fila;

export function quitarLasMarcasDeStock(fila: Fila) {
  Reflect.deleteProperty(fila, STOCK_DE);
  Reflect.deleteProperty(fila, STOCK_DELTA);
}

/** Devuelve al producto lo que movió una fila y le saca las marcas. */
function revertirStock(sistema: Sistema, bloque: Bloque, fila: Fila) {
  if (!movioStock(fila)) return;
  const producto = productoDe(sistema, bloque, fila[STOCK_DE]);
  if (producto) producto.quantity = numero(producto.quantity) - numero(fila[STOCK_DELTA]);
  quitarLasMarcasDeStock(fila);
}

/**
 * Lo que vuelve al stock al borrar un movimiento. Uno de Sami devuelve exacto lo que movió; uno que ya estaba guardado
 * movió su cantidad al cargarse en el editor, así que se le devuelve esa cantidad (como `removeRow`). Al borrar una
 * entrada el stock nunca queda en negativo: baja hasta 0. Los ejemplos no movieron stock.
 */
export function devolverStockAlBorrar(sistema: Sistema, bloque: Bloque, fila: Fila) {
  if (movioStock(fila)) {
    revertirStock(sistema, bloque, fila);
    return;
  }
  const producto = productoDe(sistema, bloque, fila.product);
  if (!bloque.stockDirection || fila.__example || !producto) return;
  const devuelto = bloque.stockDirection * numero(fila.quantity);
  producto.quantity = Math.max(0, numero(producto.quantity) - devuelto);
}

/**
 * Pone el stock de acuerdo con los movimientos de Sami: los que ya movieron stock y los de `nuevas`. Primero revierte
 * todo lo aplicado y después aplica lo actual, así una corrección deja el stock como si se hubiera cargado bien. Un
 * movimiento sin producto vinculado (pendiente) queda marcado sin mover nada, para aplicarse cuando se vincule.
 */
export function sincronizarStock(sistema: Sistema, nuevas: ReadonlySet<string>): SinStock[] {
  const movimientos = movimientosDeSami(sistema, nuevas);
  for (const { bloque, fila } of movimientos) revertirStock(sistema, bloque, fila);
  return movimientos.flatMap((movimiento) => aplicarElMovimiento(sistema, movimiento));
}

const movimientosDeSami = (sistema: Sistema, nuevas: ReadonlySet<string>): MovimientoDeSami[] =>
  allBlocks(sistema)
    .filter((bloque) => bloque.stockDirection)
    .flatMap((bloque) =>
      bloque.rows
        .filter((fila) => !fila.__example && (movioStock(fila) || nuevas.has(fila.id)))
        .map((fila) => ({ bloque, fila, direccion: bloque.stockDirection ?? -1 })),
    );

function aplicarElMovimiento(sistema: Sistema, movimiento: MovimientoDeSami): SinStock[] {
  const { bloque, fila } = movimiento;
  const producto = productoDe(sistema, bloque, fila.product);
  const pedido = numero(fila.quantity);
  const efecto =
    producto && pedido > 0 ? moverElStock(sistema, movimiento, producto, pedido) : SIN_EFECTO;
  fila[STOCK_DE] = producto ? producto.id : '';
  fila[STOCK_DELTA] = efecto.delta;
  return efecto.faltantes;
}

type EfectoEnElStock = { delta: number; faltantes: SinStock[] };

const SIN_EFECTO: EfectoEnElStock = { delta: 0, faltantes: [] };

function moverElStock(
  sistema: Sistema,
  { bloque, direccion }: MovimientoDeSami,
  producto: Fila,
  pedido: number,
): EfectoEnElStock {
  const habia = numero(producto.quantity);
  const delta = direccion > 0 ? pedido : -Math.min(pedido, Math.max(habia, 0));
  const faltantes =
    direccion < 0 && pedido > habia ? [faltante(sistema, bloque, producto, pedido, habia)] : [];
  producto.quantity = habia + delta;
  delete producto.__example;
  return { delta, faltantes };
}

const faltante = (
  sistema: Sistema,
  bloque: Bloque,
  producto: Fila,
  pedido: number,
  habia: number,
) => {
  const principal = datoPrincipal(productosDe(sistema, bloque)) ?? 'name';
  return { producto: String(producto[principal] ?? ''), pedido, habia };
};
