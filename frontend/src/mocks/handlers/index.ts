import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersPlantillas } from './plantillas';
import { handlersProyectos } from './proyectos';

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') return [...handlersAuth, ...handlersPlantillas, ...handlersProyectos];
  // Todos los endpoints actuales ya existen en el backend.
  if (modo === 'parcial') return [];
  return [];
}
