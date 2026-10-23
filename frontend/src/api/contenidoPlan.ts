import { http } from './client';
import { accesoContenidoSchema } from '@/types/contenidoPlan';

export const desbloquearAvisos = (claves: string[], signal?: AbortSignal) =>
  http.post('/planes/contenidos', { tipo: 'AVISOS', claves }, accesoContenidoSchema, signal);
