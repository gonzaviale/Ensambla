import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') return [...handlersAuth];
  return [];
}
