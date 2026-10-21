import {
  direccionesSchema,
  localidadesDelMapaSchema,
  rubrosSchema,
  type Punto,
} from '@/types/ubicacion';
import { http } from './client';

export const buscarDirecciones = (consulta: string, signal?: AbortSignal) =>
  http.post('/ubicacion/direcciones', { consulta }, direccionesSchema, signal);

export const resolverLocalidad = (punto: Punto, signal?: AbortSignal) =>
  http.post('/ubicacion/localidad', punto, localidadesDelMapaSchema, signal);

export const listarRubros = (signal?: AbortSignal) =>
  http.get('/ubicacion/rubros', rubrosSchema, signal);
