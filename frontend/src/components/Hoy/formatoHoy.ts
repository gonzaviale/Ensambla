import type { ItemPedido, ProductoRed, ProveedorRed } from '@/types/red';

/** 45 min → «45 min»; 120 → «2 h»; 195 → «3 h 15 min». */
export function formatearTiempo(minutos: number): string {
  const total = Math.max(0, Math.round(minutos));
  const horas = Math.floor(total / 60);
  const resto = total % 60;
  if (horas === 0) return `${resto} min`;
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`;
}

export type LineaPedido = { producto: ProductoRed; cantidad: number };

export type GrupoPedido = {
  proveedorId: string;
  proveedor: string;
  lineas: LineaPedido[];
  /** Costo estimado: cantidad por el costo actual de cada producto. */
  costoEstimado: number;
};

/** El pedido agrupado por proveedor (a cada uno se le compra por separado), en orden alfabético. */
export function agruparPedido(
  pedido: ItemPedido[],
  productos: ProductoRed[],
  proveedores: ProveedorRed[],
): GrupoPedido[] {
  const grupos = new Map<string, GrupoPedido>();
  for (const item of pedido) {
    const producto = productos.find((p) => p.id === item.productoId);
    if (!producto) continue;
    const proveedor = proveedores.find((p) => p.id === producto.proveedorId)?.nombre ?? 'Proveedor';
    const grupo = grupos.get(producto.proveedorId) ?? {
      proveedorId: producto.proveedorId,
      proveedor,
      lineas: [],
      costoEstimado: 0,
    };
    grupo.lineas.push({ producto, cantidad: item.cantidad });
    grupo.costoEstimado += producto.costo * item.cantidad;
    grupos.set(producto.proveedorId, grupo);
  }
  return [...grupos.values()].sort((a, b) => a.proveedor.localeCompare(b.proveedor, 'es'));
}

/** Texto del pedido listo para pegar en WhatsApp. */
export function textoPedido(negocio: string, grupos: GrupoPedido[]): string {
  const lineas = [`Pedido de ${negocio}`];
  for (const grupo of grupos) {
    lineas.push('', `${grupo.proveedor}:`);
    for (const { producto, cantidad } of grupo.lineas) {
      lineas.push(`- ${cantidad} × ${producto.nombre}`);
    }
  }
  return lineas.join('\n');
}
