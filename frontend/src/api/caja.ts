import { http } from './client';
import {
  ventaCajaSchema,
  consultaCajaSchema,
  cierreCajaSchema,
  cuentasCajaSchema,
  cobroDeudaCajaSchema,
  cobroQrSchema,
  type SolicitudCobro,
  type SolicitudCobroQr,
  type SolicitudCierreCaja,
  type SolicitudPagoDeudaCaja,
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

export const listarCuentasCaja = (proyectoId: string, inventarioId: string) =>
  http.get(`/caja/ventas?${new URLSearchParams({ proyectoId, inventarioId })}`, cuentasCajaSchema);
export const registrarPagoDeudaCaja = (request: SolicitudPagoDeudaCaja) =>
  http.post('/caja/cobros', request, cobroDeudaCajaSchema);

export const generarCobroQr = (request: SolicitudCobroQr) =>
  http.post('/caja/cobros-qr', request, cobroQrSchema);
export const consultarCobroQr = (id: string) =>
  http.get(`/caja/cobros-qr/${encodeURIComponent(id)}`, cobroQrSchema);
export const cancelarCobroQr = (id: string) =>
  http.post(`/caja/cobros-qr/${encodeURIComponent(id)}/cancelar`, {}, cobroQrSchema);
