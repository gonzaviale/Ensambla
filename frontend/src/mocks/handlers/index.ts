import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersSistemas } from './sistemas';

// El backend todavía no tiene los sistemas ni las preferencias: es lo que simula el modo parcial.
const handlersDelSistema = [...handlersSistemas];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') return [...handlersAuth, ...handlersDelSistema];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
