import { recetaSchema, type GuardarReceta } from '@/types/receta';
import { http } from './client';

export const obtenerReceta = (registroId: string, signal?: AbortSignal) =>
  http.get(`/registros/${encodeURIComponent(registroId)}/receta`, recetaSchema, signal);

// PUT reemplaza la receta del producto; sin líneas la quita.
export const guardarReceta = (registroId: string, receta: GuardarReceta) =>
  http.put(`/registros/${encodeURIComponent(registroId)}/receta`, receta, recetaSchema);
