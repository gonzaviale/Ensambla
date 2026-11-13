import { clone, findBlock, upsertRow } from '@/lib/sistema/core';
import type { Bloque, Campo, ErroresFila, Fila, Sistema, Valor } from '@/lib/sistema/tipos';
import type { BorradorProductoInventario } from '@/types/escaneo';
import { codigoGtin, mismoGtin } from './codigo';

export const buscarProductoPorCodigo = (inventario: Bloque, codigo: string) =>
  inventario.rows.find((fila) => mismoGtin(fila.barcode, codigo));

export function prepararProducto(
  inventario: Bloque,
  codigo: string,
  producto?: Fila,
): BorradorProductoInventario {
  const valores = producto
    ? { ...producto }
    : Object.fromEntries(
        inventario.fields.map((campo) => [campo.key, campo.type === 'checkbox' ? false : '']),
      );
  return { productoId: producto?.id ?? null, codigo, valores, originales: { ...valores } };
}

function comparable(campo: Campo, valor: Valor) {
  if (campo.type === 'checkbox') return Boolean(valor);
  if (valor == null || valor === '') return '';
  return campo.type === 'number' || campo.type === 'currency' ? Number(valor) : String(valor);
}

type Resultado =
  | { sistema: Sistema; producto: Fila; errores?: never }
  | { errores: ErroresFila; sistema?: never; producto?: never };

/** Suma sobre el stock vigente y conserva los demás datos del producto. */
export function sumarStockProductoEscaneado(
  sistema: Sistema,
  inventarioId: string,
  productoId: string,
  codigo: string,
  entrada: string,
): Resultado {
  const cantidad = Number(entrada);
  if (!entrada.trim() || !Number.isFinite(cantidad) || cantidad <= 0)
    return { errores: { quantity: 'Ingresá una cantidad mayor que cero para agregar.' } };
  const inventario = findBlock(sistema, inventarioId);
  if (!inventario || inventario.type !== 'inventory')
    return { errores: { _form: 'Este inventario ya no está disponible.' } };
  const producto = inventario.rows.find((fila) => fila.id === productoId);
  if (!producto)
    return { errores: { _form: 'El producto fue eliminado. Volvé a buscar el código.' } };
  if (!mismoGtin(producto.barcode, codigo))
    return {
      errores: {
        _form: 'El código de este producto cambió. Volvé a escanearlo antes de sumar stock.',
      },
    };
  if (!inventario.fields.some((campo) => campo.key === 'quantity' && campo.type === 'number'))
    return {
      errores: {
        _form: 'Este inventario necesita un campo numérico de cantidad para sumar stock.',
      },
    };
  const actual = Number(producto.quantity ?? 0);
  const total = actual + cantidad;
  if (!Number.isFinite(actual) || actual < 0)
    return {
      errores: { _form: 'No pudimos leer el stock actual. Revisá el producto en el inventario.' },
    };
  if (!Number.isFinite(total) || total > Number.MAX_SAFE_INTEGER || total <= actual)
    return {
      errores: { quantity: 'No se puede sumar esa cantidad. Revisá las unidades a agregar.' },
    };
  const borrador = prepararProducto(inventario, String(producto.barcode), producto);
  borrador.valores.quantity = total;
  const resultado = guardarProductoEscaneado(sistema, inventarioId, borrador);
  if (
    resultado.errores &&
    Object.keys(resultado.errores).some((clave) => !['quantity', '_form'].includes(clave))
  )
    return {
      errores: {
        _form: 'Revisá los datos de este producto en el inventario antes de sumar stock.',
      },
    };
  return resultado;
}

/** Usa las validaciones del cuaderno sobre una copia; no crea movimientos ni ventas. */
export function guardarProductoEscaneado(
  sistema: Sistema,
  inventarioId: string,
  borrador: BorradorProductoInventario,
): Resultado {
  let codigo: string;
  try {
    codigo = codigoGtin(borrador.codigo);
  } catch (e) {
    return { errores: { _form: e instanceof Error ? e.message : 'Revisá el código de barras.' } };
  }
  const copia = clone(sistema);
  const inventario = findBlock(copia, inventarioId);
  if (!inventario || inventario.type !== 'inventory')
    return { errores: { _form: 'Este inventario ya no está disponible.' } };
  const propio = inventario.rows.find((fila) => fila.id === borrador.productoId);
  if (borrador.productoId && !propio)
    return { errores: { _form: 'El producto fue eliminado. Volvé a buscar el código.' } };
  const repetido = buscarProductoPorCodigo(inventario, codigo);
  if (repetido && repetido.id !== propio?.id)
    return {
      errores: {
        _form: 'Este código ya está cargado. Volvé a buscarlo para revisar el producto existente.',
      },
    };
  if (propio?.barcode && !mismoGtin(propio.barcode, codigo))
    return {
      errores: {
        _form:
          'El producto elegido ya tiene otro código. Revisá la presentación antes de vincularlo.',
      },
    };

  const valores = propio ? { ...propio } : { ...borrador.valores };
  const errores: ErroresFila = {};
  for (const campo of inventario.fields) {
    if (campo.key === 'barcode') continue;
    const propuesto = borrador.valores[campo.key];
    if (propio) {
      const original = comparable(campo, borrador.originales[campo.key]);
      if (comparable(campo, propuesto) === original) continue;
      if (comparable(campo, propio[campo.key]) !== original) {
        errores[campo.key] = 'Este dato cambió mientras lo revisabas. Volvé a buscar el producto.';
        continue;
      }
      valores[campo.key] = propuesto;
    }
    if (
      ['quantity', 'price', 'cost', 'minimum'].includes(campo.key) &&
      propuesto !== '' &&
      propuesto != null &&
      Number(propuesto) < 0
    )
      errores[campo.key] = 'Ingresá un valor mayor o igual a cero.';
  }
  if (Object.keys(errores).length) return { errores };
  const campoCodigo = inventario.fields.find((campo) => campo.key === 'barcode');
  if (campoCodigo && campoCodigo.type !== 'text')
    return {
      errores: {
        _form: 'El campo de código de barras debe ser de texto para conservar los ceros iniciales.',
      },
    };
  if (!campoCodigo)
    inventario.fields.push({
      key: 'barcode',
      label: 'Código de barras',
      type: 'text',
      required: false,
    });
  valores.barcode = codigo;
  const resultado = upsertRow(inventario, valores, borrador.productoId, copia);
  return resultado.errors
    ? { errores: resultado.errors }
    : { sistema: copia, producto: resultado.row };
}
