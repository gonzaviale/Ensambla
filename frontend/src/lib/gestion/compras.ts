import { normalize } from '@/lib/sistema/core';
import { CAJA_PARA_COMPRAS, PROVEEDORES, type Proveedor } from './datosDeEjemplo';
import { ventasDelPeriodo, type DatosDelNegocio, type Producto } from './resultado';

// Compras: cuánto dura el stock de cada producto al ritmo al que se vende, qué pedirle a cada proveedor para
// llegar a la próxima entrega con una semana de colchón y qué dejar para después si la plata no alcanza.

/** Días de colchón por encima de la próxima entrega. */
export const COLCHON_DIAS = 7;

export type Urgencia = 'agotado' | 'urgente' | 'pronto';

export type Faltante = {
  producto: Producto;
  proveedor: Proveedor;
  /** Unidades por día en los últimos 30 días. */
  ritmo: number;
  /** Días que dura el stock a ese ritmo. */
  diasDeStock: number;
  pedir: number;
  subtotal: number;
  urgencia: Urgencia;
};

export function proveedorDe(nombreDelProducto: string): Proveedor {
  const nombre = normalize(nombreDelProducto);
  const propio = PROVEEDORES.find((proveedor) =>
    proveedor.palabras.some((palabra) => nombre.includes(normalize(palabra))),
  );
  const general = PROVEEDORES.find((proveedor) => proveedor.palabras.length === 0);
  // PROVEEDORES siempre tiene uno general (sin palabras), que se lleva lo que no es de nadie.
  return (propio ?? general ?? PROVEEDORES[0]) as Proveedor;
}

/** Cómo viene cada producto: lo caro o frágil por unidad, el resto de a 6. */
const bultoDe = (producto: Producto) => (producto.precio >= 7000 ? 1 : 6);

/** Los productos que no llegan a la próxima entrega con colchón, del más urgente al menos. */
export function faltantes(datos: DatosDelNegocio, hoy: string): Faltante[] {
  const delMes = ventasDelPeriodo(datos.ventas, hoy, 30);
  const lista: Faltante[] = [];
  for (const producto of datos.productos) {
    const vendidas = delMes
      .filter((venta) => venta.productoId === producto.id)
      .reduce((total, venta) => total + venta.cantidad, 0);
    const ritmo = vendidas / 30;
    // Lo que no se vende no se repone por ritmo: eso lo mira el aviso de producto parado.
    if (ritmo === 0) continue;
    const proveedor = proveedorDe(producto.nombre);
    const diasDeStock = producto.stock / ritmo;
    const noLlega = diasDeStock < proveedor.entregaCadaDias;
    const bajoMinimo = producto.minimo > 0 && producto.stock <= producto.minimo;
    if (!noLlega && !bajoMinimo) continue;
    const necesario =
      Math.ceil(ritmo * (proveedor.entregaCadaDias + COLCHON_DIAS)) - producto.stock;
    if (necesario <= 0) continue;
    const bulto = bultoDe(producto);
    const pedir = Math.ceil(necesario / bulto) * bulto;
    lista.push({
      producto,
      proveedor,
      ritmo,
      diasDeStock,
      pedir,
      subtotal: pedir * producto.costo,
      urgencia: producto.stock === 0 ? 'agotado' : diasDeStock <= 3 ? 'urgente' : 'pronto',
    });
  }
  return lista.sort((a, b) => a.diasDeStock - b.diasDeStock);
}

export type OrdenDeCompra = {
  proveedor: Proveedor;
  items: Faltante[];
  /** Lo de este proveedor que no entra en la plata: se pide en la próxima vuelta. */
  postergados: Faltante[];
  total: number;
  /** Ya se le mandó al proveedor: queda como se mandó, aunque cambie la plata. */
  enviada: boolean;
};

