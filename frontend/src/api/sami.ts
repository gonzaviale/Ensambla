import type { PedidoDePropuesta } from '@/types/sami';
import { propuestaSchema } from '@/types/sami';
import { http } from './client';

export const pedirPropuesta = (pedido: PedidoDePropuesta) =>
  http.post('/sami/propuesta', pedido, propuestaSchema);
