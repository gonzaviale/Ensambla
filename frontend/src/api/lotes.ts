import { z } from 'zod';
import { env } from '@/config/env';
import { confirmacionSchema, lecturaSchema, type ConfirmarLectura } from '@/types/lectura';
import { loteSchema } from '@/types/lote';
import { http } from './client';

// El backend valida tipo y tamaño de cada foto; el campo multipart se llama "fotos".
export const subirLote = (fotos: Blob[]) => {
  const datos = new FormData();
  fotos.forEach((foto, i) => datos.append('fotos', foto, `pagina-${i + 1}.jpg`));
  return http.postForm('/lotes', datos, loteSchema);
};

export const obtenerLote = (loteId: string, signal?: AbortSignal) =>
  http.get(`/lotes/${encodeURIComponent(loteId)}`, loteSchema, signal);

export const obtenerLectura = (loteId: string, signal?: AbortSignal) =>
  http.get(`/lotes/${loteId}/lectura`, lecturaSchema, signal);

// Responde 202 sin cuerpo.
export const reintentarLectura = (loteId: string) =>
  http.post(`/lotes/${loteId}/reintentar`, undefined, z.undefined());

// Nada se guarda hasta acá: crea de una vez el proyecto (si hace falta), la tabla y los registros.
export const confirmarLectura = ({ loteId, datos }: { loteId: string; datos: ConfirmarLectura }) =>
  http.post(`/lotes/${encodeURIComponent(loteId)}/confirmar`, datos, confirmacionSchema);

// Las fotos son privadas: la sesión viaja en la cookie, así que un <img> same-origin las puede pedir.
export const urlFotoLote = (loteId: string, fotoId: string) =>
  `${env.apiUrl}/lotes/${encodeURIComponent(loteId)}/fotos/${encodeURIComponent(fotoId)}`;
