import { porcentaje } from '@/lib/red/formato';

// Ayudas puras de «Mis precios»: estado de la ganancia, lectura de números y textos de stock.

export type EstadoGanancia = 'riesgo' | 'atencion' | 'bien';

/** Cuánto por encima del mínimo se considera «justo» antes de pasar a «bien». */
const COLCHON_ATENCION = 8;

export function estadoDeGanancia(margen: number, minimo: number): EstadoGanancia {
  if (margen < minimo) return 'riesgo';
  if (margen < minimo + COLCHON_ATENCION) return 'atencion';
  return 'bien';
}

/** «9 %»; con un decimal sólo cuando el redondeo escondería que está por debajo del mínimo. */
export function textoGanancia(margen: number, minimo: number): string {
  const redondeadoAlcanza = Math.round(margen) >= minimo && margen < minimo;
  return porcentaje(margen, { decimales: redondeadoAlcanza ? 1 : 0 });
}

/** Lee un número escrito a la argentina («2.450», «2450,5») o con punto decimal («2450.5»). */
export function leerNumero(texto: string): number | null {
  const limpio = texto.replace(/[\s$]/g, '');
  if (limpio === '') return null;
  let normalizado = limpio;
  if (limpio.includes(',')) {
    normalizado = limpio.replace(/\./g, '').replace(',', '.');
  } else if (/^\d{1,3}(\.\d{3})+$/.test(limpio)) {
    normalizado = limpio.replace(/\./g, '');
  }
  if (!/^\d+(\.\d+)?$/.test(normalizado)) return null;
  const valor = Number(normalizado);
  return Number.isFinite(valor) ? valor : null;
}

/** Para comparar textos sin importar mayúsculas ni tildes. */
export const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();

/** «alcanza para 3 días», «alcanza para menos de 1 día» o «sin ventas esta semana». */
export function textoDiasDeStock(dias: number): string {
  if (!Number.isFinite(dias)) return 'sin ventas esta semana';
  if (dias < 1) return 'alcanza para menos de 1 día';
  const entero = Math.floor(dias);
  return `alcanza para ${entero} ${entero === 1 ? 'día' : 'días'}`;
}

/** Aviso que la pantalla muestra después de cambiar precios o reglas. */
export type AvisoPrecios = {
  mensaje: string;
  /** Ofrece ir al mostrador a cambiar las etiquetas. */
  mostrador?: boolean;
};

/** Ids que enlazan cada pestaña con su panel. */
export const idPestana = (prefijo: string, id: string) => `${prefijo}-pestana-${id}`;
export const idPanel = (prefijo: string, id: string) => `${prefijo}-panel-${id}`;
