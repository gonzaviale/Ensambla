import { respuestaDelTallerSchema, type PedidoAlTaller } from '@/types/samiTaller';
import { http } from './client';

export const conversarEnTaller = (pedido: PedidoAlTaller) =>
  http.post('/sami/taller', pedido, respuestaDelTallerSchema);
