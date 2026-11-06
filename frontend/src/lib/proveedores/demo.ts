import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { allBlocks } from '@/lib/sistema/core';
import { sugerirPedido, totalCompra } from './calculos';
import type { CompraProveedor, DemoProveedores, RevisionCompra } from '@/types/proveedores';

export function hoyLocal() {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
}

export function crearDemoProveedores(hoy = hoyLocal()): DemoProveedores {
  // Reutiliza los bloques y campos del almacén existente; sólo se trabaja sobre esta copia de demostración.
  const bloques = allBlocks(crearAlmacenDeEjemplo(hoy));
  const inventario = bloques.find((b) => b.type === 'inventory');
  const productos = (inventario?.rows ?? []).map((p, indice) => ({
    id: p.id,
    nombre: String(p.name),
    stock: Number(p.quantity),
    minimo: Number(p.minimum),
    costo: Number(p.cost),
    precio: Number(p.price),
    presentacion: 'unidad / envase indicado',
    proveedorId: indice === 5 ? 'limpieza' : 'distribuidora',
  }));
  const mesAnterior = new Date(`${hoy.slice(0, 7)}-01T12:00:00`);
  mesAnterior.setMonth(mesAnterior.getMonth() - 1);
  const fechaAnterior = `${mesAnterior.getFullYear()}-${String(mesAnterior.getMonth() + 1).padStart(2, '0')}-15`;
  const compras: CompraProveedor[] = ['distribuidora', 'limpieza'].map((proveedorId, indice) => {
    const lineas = productos
      .filter((p) => p.proveedorId === proveedorId)
      .map((p) => ({ productoId: p.id, cantidad: 12, costo: p.costo }));
    const base = {
      id: `historica-${indice}`,
      proveedorId,
      numero: `DEMO-00${indice + 1}`,
      fecha: fechaAnterior,
      archivo: 'Comprobante histórico de ejemplo',
      lineas,
      extras: 0,
      descuento: 0,
    };
    return { ...base, total: totalCompra(base) };
  });
  const costos = new Map(productos.map((p) => [p.id, p.costo]));
  return {
    version: 1,
    proveedores: [
      { id: 'distribuidora', nombre: 'Distribuidora del Sur', telefono: '' },
      { id: 'limpieza', nombre: 'Limpieza Mayorista', telefono: '' },
    ],
    productos,
    compras,
    pedido: sugerirPedido(productos),
    gastos: [
      ...compras.map((c) => ({
        id: `gasto-${c.id}`,
        name: `Compra ${c.numero}`,
        date: c.fecha,
        total: c.total,
        category: 'Mercadería' as const,
        compraId: c.id,
      })),
      {
        id: 'alquiler',
        name: 'Alquiler del local',
        date: `${hoy.slice(0, 7)}-01`,
        total: 45000,
        category: 'Alquiler',
      },
      { id: 'luz', name: 'Electricidad y gas', date: hoy, total: 12500, category: 'Servicios' },
      {
        id: 'internet',
        name: 'Internet del negocio',
        date: hoy,
        total: 5500,
        category: 'Servicios',
      },
    ],
    ventas: (bloques.find((b) => b.type === 'sales')?.rows ?? []).map((v) => ({
      fecha: String(v.date),
      cantidad: Number(v.quantity),
      precio: Number(v.price),
      costo: costos.get(String(v.product)) ?? 0,
    })),
  };
}

export function crearRevisionSimulada(
  datos: DemoProveedores,
  proveedorId: string,
  archivo: string,
): RevisionCompra {
  const productos = datos.productos.filter((p) => p.proveedorId === proveedorId).slice(0, 3);
  const base = {
    id: crypto.randomUUID(),
    proveedorId,
    numero: '',
    fecha: hoyLocal(),
    archivo,
    lineas: productos.map((p, i) => ({
      productoId: p.id,
      cantidad: 12,
      costo: Math.round(p.costo * (i === 0 ? 1.12 : 1.04)),
    })),
    extras: 0,
    descuento: 0,
  };
  return { ...base, numero: `DEMO-${datos.compras.length + 1}`, totalDeclarado: totalCompra(base) };
}
