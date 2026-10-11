import { z } from 'zod';
import { env } from '@/config/env';
import {
  registroSchema,
  totalesRegistrosSchema,
  type CambiosRegistro,
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
    pageSchema(registroSchema).extend({ totales: totalesRegistrosSchema }),
    signal,
  );

// PATCH: los campos que no se envían conservan su valor; un `null` borra el dato.
export const editarRegistro = (id: string, valores: CambiosRegistro) =>
  http.patch(`/registros/${encodeURIComponent(id)}`, { valores }, registroSchema);

export const eliminarRegistro = (id: string) =>
  http.delete(`/registros/${encodeURIComponent(id)}`, z.undefined());

// Foto del cuaderno de la que salió el registro; sólo existe si no se cargó a mano.
export const urlFotoOrigen = (registroId: string) =>
  `${env.apiUrl}/registros/${encodeURIComponent(registroId)}/foto-origen`;
