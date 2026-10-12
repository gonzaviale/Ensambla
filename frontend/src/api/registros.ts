import { z } from 'zod';
import { env } from '@/config/env';
import { referenciasSchema } from '@/types/referencia';
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

export type FiltroReferencias = { q?: string; ids?: readonly string[] };

// Filas de una tabla con el texto que las nombra, para las columnas que se conectan con ella:
// `q` busca (las 20 más nuevas) e `ids` trae sólo esas (las que ya no existen no vuelven).
export function listarReferencias(
  bloqueId: string,
  { q, ids }: FiltroReferencias,
  signal?: AbortSignal,
) {
  const params = new URLSearchParams();
  if (q) params.set('q', q);
  if (ids && ids.length > 0) params.set('ids', ids.join(','));
  const query = params.toString();
  return http.get(
    `/bloques/${encodeURIComponent(bloqueId)}/referencias${query ? `?${query}` : ''}`,
    referenciasSchema,
    signal,
  );
}

export const eliminarRegistro = (id: string) =>
  http.delete(`/registros/${encodeURIComponent(id)}`, z.undefined());

// Foto del cuaderno de la que salió el registro; sólo existe si no se cargó a mano.
export const urlFotoOrigen = (registroId: string) =>
  `${env.apiUrl}/registros/${encodeURIComponent(registroId)}/foto-origen`;
