import { z } from 'zod';
import { plantillaSchema, type Plantilla } from '@/types/plantilla';
import { http } from './client';

export function listarPlantillas(signal?: AbortSignal): Promise<Plantilla[]> {
  return http.get('/plantillas', z.array(plantillaSchema), signal);
}
