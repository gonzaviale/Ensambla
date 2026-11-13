import { http } from './client';
import {
  ventaCajaSchema,
  consultaCajaSchema,
  cierreCajaSchema,
  type SolicitudCobro,
  type SolicitudCierreCaja,
} from '@/types/caja';

export const cobrarVenta = (request: SolicitudCobro) =>
  http.post('/caja/ventas', request, ventaCajaSchema);

export const consultarCaja = (
  proyectoId: string,
  inventarioId: string,
  fecha: string,
  pagina: number,
) => {
  const params = new URLSearchParams({ proyectoId, inventarioId, pagina: String(pagina) });
  if (fecha) params.set('fecha', fecha);
  return http.get(`/caja/resumen?${params}`, consultaCajaSchema);
};
export const cerrarCaja = (request: SolicitudCierreCaja) =>
  http.post('/caja/cierres', request, cierreCajaSchema);
export const obtenerCierreCaja = (id: string) =>
  http.get(`/caja/cierres/${encodeURIComponent(id)}`, cierreCajaSchema);
