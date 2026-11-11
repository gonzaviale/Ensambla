import { comoVieneTuNegocioSchema } from '@/types/comoVieneTuNegocio';
import { http } from './client';

/** Cómo viene el negocio de la sesión: los últimos 30 días y las últimas seis semanas. */
export const obtenerComoVieneTuNegocio = (signal?: AbortSignal) =>
  http.get('/objetivos/negocio', comoVieneTuNegocioSchema, signal);
