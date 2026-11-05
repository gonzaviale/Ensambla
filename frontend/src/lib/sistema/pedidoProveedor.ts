import type { GrupoDeReposicion, LineaDelPedido, ProductoFaltante } from '@/types/reposicion';

export const NOMBRE_SIN_PROVEEDOR = 'Sin proveedor';

const MENSAJE_SIN_PRODUCTOS = 'Elegí al menos un producto para el pedido.';
const MENSAJE_CANTIDAD_INVALIDA = 'Revisá las cantidades: tienen que ser un número mayor a cero.';
const ENCABEZADO_DEL_PEDIDO = 'Hola, necesito reponer lo siguiente:';

/** Lo que la persona decide de un producto: si lo pide y cuánto (como lo escribe en el campo). */
export type DecisionDelProducto = { pedir: boolean; cantidad: string };

/** Las decisiones de todos los productos, por el id del producto. */
export type DecisionesDelPedido = Record<string, DecisionDelProducto>;

const DECISION_VACIA: DecisionDelProducto = { pedir: false, cantidad: '' };

export const nombreDelGrupo = (grupo: GrupoDeReposicion) => grupo.proveedor ?? NOMBRE_SIN_PROVEEDOR;

/** La decisión tomada sobre un producto; si no hay ninguna, no se pide. */
export const decisionDe = (decisiones: DecisionesDelPedido, productoId: string): DecisionDelProducto =>
  decisiones[productoId] ?? DECISION_VACIA;

const decisionInicial = (producto: ProductoFaltante): DecisionDelProducto => ({
  pedir: true,
  cantidad: String(producto.cantidadAPedir),
});

/** Al abrir el pedido se piden todos los productos, con la cantidad que sugiere el servidor. */
export function decisionesIniciales(grupos: GrupoDeReposicion[]): DecisionesDelPedido {
  const decisiones: DecisionesDelPedido = {};
  grupos.forEach((grupo) =>
    grupo.productos.forEach((producto) => (decisiones[producto.id] = decisionInicial(producto))),
  );
  return decisiones;
}

/** La cantidad escrita como número; `null` si no es un número mayor a cero. */
export function cantidadEscrita(texto: string): number | null {
  const cantidad = Number(texto.replace(',', '.'));
  return texto.trim() !== '' && Number.isFinite(cantidad) && cantidad > 0 ? cantidad : null;
}

const estaElegido = (decisiones: DecisionesDelPedido, producto: ProductoFaltante) =>
  decisiones[producto.id]?.pedir === true;

const productosElegidos = (grupo: GrupoDeReposicion, decisiones: DecisionesDelPedido) =>
  grupo.productos.filter((producto) => estaElegido(decisiones, producto));

const cantidadDe = (decisiones: DecisionesDelPedido, producto: ProductoFaltante) =>
  cantidadEscrita(decisiones[producto.id]?.cantidad ?? '');

/** Por qué todavía no se puede armar el pedido de ese proveedor, o `null` si está listo. */
export function errorDelPedido(grupo: GrupoDeReposicion, decisiones: DecisionesDelPedido): string | null {
  const elegidos = productosElegidos(grupo, decisiones);
  if (elegidos.length === 0) return MENSAJE_SIN_PRODUCTOS;
  const hayCantidadInvalida = elegidos.some((producto) => cantidadDe(decisiones, producto) === null);
  return hayCantidadInvalida ? MENSAJE_CANTIDAD_INVALIDA : null;
}

/** Los productos elegidos con su cantidad, tal como se le mandan al servidor. */
export function lineasDelPedido(grupo: GrupoDeReposicion, decisiones: DecisionesDelPedido): LineaDelPedido[] {
  return productosElegidos(grupo, decisiones).flatMap((producto) => {
    const cantidad = cantidadDe(decisiones, producto);
    return cantidad === null ? [] : [{ productoId: producto.id, cantidad }];
  });
}

/** El mensaje que acompaña al PDF: una línea por producto, en el orden del pedido. */
export function textoDelPedido(grupo: GrupoDeReposicion, decisiones: DecisionesDelPedido): string {
  const renglones = lineasDelPedido(grupo, decisiones).map((linea) => {
    const producto = grupo.productos.find((otro) => otro.id === linea.productoId);
    return `- ${producto?.nombre ?? ''}: ${linea.cantidad}`;
  });
  return [ENCABEZADO_DEL_PEDIDO, ...renglones].join('\n');
}
