import { avisoDelServidorSchema, avisosDelServidorSchema } from '@/types/avisos';
import { http } from './client';

// Los pendientes primero y del más urgente al menos urgente: es el orden en que se muestran.
export const listarAvisosPendientes = (signal?: AbortSignal) =>
  http.get('/avisos?estado=PENDIENTE', avisosDelServidorSchema, signal);

// Ejecuta la acción sugerida y cierra el aviso en una sola operación.
export const aplicarAviso = (id: string) =>
  http.post(`/avisos/${encodeURIComponent(id)}/aplicar`, undefined, avisoDelServidorSchema);

// Un aviso descartado no vuelve mientras no cambien los datos que lo originaron.
export const descartarAviso = (id: string) =>
  http.post(`/avisos/${encodeURIComponent(id)}/descartar`, undefined, avisoDelServidorSchema);
