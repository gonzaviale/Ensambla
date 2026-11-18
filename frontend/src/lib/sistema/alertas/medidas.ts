import { normalize } from '../core';
import type { Valor } from '../tipos';
import type { ProductoDeZona, Zona } from '../zona';

const VACIAS = new Set([
  'de',
  'del',
  'la',
  'el',
  'los',
  'las',
  'con',
  'sin',
  'por',
  'para',
  'kg',
  'gr',
  'ml',
  'lts',
  'unidad',
  'unidades',
  'pack',
  'docena',
]);

const DIA_MS = 86_400_000;

export const palabras = (texto: string) =>
  normalize(texto)
    .replace(/[^a-z0-9 ]+/g, ' ')
    .split(/\s+/)
    .filter((palabra) => palabra.length >= 3 && !VACIAS.has(palabra) && !/^\d/.test(palabra));

/** Qué tan parecido es un producto tuyo a otro: comparten la primera palabra y suman por cada palabra común. */
export function parecido(tuyo: string, otro: string) {
  const a = palabras(tuyo);
  const b = new Set(palabras(otro));
  if (!a.length || !b.has(a[0] ?? '')) return 0;
  return a.filter((palabra) => b.has(palabra)).length / a.length;
}

/** El producto de la zona que es el mismo que el tuyo (mismo nombre, salvo el tamaño o la marca extra). */
export const productoDeZona = (nombre: string, zona: Zona): ProductoDeZona | undefined =>
  zona.productos.find((producto) => parecido(nombre, producto.nombre) === 1);

export const pesos = (valor: number) =>
  `$ ${Math.round(Number(valor) || 0).toLocaleString('es-AR')}`;

export const porcentaje = (valor: number) => `${Math.round(valor * 100)}%`;

export const aDiez = (valor: number) => Math.round(valor / 10) * 10;

export const numero = (valor: Valor) => Number(valor ?? 0) || 0;

export const diasEntre = (hoy: string, fecha: string) =>
  Math.floor((Date.parse(`${hoy}T12:00:00`) - Date.parse(`${fecha}T12:00:00`)) / DIA_MS);

export const fechaCorta = (iso: string) => {
  const [, mes, dia] = iso.split('-');
  return `${Number(dia)}/${Number(mes)}`;
};

export const mediana = (valores: number[]) => {
  if (!valores.length) return 0;
  const ordenados = [...valores].sort((a, b) => a - b);
  const medio = Math.floor(ordenados.length / 2);
  if (ordenados.length % 2) return ordenados[medio] ?? 0;
  return ((ordenados[medio - 1] ?? 0) + (ordenados[medio] ?? 0)) / 2;
};

export const cuantosProductos = (cantidad: number) =>
  cantidad === 1 ? '1 producto' : `${cantidad} productos`;
