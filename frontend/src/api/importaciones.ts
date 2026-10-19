import type { Lectura, PedidoDeLectura } from '@/types/importacion';
import { lecturaSchema } from '@/types/importacion';
import { http } from './client';

export const pedirLectura = (pedido: PedidoDeLectura): Promise<Lectura> =>
  http.post('/importaciones/lectura', pedido, lecturaSchema);
