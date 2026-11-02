import { pesos, type AccionDeAviso, type Aviso, type TipoDeAviso } from '@/lib/sistema/alertas';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

export const ICONO_DE_AVISO: Record<TipoDeAviso, NombreDeIcono> = {
  stock: 'box',
  precio_zona: 'chart',
  estancado: 'clock',
  muy_vendido: 'spark',
  combo: 'orders',
  consumo_zona: 'users',
  revisar_precio: 'undo',
};

/** El nombre corto de cada tipo: lo primero que se lee para saber de qué se trata. */
export const NOMBRE_DE_TIPO: Record<TipoDeAviso, string> = {
  stock: 'Stock',
  precio_zona: 'Precio vs. zona',
  estancado: 'Sin movimiento',
  muy_vendido: 'Se vende mucho',
  combo: 'Combo',
  consumo_zona: 'Lo que pide la zona',
  revisar_precio: 'Revisar el precio',
};

export const NIVEL_DE_AVISO = { 3: 'Urgente', 2: 'Para hoy', 1: 'Para mirar' } as const;

/** Lo que dice el botón: lo que modifica los datos arranca con «Aplicar», salvo sumar algo nuevo, que ya se entiende solo. */
export function textoDeAccion(accion: AccionDeAviso) {
  if (accion.tipo === 'aplicar' || accion.tipo === 'servidor') {
    return `Aplicar: ${accion.texto.charAt(0).toLowerCase()}${accion.texto.slice(1)}`;
  }
  return accion.texto;
}

const numero = (valor: unknown) => {
  const convertido = typeof valor === 'string' ? Number(valor) : valor;
  return typeof convertido === 'number' && Number.isFinite(convertido) ? convertido : undefined;
};

export type DatoDelAviso = { etiqueta: string; valor: string; tono?: 'bueno' | 'malo' };

/** Los números del aviso listos para mostrar. Vacío cuando el aviso no los trae (los calculados en el navegador). */
export function datosDelAviso(aviso: Aviso): DatoDelAviso[] {
  const datos = aviso.datos ?? {};
  if (aviso.tipo === 'stock') {
    const cantidad = numero(datos.cantidad);
    const minimo = numero(datos.minimo);
    if (cantidad === undefined) return [];
    return [
      { etiqueta: 'Te quedan', valor: `${cantidad} u.`, tono: 'malo' },
      ...(minimo ? [{ etiqueta: 'Tu mínimo', valor: `${minimo} u.` }] : []),
    ];
  }
  if (aviso.tipo === 'estancado') {
    const precio = numero(datos.precio);
    const sugerido = numero(datos.sugerido);
    if (precio === undefined || sugerido === undefined) return [];
    const baja = precio > 0 ? Math.round(((precio - sugerido) / precio) * 100) : 0;
    return [
      { etiqueta: 'Lo tenés a', valor: pesos(precio), tono: 'malo' },
      { etiqueta: 'Precio sugerido', valor: pesos(sugerido), tono: 'bueno' },
      ...(baja > 0 ? [{ etiqueta: 'Rebaja', valor: `${baja} %` }] : []),
    ];
  }
  if (aviso.tipo === 'revisar_precio') {
    const anterior = numero(datos.precioAnterior);
    const nuevo = numero(datos.precioNuevo);
    if (anterior === undefined || nuevo === undefined) return [];
    return [
      { etiqueta: 'Precio anterior', valor: pesos(anterior), tono: 'bueno' },
      { etiqueta: 'Precio actual', valor: pesos(nuevo), tono: 'malo' },
    ];
  }
  return [];
}

/** Qué se propone hacer, en una línea y con números cuando se conocen. */
export function propuestaDelAviso(aviso: Aviso): string | undefined {
  const accion = aviso.acciones[0];
  if (!accion) return undefined;
  const datos = aviso.datos ?? {};
  if (aviso.tipo === 'estancado') {
    const precio = numero(datos.precio);
    const sugerido = numero(datos.sugerido);
    if (precio !== undefined && sugerido !== undefined) {
      return `Bajar el precio de ${pesos(precio)} a ${pesos(sugerido)}`;
    }
  }
  return accion.texto;
}

/** «hoy», «ayer» o «hace N días», para saber cuánto hace que el motor lo detectó. */
export function haceCuanto(creadoEn: string | undefined, ahora = new Date()): string | undefined {
  if (!creadoEn) return undefined;
  const fecha = new Date(creadoEn);
  if (Number.isNaN(fecha.getTime())) return undefined;
  const dias = Math.floor((ahora.getTime() - fecha.getTime()) / 86_400_000);
  if (dias <= 0) return 'hoy';
  if (dias === 1) return 'ayer';
  return `hace ${dias} días`;
}
