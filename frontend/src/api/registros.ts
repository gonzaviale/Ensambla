import { z } from 'zod';
import {
  registroSchema,
  type FiltroRegistros,
  type ValoresRegistro,
} from '@/types/registro';
import { http } from './client';
import { pageSchema } from './types';

function aQuery(filtro: FiltroRegistros) {
  const params = new URLSearchParams();
  for (const [clave, valor] of Object.entries(filtro)) {
    if (valor !== undefined && valor !== '') params.set(clave, String(valor));
  }
  const query = params.toString();
  return query ? `?${query}` : '';
}

export const crearRegistro = (bloqueId: string, valores: ValoresRegistro) =>
  http.post(`/bloques/${bloqueId}/registros`, { valores }, registroSchema);

export const listarRegistros = (bloqueId: string, filtro: FiltroRegistros, signal?: AbortSignal) =>
  http.get(
    `/bloques/${encodeURIComponent(bloqueId)}/registros${aQuery(filtro)}`,
    pageSchema(registroSchema),
    signal,
  );

export const eliminarRegistro = (id: string) =>
  http.delete(`/registros/${encodeURIComponent(id)}`, z.undefined());
