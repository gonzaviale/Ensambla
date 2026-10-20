import { direccionesSchema, rubrosSchema } from '@/types/ubicacion';
import { http } from './client';

export const buscarDirecciones = (consulta: string, signal?: AbortSignal) =>
  http.post('/ubicacion/direcciones', { consulta }, direccionesSchema, signal);

export const listarRubros = (signal?: AbortSignal) =>
  http.get('/ubicacion/rubros', rubrosSchema, signal);
