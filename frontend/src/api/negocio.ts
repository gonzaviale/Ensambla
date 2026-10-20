import { configuracionNegocioSchema, type CambiosNegocio } from '@/types/negocio';
import { http } from './client';

export const obtenerConfiguracionNegocio = (signal?: AbortSignal) =>
  http.get('/business', configuracionNegocioSchema, signal);

export const actualizarNegocio = (cambios: CambiosNegocio) =>
  http.patch('/business', cambios, configuracionNegocioSchema);
