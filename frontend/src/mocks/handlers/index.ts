import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersAvisos } from './avisos';
import { handlersImportaciones } from './importaciones';
import { handlersSami } from './sami';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';

// El backend todavía no tiene los sistemas, las preferencias, a Sami ni la zona: es lo que simula el modo parcial.
// La lectura de fotos y los avisos del motor de monitoreo sí están en el backend, así que sólo se simulan en el modo
// completo.
const handlersDelSistema = [...handlersSistemas, ...handlersSami, ...handlersZona];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo')
    return [...handlersAuth, ...handlersDelSistema, ...handlersImportaciones, ...handlersAvisos];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
