import type { Zona } from '@/lib/sistema/zona';
import type { PedidoDeAnalisis } from '@/types/zona';
import { zonaSchema } from '@/types/zona';
import { http } from './client';

export const obtenerZona = (signal?: AbortSignal): Promise<Zona> =>
  http.get('/zona/agregados', zonaSchema, signal);

export const analizarZona = (pedido: PedidoDeAnalisis): Promise<Zona> =>
  http.post('/zona/analisis', pedido, zonaSchema);