export type PlanDeCompras = {
  ordenes: OrdenDeCompra[];
  /** Lo que no entra en la plata disponible, de todos los proveedores. */
  postergados: Faltante[];
  /** Lo que suman los pedidos que todavía no se mandaron. */
  total: number;
  /** Lo que ya se comprometió con los pedidos enviados. */
  enviado: number;
  caja: number;
  /** La plata que queda después de lo enviado y lo que falta mandar; negativa si no alcanza. */
  quedan: number;
};

const sumar = (items: Faltante[]) => items.reduce((total, item) => total + item.subtotal, 0);

/**
 * Agrupa los faltantes por proveedor. Lo agotado y lo urgente entra siempre; lo demás, del que dura menos al que
 * dura más, mientras alcance la plata. Lo que no entra queda para la próxima vuelta, dentro del pedido de su
 * proveedor. Los pedidos ya enviados no se tocan: su plata se descuenta y sus productos no se vuelven a pedir.
 */
export function planDeCompras(
  lista: Faltante[],
  caja = CAJA_PARA_COMPRAS,
  enviadas: OrdenDeCompra[] = [],
): PlanDeCompras {
  const yaPedidos = new Set(enviadas.map((orden) => orden.proveedor.id));
  const enviado = enviadas.reduce((total, orden) => total + orden.total, 0);
  const incluidos: Faltante[] = [];
  const postergados: Faltante[] = [];
  let gastado = 0;
  for (const faltante of lista) {
    if (yaPedidos.has(faltante.proveedor.id)) continue;
    if (faltante.urgencia !== 'pronto' || enviado + gastado + faltante.subtotal <= caja) {
      incluidos.push(faltante);
      gastado += faltante.subtotal;
    } else {
      postergados.push(faltante);
    }
  }
  const ordenes = PROVEEDORES.map((proveedor): OrdenDeCompra => {
    const mandada = enviadas.find((orden) => orden.proveedor.id === proveedor.id);
    if (mandada) return { ...mandada, enviada: true };
    const items = incluidos.filter((item) => item.proveedor.id === proveedor.id);
    return {
      proveedor,
      items,
      postergados: postergados.filter((item) => item.proveedor.id === proveedor.id),
      total: sumar(items),
      enviada: false,
    };
  }).filter((orden) => orden.items.length > 0 || orden.postergados.length > 0);
  return { ordenes, postergados, total: gastado, enviado, caja, quedan: caja - enviado - gastado };
}

/** El mensaje de WhatsApp para el proveedor, listo para revisar antes de mandarlo. */
export function mensajeParaProveedor(orden: OrdenDeCompra, negocio: string) {
  const lineas = orden.items.map((item) => `• ${item.pedir} × ${item.producto.nombre}`);
  return [
    `Hola, ${orden.proveedor.nombre}. Te hago un pedido para ${negocio}:`,
    ...lineas,
    '¿Me confirmás precio y día de entrega? ¡Gracias!',
  ].join('\n');
}

export type CambioDeCosto = {
  producto: Producto;
  costoNuevo: number;
  variacion: number;
  /** El margen sobre el precio que tenía antes de la suba. */
  margen: number;
  /** El precio que mantiene ese margen con el costo nuevo, redondeado a $50. */
  precioSugerido: number;
};

/** Lo que cambió con el remito: los productos que subieron y el precio que mantiene el margen. */
export function cambiosDeCosto(
  productos: Producto[],
  costosNuevos: Record<string, number>,
): CambioDeCosto[] {
  return productos
    .filter(
      (producto) => (costosNuevos[producto.nombre] ?? 0) > producto.costo && producto.costo > 0,
    )
    .map((producto) => {
      const costoNuevo = costosNuevos[producto.nombre] ?? producto.costo;
      const margen = producto.precio > 0 ? (producto.precio - producto.costo) / producto.precio : 0;
      const precioSugerido = Math.ceil(costoNuevo / (1 - margen) / 50) * 50;
      return {
        producto,
        costoNuevo,
        variacion: (costoNuevo - producto.costo) / producto.costo,
        margen,
        precioSugerido,
      };
    })
    .sort((a, b) => b.variacion - a.variacion);
}
