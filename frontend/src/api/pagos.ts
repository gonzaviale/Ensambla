import { z } from 'zod';
import { renovacionSchema } from '@/types/pagos';
import { http } from './client';

export const consultarRenovacion = (signal?: AbortSignal) =>
  http.get('/planes/renovacion', renovacionSchema, signal);

export const contratarPremium = () => http.post('/planes/renovacion', undefined, renovacionSchema);

export const cancelarRenovacion = () =>
  http.post('/planes/renovacion/cancelar', undefined, renovacionSchema);

export const actualizarPagos = () =>
  http.post('/planes/renovacion/actualizar', undefined, z.void());
