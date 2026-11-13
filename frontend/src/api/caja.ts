import { http } from './client';
import { ventaCajaSchema, type SolicitudCobro } from '@/types/caja';

export const cobrarVenta = (request: SolicitudCobro) => http.post('/caja/ventas', request, ventaCajaSchema);
