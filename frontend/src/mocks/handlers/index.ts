import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersBloques } from './bloques';
import { handlersCampos } from './campos';
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
  // Hoy el backend ya cubre todo lo que usa la interfaz: parcial no simula nada.
  return [];
}
