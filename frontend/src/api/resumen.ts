import { avisoEconomicoResponseSchema } from '@/types/avisoEconomico';
import { resumenInicioSchema } from '@/types/resumenInicio';
import { http } from './client';

/** Los días que cubre el resumen, en formato aaaa-mm-dd; sin ellos el servidor usa los últimos 30 días. */
export type PeriodoDelResumen = { desde?: string; hasta?: string };

/** El resumen del negocio de un sistema, como PDF listo para guardar. */
export function descargarResumen(
  sistemaId: string,
  periodo: PeriodoDelResumen,
  signal?: AbortSignal,
) {
  const consulta = new URLSearchParams();
  if (periodo.desde) consulta.set('desde', periodo.desde);
  if (periodo.hasta) consulta.set('hasta', periodo.hasta);
  const parametros = consulta.toString();
  return http.getArchivo(
    `/proyectos/${encodeURIComponent(sistemaId)}/resumen/pdf${parametros ? `?${parametros}` : ''}`,
    signal,
  );
}

/** Las métricas del inicio de un cuaderno: lo vendido hoy, lo que más salió, lo que falta y lo que está parado. */
export const obtenerResumenInicio = (sistemaId: string, signal?: AbortSignal) =>
  http.get(
    `/proyectos/${encodeURIComponent(sistemaId)}/resumen/inicio`,
    resumenInicioSchema,
    signal,
  );

export const obtenerAvisoEconomico = (sistemaId: string, signal?: AbortSignal) =>
  http.get(
    `/proyectos/${encodeURIComponent(sistemaId)}/resumen/aviso-economico`,
    avisoEconomicoResponseSchema,
    signal,
  );
