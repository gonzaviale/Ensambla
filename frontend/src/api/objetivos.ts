import { productosParaPasosSchema, type ProductoParaPaso } from '@/types/objetivos';
import { http } from './client';

export const obtenerPasosObjetivo = (signal?: AbortSignal): Promise<ProductoParaPaso[]> =>
  http.get('/objetivos/pasos', productosParaPasosSchema, signal);
