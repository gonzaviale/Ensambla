import { pesos } from '@/lib/sistema/panelDelNegocio';
import type { ResumenDeNegocio } from '@/types/negocios';

// «Comparar mis negocios» (ENS-284 · T-205): cómo se ordenan y se leen las métricas del resumen conjunto, que son
// las del panel de cada negocio (ver `lib/negocios/lista`).

export type MetricaComparable = 'vendiste' | 'gastos' | 'queda' | 'teDeben';

export type Sentido = 'desc' | 'asc';

export type Orden = { metrica: MetricaComparable; sentido: Sentido };

/** Las columnas de la tabla, en el orden en que se muestran. */
export const METRICAS: ReadonlyArray<{ clave: MetricaComparable; titulo: string }> = [
  { clave: 'vendiste', titulo: 'Vendiste' },
  { clave: 'gastos', titulo: 'Gastos' },
  { clave: 'queda', titulo: 'Te queda' },
  { clave: 'teDeben', titulo: 'Te deben' },
];

/** Arranca por lo que más vendió, que es lo primero que se quiere comparar. */
export const ORDEN_INICIAL: Orden = { metrica: 'vendiste', sentido: 'desc' };

/**
 * Los negocios ordenados por la métrica elegida. Lo que no tiene valor (un negocio sin ventas, o una métrica que no
 * se puede calcular) va siempre al final, en cualquier sentido; los empates conservan el orden del servidor.
 */
export function ordenar(negocios: ResumenDeNegocio[], { metrica, sentido }: Orden) {
  const signo = sentido === 'desc' ? -1 : 1;
  return [...negocios].sort((a, b) => {
    const izquierda = a[metrica];
    const derecha = b[metrica];
    if (izquierda === null || derecha === null) {
      return Number(izquierda === null) - Number(derecha === null);
    }
    return (izquierda - derecha) * signo;
  });
}

/** Tocar la misma columna da vuelta el sentido; otra columna arranca de mayor a menor. */
export const siguienteOrden = (actual: Orden, metrica: MetricaComparable): Orden =>
  actual.metrica === metrica
    ? { metrica, sentido: actual.sentido === 'desc' ? 'asc' : 'desc' }
    : { metrica, sentido: 'desc' };

/** El valor de una celda: los importes en pesos y lo que no hay como raya. */
export const valorDeMetrica = (negocio: ResumenDeNegocio, metrica: MetricaComparable) => {
  const valor = negocio[metrica];
  return valor === null ? '—' : pesos(valor);
};

const DIA_Y_MES = new Intl.DateTimeFormat('es-AR', {
  day: 'numeric',
  month: 'long',
  timeZone: 'UTC',
});

/** «Del 15 de octubre al 13 de noviembre»: las fechas del servidor son días, sin hora. */
export const textoDelPeriodo = (desde: string, hasta: string) =>
  `Del ${DIA_Y_MES.format(new Date(`${desde}T00:00:00Z`))} al ${DIA_Y_MES.format(
    new Date(`${hasta}T00:00:00Z`),
  )}`;
