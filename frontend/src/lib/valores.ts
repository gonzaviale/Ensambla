import type { Campo } from '@/types/proyecto';
import type { Valor } from '@/types/registro';

export const SIN_DATO = '—';

/** Cómo está el texto de una fila de otra tabla a la que apunta una columna RELACION. */
export type EtiquetaRelacion =
  | { estado: 'lista'; texto: string }
  | { estado: 'cargando' }
  | { estado: 'eliminada' }
  | { estado: 'error' };

const TEXTO_RELACION: Record<Exclude<EtiquetaRelacion['estado'], 'lista'>, string> = {
  cargando: 'Cargando…',
  eliminada: 'Fila eliminada',
  error: 'No pudimos cargarla',
};

const formatoNumero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });
const formatoCantidad = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 });
const formatoDinero = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const formatoMomento = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

function formatearFecha(iso: string) {
  const [anio, mes, dia] = iso.split('-');
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : iso;
}

/** Texto de una conexión con otra tabla; sin etiqueta todavía, se está cargando. */
export function describirEtiqueta(etiqueta: EtiquetaRelacion | undefined) {
  if (!etiqueta) return TEXTO_RELACION.cargando;
  return etiqueta.estado === 'lista' ? etiqueta.texto : TEXTO_RELACION[etiqueta.estado];
}

// Cómo se muestra un dato según el tipo de su campo: fecha dd/mm/aaaa, sí/no y números con separadores.
// Una conexión con otra tabla guarda el id de la fila: se muestra su etiqueta (ver useEtiquetas).
export function formatearValor(
  tipo: Campo['tipo'],
  valor: Valor | undefined,
  etiqueta?: EtiquetaRelacion,
): string {
  if (valor === null || valor === undefined || valor === '') return SIN_DATO;
  if (tipo === 'SI_NO') return valor === true ? 'Sí' : 'No';
  if ((tipo === 'NUMERO' || tipo === 'FORMULA') && typeof valor === 'number') return formatoNumero.format(valor);
  if (tipo === 'FECHA' && typeof valor === 'string') return formatearFecha(valor);
  if (tipo === 'RELACION') return describirEtiqueta(etiqueta);
  return String(valor);
}

// Fecha y hora de carga o de modificación de un registro (llegan como instante ISO).
export function formatearMomento(iso: string) {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? iso : formatoMomento.format(fecha);
}

/** Una cantidad de stock con separadores y hasta tres decimales: 1.250,5. */
export const formatearCantidad = (cantidad: number) => formatoCantidad.format(cantidad);

/** Un importe en pesos con separadores y dos decimales: $ 1.250,50. */
export const formatearDinero = (importe: number) => formatoDinero.format(importe);
