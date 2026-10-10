import { http } from './client';
import { registroSchema, type ValoresRegistro } from '@/types/registro';

export function crearRegistro(bloqueId: string, valores: ValoresRegistro) {
  return http.post(`/bloques/${bloqueId}/registros`, { valores }, registroSchema);
}
