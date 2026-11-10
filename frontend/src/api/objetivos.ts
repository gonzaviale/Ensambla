import { z } from 'zod';
import {
  objetivoDelServidorSchema,
  objetivosDelServidorSchema,
  type NuevoObjetivo,
} from '@/types/objetivos';
import { http } from './client';

// Cada negocio ve solo los suyos: el servidor los busca por la sesión, nunca por un dato que mande el navegador.
export const listarObjetivos = (signal?: AbortSignal) =>
  http.get('/objetivos', objetivosDelServidorSchema, signal);

export const crearObjetivo = (objetivo: NuevoObjetivo) =>
  http.post('/objetivos', objetivo, objetivoDelServidorSchema);

// Un objetivo ajeno responde 404, igual que uno que no existe.
export const eliminarObjetivo = (id: string) =>
  http.delete(`/objetivos/${encodeURIComponent(id)}`, z.undefined());
