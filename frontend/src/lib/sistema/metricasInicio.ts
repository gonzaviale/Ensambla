import { formatoPesos } from './formato';
import type { Sistema } from './tipos';

const unidades = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });

/** Una cantidad sin ceros de más: «3», «2,5». */
export const formatoCantidad = (valor: number) => unidades.format(valor);

/** Lo que cuenta «1 unidad» o «3 unidades». */
export const textoUnidades = (valor: number) =>
  `${formatoCantidad(valor)} ${valor === 1 ? 'unidad' : 'unidades'}`;

const tieneFilasPropias = (sistema: Sistema) =>
  sistema.sections.some((seccion) =>
    seccion.blocks.some(
      (bloque) =>
        (bloque.type === 'sales' || bloque.type === 'inventory') &&
        bloque.rows.some((fila) => !fila.__example),
    ),
  );

/**
 * Los cuadernos de los que se pueden mostrar números: los que ya tienen ventas o productos cargados de verdad
 * (las filas de ejemplo no cuentan). El que se tocó más recientemente va primero.
 */
export function cuadernosConDatos(sistemas: Sistema[]): Sistema[] {
  return sistemas
    .filter(tieneFilasPropias)
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
}

const aFecha = (iso: string) => {
  const [anio = 0, mes = 1, dia = 1] = iso.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
};

/** «lunes» para «2026-10-19». */
export const nombreDelDia = (iso: string) =>
  aFecha(iso).toLocaleDateString('es-AR', { weekday: 'long' });

/** «ayer» o «el lunes 12/10», según cuánto pasó desde `hoy`. */
export function cuandoFue(fecha: string, hoy: string) {
  const dias = Math.round((aFecha(hoy).getTime() - aFecha(fecha).getTime()) / 86_400_000);
  if (dias === 1) return 'ayer';
  const [, mes = '', dia = ''] = fecha.split('-');
  return `el ${nombreDelDia(fecha)} ${Number(dia)}/${Number(mes)}`;
}

/** Cuánto cambió lo vendido hoy contra el mismo día de la semana pasada; sin base para comparar no dice nada. */
export function comparacionConLaSemanaPasada(ingresos: number, anterior: number, hoy: string) {
  if (anterior <= 0) return null;
  const dia = nombreDelDia(hoy);
  const diferencia = ingresos - anterior;
  if (diferencia === 0) return `Igual que el ${dia} pasado`;
  const cuanto = formatoPesos(Math.abs(diferencia));
  return `${cuanto} ${diferencia > 0 ? 'más' : 'menos'} que el ${dia} pasado`;
}
