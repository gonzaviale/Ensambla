import { normalize } from '../core';
import type { Producto } from './datos';

// Las palabras de la pregunta que no nombran un producto.
const RELLENO = new Set([
  'de',
  'del',
  'la',
  'el',
  'los',
  'las',
  'un',
  'una',
  'que',
  'cuanto',
  'cuantos',
  'cuanta',
  'quedan',
  'queda',
  'tengo',
  'hay',
  'cual',
  'es',
  'mi',
  'mis',
  'para',
  'por',
  'con',
  'sin',
  'en',
  'y',
  'o',
  'me',
  'te',
  'se',
  'lo',
  'al',
  'a',
  'stock',
  'precio',
  'vendi',
  'vendo',
  'vendio',
  'ventas',
  'venta',
  'vender',
  'cuesta',
  'sale',
  'costo',
  'margen',
  'hoy',
  'ayer',
  'semana',
  'mes',
  'dias',
  'ultimos',
  'esta',
  'este',
  'rotacion',
  'salida',
  'unidades',
  'dime',
  'decime',
  'mostrame',
  'muestrame',
  'quiero',
  'saber',
  'ver',
  'como',
  'va',
  'estan',
  'esta',
]);

const palabrasQueNombran = (pregunta: string) =>
  normalize(pregunta)
    .replace(/[^a-z0-9 ]+/g, ' ')
    .split(/\s+/)
    .filter((palabra) => palabra.length >= 3 && !RELLENO.has(palabra));

/** Los productos de la sección que la pregunta nombra (por ejemplo «yerba» o «aceite natura»). */
export function productosNombrados(pregunta: string, productos: Producto[]): Producto[] {
  const palabras = palabrasQueNombran(pregunta);
  if (!palabras.length) return [];
  const puntuados = productos
    .map((producto) => ({ producto, puntos: coincidencias(producto, palabras) }))
    .filter((item) => item.puntos > 0);
  const mejor = Math.max(0, ...puntuados.map((item) => item.puntos));
  return puntuados.filter((item) => item.puntos === mejor).map((item) => item.producto);
}

const coincidencias = (producto: Producto, palabras: string[]) => {
  const nombre = normalize(producto.nombre);
  return palabras.filter((palabra) => nombre.includes(palabra)).length;
};
