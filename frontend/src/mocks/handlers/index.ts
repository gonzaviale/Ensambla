import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersImportaciones } from './importaciones';
import { handlersSami } from './sami';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';

// El backend todavía no tiene los sistemas, las preferencias, a Sami ni la zona: es lo que simula el modo parcial.
// La lectura de fotos sí está en el backend (la hace la IA), así que sólo se simula en el modo completo.
const handlersDelSistema = [...handlersSistemas, ...handlersSami, ...handlersZona];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo')
    return [...handlersAuth, ...handlersDelSistema, ...handlersImportaciones];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
