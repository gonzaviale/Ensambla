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
 * Las secciones de las que se pueden mostrar números: las que ya tienen ventas o productos cargados de verdad
 * (las filas de ejemplo no cuentan). La que se tocó más recientemente va primero.
 */
export function sistemasConDatos(sistemas: Sistema[]): Sistema[] {
  return sistemas.filter(tieneFilasPropias).sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
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

export type PeriodoDeVentas = 'hoy' | 'semana' | 'mes';

/** Cómo se llama cada período en la pantalla. */
export const PERIODOS_DE_VENTAS: {
  clave: PeriodoDeVentas;
  boton: string;
  titulo: string;
  sin: string;
}[] = [
  { clave: 'hoy', boton: 'Hoy', titulo: 'Hoy vendiste', sin: 'Todavía no anotaste ventas de hoy' },
  {
    clave: 'semana',
    boton: 'Esta semana',
    titulo: 'Esta semana vendiste',
    sin: 'Esta semana todavía no anotaste ventas',
  },
  {
    clave: 'mes',
    boton: 'Este mes',
    titulo: 'Este mes vendiste',
    sin: 'Este mes todavía no anotaste ventas',
  },
];

/**
 * Cuánto cambió lo vendido contra el período anterior: el mismo día de la semana pasada, la semana pasada o el mes
 * pasado. Sin base para comparar no dice nada.
 */
export function comparacionConElPeriodoAnterior(
  periodo: PeriodoDeVentas,
  ingresos: number,
  anterior: number,
  hoy: string,
) {
  if (anterior <= 0) return null;
  const contra =
    periodo === 'hoy'
      ? `el ${nombreDelDia(hoy)} pasado`
      : periodo === 'semana'
        ? 'la semana pasada'
        : 'el mes pasado';
  const diferencia = ingresos - anterior;
  if (diferencia === 0) return `Igual que ${contra}`;
  return `${formatoPesos(Math.abs(diferencia))} ${diferencia > 0 ? 'más' : 'menos'} que ${contra}`;
}

/** «lun 19» para «2026-10-19»: lo que va debajo de cada barra. */
export function etiquetaDelDia(iso: string) {
  const [, , dia = ''] = iso.split('-');
  const nombre = nombreDelDia(iso).slice(0, 3);
  return `${nombre} ${Number(dia)}`;
}

/** «lunes 19/10»: el día completo, para el detalle de una barra. */
export function diaCompleto(iso: string) {
  const [, mes = '', dia = ''] = iso.split('-');
  return `${nombreDelDia(iso)} ${Number(dia)}/${Number(mes)}`;
}

/** Los días que se dibujan para cada período: el mes completo, o la última semana para hoy y la semana. */
export const diasDelGrafico = (periodo: PeriodoDeVentas) => (periodo === 'mes' ? 30 : 7);
