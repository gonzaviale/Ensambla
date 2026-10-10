import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersLotes } from './lotes';
import { handlersPlantillas } from './plantillas';
import { handlersProyectos } from './proyectos';
import { handlersRegistros } from './registros';

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') {
    return [
      ...handlersAuth,
      ...handlersPlantillas,
      ...handlersProyectos,
      ...handlersRegistros,
      ...handlersLotes,
    ];
  }
  // Todos los endpoints actuales ya existen en el backend.
  if (modo === 'parcial') return [];
  return [];
}
