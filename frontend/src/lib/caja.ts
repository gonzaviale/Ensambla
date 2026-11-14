import { allBlocks, id, makeBlock, upsertRow } from '@/lib/sistema/core';
import { codigoGtin } from '@/lib/escaneo/codigo';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';
import type { DatosArticuloCaja, LineaCaja, ModoVentaCaja } from '@/types/caja';

export const dineroCaja = (centavos: number) =>
  new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(centavos / 100);
export const fechaCaja = (valor: string) =>
  new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Argentina/Buenos_Aires',
    hourCycle: 'h23',
  }).format(new Date(valor));
export const modoVenta = (producto: Fila): ModoVentaCaja =>
  producto.saleMode === 'peso' ? 'peso' : 'unidad';
export const idCaja = (valor: string) => (/^e[0-9a-f-]{36}$/i.test(valor) ? valor.slice(1) : valor);

export function leerMontoCaja(texto: string) {
  const valor = texto.trim().replace(',', '.');
  if (!valor) return 0;
  if (!/^\d{1,12}(?:\.\d{1,2})?$/.test(valor))
    throw new Error('Usá un importe positivo o cero, con hasta dos decimales.');
  const [entero = '', decimales = ''] = valor.split('.');
  return Number(entero) * 100 + Number(decimales.padEnd(2, '0'));
}

export function leerPrecio(texto: string) {
  const limpio = texto.trim().replace(',', '.');
  if (!/^\d{1,9}(?:\.\d{1,2})?$/.test(limpio))
    throw new Error('Ingresá un precio sin separador de miles y con hasta dos decimales.');
  const [entero = '', decimal = ''] = limpio.split('.');
  const centavos = Number(entero) * 100 + Number(decimal.padEnd(2, '0'));
  if (centavos <= 0) throw new Error('El precio debe ser mayor a cero.');
  return centavos;
}

export function crearLinea(producto: Fila, entrada = '1'): LineaCaja {
  if (!/^\d{1,8}$/.test(entrada.trim()) || Number(entrada) <= 0)
    throw new Error('Ingresá una cantidad entera mayor a cero.');
  const modo = modoVenta(producto);
  const cantidad = Number(entrada) / (modo === 'peso' ? 1000 : 1);
  const precioCentavos = leerPrecio(String(producto.price ?? ''));
  const importeMiles = BigInt(precioCentavos) * BigInt(entrada.trim());
  const importeCentavos = Number(modo === 'peso' ? (importeMiles + 500n) / 1000n : importeMiles);
  if (!Number.isSafeInteger(importeCentavos) || importeCentavos <= 0)
    throw new Error('Revisá el importe del artículo.');
  return {
    id: id(),
    productoId: producto.id,
    nombre: String(producto.name ?? ''),
    modo,
    cantidad,
    precio: precioCentavos / 100,
    importeCentavos,
  };
}

export function agregarLinea(lineas: LineaCaja[], linea: LineaCaja): LineaCaja[] {
  const indice =
    linea.modo === 'unidad'
      ? lineas.findIndex(
          (item) =>
            item.productoId === linea.productoId &&
            item.modo === linea.modo &&
            item.precio === linea.precio,
        )
      : -1;
  if (indice < 0) {
    if (lineas.length >= 100) throw new Error('La compra admite hasta 100 renglones.');
    return [...lineas, linea];
  }
  return lineas.map((item, i) =>
    i === indice
      ? {
          ...item,
          cantidad: item.cantidad + linea.cantidad,
          importeCentavos: item.importeCentavos + linea.importeCentavos,
        }
      : item,
  );
}

export function cambiarCantidad(linea: LineaCaja, cantidad: number): LineaCaja {
  const entrada = Math.round(cantidad * (linea.modo === 'peso' ? 1000 : 1));
  return {
    ...crearLinea(
      { id: linea.productoId, name: linea.nombre, price: linea.precio, saleMode: linea.modo },
      String(entrada),
    ),
    id: linea.id,
  };
}

export function prepararCaja(sistema: Sistema, inventarioId: string) {
  const inventario = allBlocks(sistema).find(
    (b) => b.id === inventarioId && b.type === 'inventory',
  );
  if (!inventario) throw new Error('Elegí el inventario para esta caja.');
  for (const campo of [
    {
      key: 'saleMode',
      label: 'Venta por',
      type: 'select' as const,
      options: ['unidad', 'peso'],
      required: false,
    },
    { key: 'favorite', label: 'Favorito en caja', type: 'checkbox' as const, required: false },
    { key: 'stockControl', label: 'Controlar stock', type: 'checkbox' as const, required: false },
    { key: 'barcode', label: 'Código de barras', type: 'text' as const, required: false },
  ]) {
    const existente = inventario.fields.find((f) => f.key === campo.key);
    if (existente && existente.type !== campo.type)
      throw new Error(`Revisá el campo ${campo.label}.`);
    if (!existente) inventario.fields.push(campo);
  }
  for (const producto of inventario.rows) {
    producto.saleMode ??= 'unidad';
    producto.stockControl ??= true;
    producto.favorite ??= false;
  }
  let ventas = allBlocks(sistema).find(
    (b) =>
      b.type === 'sales' && b.fields.some((f) => f.key === 'product' && f.target === inventarioId),
  );
  if (!ventas) {
    ventas = makeBlock('sales');
    ventas.name = `Ventas de Caja · ${inventario.name}`;
    const relacion = ventas.fields.find((f) => f.key === 'product');
    if (relacion) relacion.target = inventarioId;
    sistema.sections[0]?.blocks.push(ventas);
  }
  const cantidad = ventas.fields.find((f) => f.key === 'quantity');
  if (cantidad) cantidad.min = 0.001;
  return ventas.id;
}

export function guardarArticuloCaja(
  sistema: Sistema,
  inventario: Bloque,
  datos: DatosArticuloCaja,
  codigo: string,
  productoId?: string,
) {
  const anterior = inventario.rows.find((p) => p.id === productoId);
  const valores: Fila = {
    ...anterior,
    id: productoId ?? id(),
    ...datos.extras,
    name: datos.nombre.trim(),
    price: leerPrecio(datos.precio) / 100,
    saleMode: datos.modo,
    favorite: datos.favorito,
    stockControl: datos.controlarStock,
    quantity: anterior?.quantity ?? 0,
  };
  if (codigo) valores.barcode = codigoGtin(codigo);
  const resultado = upsertRow(inventario, valores, productoId ?? null, sistema);
  if (resultado.errors)
    throw new Error(
      Object.entries(resultado.errors)
        .map(
          ([key, mensaje]) =>
            `${inventario.fields.find((f) => f.key === key)?.label ?? key}: ${mensaje}`,
        )
        .join(' '),
    );
  return resultado.row;
}
