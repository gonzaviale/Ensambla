import { leerPrecio, idCaja } from '@/lib/caja';
import type { LineaCaja, SolicitudCobro, VentaCaja } from '@/types/caja';

export function crearImporteLibre(descripcion: string, importe: string): LineaCaja {
  const nombre = descripcion.trim();
  if (!nombre || nombre.length > 160)
    throw new Error('Escribí una descripción de hasta 160 caracteres.');
  const importeCentavos = leerPrecio(importe);
  return {
    id: crypto.randomUUID(),
    productoId: null,
    nombre,
    modo: 'unidad',
    cantidad: 1,
    precio: importeCentavos / 100,
    importeCentavos,
  };
}

export function lineaParaCobrar(linea: LineaCaja): SolicitudCobro['lineas'][number] {
  if (linea.productoId === null)
    return {
      productoId: null,
      descripcion: linea.nombre,
      cantidad: 1,
      precio: linea.precio,
      modo: 'UNIDAD',
    };
  return {
    productoId: idCaja(linea.productoId),
    cantidad: linea.cantidad,
    precio: linea.precio,
    modo: linea.modo === 'peso' ? 'PESO' : 'UNIDAD',
  };
}

export function detalleLineaVendida(linea: VentaCaja['lineas'][number]) {
  if (linea.productoId === null) return 'Importe libre';
  return linea.modo === 'PESO' ? `${Math.round(linea.cantidad * 1000)} g` : `${linea.cantidad} u.`;
}
