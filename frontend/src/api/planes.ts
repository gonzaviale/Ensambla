import { catalogoPlanesSchema, planesSchema, pruebaPremiumSchema } from '@/types/planes';
import { http } from './client';

export const consultarPlanes = (signal?: AbortSignal) => http.get('/planes', planesSchema, signal);

export const iniciarPruebaPremium = () =>
  http.post('/planes/prueba', undefined, pruebaPremiumSchema);

export const consultarCatalogoPlanes = (signal?: AbortSignal) =>
  http.get('/planes/catalogo', catalogoPlanesSchema, signal);
