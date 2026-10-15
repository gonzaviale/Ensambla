import { useMemo } from 'react';
import { useProductos, useUmbrales } from '@/hooks/red/useRed';
import { costoDeItem, costoUnitario, variacionPct } from '@/lib/red/calculos';
import type {
  CambioPropuesto,
  ItemLista,
  ListaProveedor,
  ProductoRed,
  Umbrales,
} from '@/types/red';

// Ayudas puras de las pantallas de listas: qué líneas necesitan una mirada y qué tan seguras están.

export type CambioRaro = {
  itemId: string;
  productoId: string;
  nombre: string;
  costoAnterior: number;
  costoNuevo: number;
  variacion: number;
};

/**
 * Costo por unidad que se espera de una línea. Si la duda es «¿unidad o bulto?» y todavía no se
 * resolvió, se asume por bulto (así evitamos marcar como «raro» un precio de bulto cerrado).
 */
export function costoEsperado(item: ItemLista): number {
  const sinResolver = item.motivo === 'unidad-bulto' && !item.confirmado;
  if (sinResolver && item.unidadesPorBulto) {
    return costoUnitario(item.precioLista, 'BULTO', item.unidadesPorBulto);
  }
  return costoDeItem(item);
}

/** Líneas cuyo costo se movió más que el umbral de «cambio raro» (también las que aún no se confirmaron). */
export function cambiosRaros(
  lista: ListaProveedor,
  productos: ProductoRed[],
  umbrales: Umbrales,
): CambioRaro[] {
  const raros: CambioRaro[] = [];
  for (const item of lista.items) {
    if (item.ignorado || !item.productoId) continue;
    const producto = productos.find((p) => p.id === item.productoId);
    if (!producto) continue;
    const costoNuevo = costoEsperado(item);
    const variacion = variacionPct(producto.costo, costoNuevo);
    if (Math.abs(variacion) < umbrales.aumentoRaro) continue;
    raros.push({
      itemId: item.id,
      productoId: producto.id,
      nombre: producto.nombre,
      costoAnterior: producto.costo,
      costoNuevo,
      variacion,
    });
  }
  return raros;
}

export function useRarosDeLista(lista: ListaProveedor | undefined): CambioRaro[] {
  const productos = useProductos();
  const umbrales = useUmbrales();
  return useMemo(
    () => (lista ? cambiosRaros(lista, productos, umbrales) : []),
    [lista, productos, umbrales],
  );
}

/** Ids de las líneas que hay que mirar: las que esperan una decisión y los cambios raros. */
export function idsParaRevisar(
  lista: ListaProveedor,
  productos: ProductoRed[],
  umbrales: Umbrales,
): string[] {
  const raros = new Set(cambiosRaros(lista, productos, umbrales).map((r) => r.itemId));
  return lista.items
    .filter((item) => {
      if (item.ignorado) return false;
      const pendiente = !item.confirmado && item.estado !== 'seguro';
      return pendiente || raros.has(item.id);
    })
    .map((item) => item.id);
}

export type NivelConfianza = { texto: string; tono: 'ok' | 'warn' | 'bad' };

/** La seguridad del emparejamiento, en palabras. */
export function nivelDeConfianza(confianza: number): NivelConfianza {
  if (confianza >= 0.9) return { texto: 'Muy segura', tono: 'ok' };
  if (confianza >= 0.7) return { texto: 'Bastante segura', tono: 'warn' };
  if (confianza > 0) return { texto: 'Poco segura', tono: 'bad' };
  return { texto: 'No encontramos coincidencia', tono: 'bad' };
}

export const NOMBRE_ORIGEN_CORTO = {
  whatsapp: 'WhatsApp',
  pdf: 'PDF',
  excel: 'Excel',
  foto: 'Foto',
} as const;

/** El precio que se va a aplicar: el que escribió el comercio o, si no tocó nada, el de sus reglas. */
export function precioElegido(cambio: CambioPropuesto, precios: Record<string, number>): number {
  return precios[cambio.itemId] ?? cambio.precioSugerido;
}

/** Plata por mes que se deja de perder: lo que sube cada precio × lo que se vende en 4 semanas. */
export function proteccionMensual(
  cambios: CambioPropuesto[],
  productos: ProductoRed[],
  precios: Record<string, number>,
): number {
  return cambios.reduce((suma, cambio) => {
    const ventas = productos.find((p) => p.id === cambio.productoId)?.ventasSemana ?? 0;
    const sube = Math.max(0, precioElegido(cambio, precios) - cambio.precioActual);
    return suma + sube * ventas * 4;
  }, 0);
}
