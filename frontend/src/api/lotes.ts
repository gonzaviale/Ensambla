import { z } from 'zod';
import { lecturaSchema } from '@/types/lectura';
import { loteSchema } from '@/types/lote';
import { http } from './client';

// El backend valida tipo y tamaño de cada foto; el campo multipart se llama "fotos".
export const subirLote = (fotos: Blob[]) => {
  const datos = new FormData();
  fotos.forEach((foto, i) => datos.append('fotos', foto, `pagina-${i + 1}.jpg`));
  return http.postForm('/lotes', datos, loteSchema);
};

export const obtenerLectura = (loteId: string, signal?: AbortSignal) =>
  http.get(`/lotes/${loteId}/lectura`, lecturaSchema, signal);

// Responde 202 sin cuerpo.
export const reintentarLectura = (loteId: string) =>
  http.post(`/lotes/${loteId}/reintentar`, undefined, z.undefined());
