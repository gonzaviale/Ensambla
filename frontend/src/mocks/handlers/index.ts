import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersBloques } from './bloques';
import { handlersCampos, handlersCamposSobreBackendReal } from './campos';
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
      ...handlersBloques,
      ...handlersCampos,
      ...handlersRegistros,
    ];
  }
  // Bloques (T-017) ya está en el backend. Campos (T-018) todavía no: cuando llegue, sacar esta línea.
  if (modo === 'parcial') return [...handlersCamposSobreBackendReal];
  return [];
}
