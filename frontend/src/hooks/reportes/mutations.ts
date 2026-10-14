import { useMutation } from '@tanstack/react-query';
import { descargarReporte } from '@/api/reportes';
import { guardarArchivo } from '@/lib/descarga';
import type { FiltroReporte } from '@/types/reporte';

/** Descarga el reporte como XLSX con los mismos filtros que se ven en pantalla. */
export function useDescargarReporte(bloqueId: string) {
  return useMutation({
    mutationFn: (filtro: FiltroReporte) => descargarReporte(bloqueId, filtro),
    onSuccess: ({ blob, nombre }) => guardarArchivo(blob, nombre),
  });
}
