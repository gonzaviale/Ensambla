import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersImportaciones } from './importaciones';
import { handlersSamiProyecto } from './samiProyecto';
import { handlersSamiTaller } from './samiTaller';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';

// El backend todavía no tiene los sistemas, las preferencias, la lectura de fotos ni la zona: es lo que simula el modo parcial.
const handlersDelSistema = [...handlersSistemas, ...handlersImportaciones, ...handlersZona];

// Las plantillas, «Armalo con Sami» y la charla del taller con Sami ya existen en el backend: sólo se simulan en el
// modo completo.
const handlersDeSami = [...handlersSamiProyecto, ...handlersSamiTaller];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') return [...handlersAuth, ...handlersDelSistema, ...handlersDeSami];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
