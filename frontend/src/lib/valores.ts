import type { Campo } from '@/types/proyecto';
import type { Valor } from '@/types/registro';

export const SIN_DATO = '—';

const formatoNumero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });
const formatoMomento = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

function formatearFecha(iso: string) {
  const [anio, mes, dia] = iso.split('-');
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : iso;
}

// Cómo se muestra un dato según el tipo de su campo: fecha dd/mm/aaaa, sí/no y números con separadores.
export function formatearValor(tipo: Campo['tipo'], valor: Valor | undefined): string {
  if (valor === null || valor === undefined || valor === '') return SIN_DATO;
  if (tipo === 'SI_NO') return valor === true ? 'Sí' : 'No';
  if (tipo === 'NUMERO' && typeof valor === 'number') return formatoNumero.format(valor);
  if (tipo === 'FECHA' && typeof valor === 'string') return formatearFecha(valor);
  return String(valor);
}

// Fecha y hora de carga o de modificación de un registro (llegan como instante ISO).
export function formatearMomento(iso: string) {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? iso : formatoMomento.format(fecha);
}
