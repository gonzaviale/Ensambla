import { z } from 'zod';
import {
  codigoVinculacionSchema,
  verificacionWhatsappSchema,
  vinculoWhatsappSchema,
  type VinculoWhatsapp,
} from '@/types/whatsapp';
import { ApiError } from './apiError';
import { http } from './client';

/** El número vinculado de la persona, o `null` si todavía no vinculó ninguno (el backend responde 404). */
export async function obtenerVinculacionWhatsapp(signal?: AbortSignal): Promise<VinculoWhatsapp | null> {
  try {
    return await http.get('/whatsapp/vinculacion', vinculoWhatsappSchema, signal);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export const solicitarVerificacionWhatsapp = (telefono: string) =>
  http.post('/whatsapp/verificacion', { telefono }, verificacionWhatsappSchema);

export const confirmarVerificacionWhatsapp = (codigo: string) =>
  http.post('/whatsapp/verificacion/confirmar', { codigo }, vinculoWhatsappSchema);

export const generarCodigoVinculacion = () =>
  http.post('/whatsapp/vinculacion/codigo', {}, codigoVinculacionSchema);

export const desvincularWhatsapp = () => http.delete('/whatsapp/vinculacion', z.undefined());
