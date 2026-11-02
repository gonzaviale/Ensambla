import { hoy } from './formato';

/** Un resumen de más de un año se pide por partes; el servidor pone el mismo límite. */
export const MAXIMO_DIAS_DEL_RESUMEN = 366;

export const PERIODOS_DEL_RESUMEN = [
  { clave: '7', etiqueta: 'Últimos 7 días', dias: 7 },
  { clave: '30', etiqueta: 'Últimos 30 días', dias: 30 },
  { clave: '90', etiqueta: 'Últimos 90 días', dias: 90 },
] as const;

export type ClaveDePeriodo = (typeof PERIODOS_DEL_RESUMEN)[number]['clave'] | 'otro';

const DIA_MS = 86_400_000;

const aTexto = (fecha: Date) =>
  `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;

const aFecha = (texto: string) => new Date(`${texto}T12:00:00`);

/** Los últimos `dias` días contando el de referencia (hoy por defecto): 7 días son hoy y los 6 anteriores. */
export function ultimosDias(dias: number, referencia = hoy()) {
  const desde = aFecha(referencia);
  desde.setDate(desde.getDate() - (dias - 1));
  return { desde: aTexto(desde), hasta: referencia };
}

/** Por qué no se puede pedir el período elegido, o `null` si está bien. */
export function errorDelPeriodo(desde: string, hasta: string): string | null {
  if (!desde || !hasta) return 'Elegí las dos fechas.';
  if (desde > hasta) return 'La fecha desde no puede ser posterior a la fecha hasta.';
  const dias = Math.round((aFecha(hasta).getTime() - aFecha(desde).getTime()) / DIA_MS) + 1;
  if (dias > MAXIMO_DIAS_DEL_RESUMEN) {
    return `El resumen no puede cubrir más de ${MAXIMO_DIAS_DEL_RESUMEN} días.`;
  }
  return null;
}
