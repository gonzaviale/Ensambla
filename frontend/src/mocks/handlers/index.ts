import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersBloques } from './bloques';
import { handlersCampos } from './campos';
import { handlersLotes } from './lotes';
import { handlersPlantillas } from './plantillas';
import { handlersProyectos } from './proyectos';
import { handlersReceta } from './receta';
import { handlersRegistros } from './registros';
import { handlersStock } from './stock';

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
      ...handlersStock,
      ...handlersReceta,
      ...handlersLotes,
    ];
  }
  // Hoy el backend ya cubre todo lo que usa la interfaz: parcial no simula nada.
  return [];
}
