import { reporteSchema, type FiltroReporte } from '@/types/reporte';
import { http } from './client';

function aQuery(filtro: FiltroReporte) {
  const params = new URLSearchParams();
  for (const [clave, valor] of Object.entries(filtro)) {
    if (valor !== undefined && valor !== '') params.set(clave, valor);
  }
  const query = params.toString();
  return query ? `?${query}` : '';
}

export const generarReporte = (bloqueId: string, filtro: FiltroReporte, signal?: AbortSignal) =>
  http.get(
    `/bloques/${encodeURIComponent(bloqueId)}/reporte${aQuery(filtro)}`,
    reporteSchema,
    signal,
  );

// El mismo reporte, con todas las filas que cumplen el filtro, como planilla XLSX.
export const descargarReporte = (bloqueId: string, filtro: FiltroReporte) =>
  http.getArchivo(`/bloques/${encodeURIComponent(bloqueId)}/reporte/descarga${aQuery(filtro)}`);
