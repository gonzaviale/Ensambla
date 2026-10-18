import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersImportaciones } from './importaciones';
import { handlersSami } from './sami';
import { handlersSistemas } from './sistemas';

// El backend todavía no tiene los sistemas, las preferencias, a Sami ni la lectura de fotos: es lo que simula el modo parcial.
const handlersDelSistema = [...handlersSistemas, ...handlersSami, ...handlersImportaciones];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') return [...handlersAuth, ...handlersDelSistema];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
