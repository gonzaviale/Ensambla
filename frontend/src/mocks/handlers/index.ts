import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersAvisos } from './avisos';
import { handlersImportaciones } from './importaciones';
import { handlersSamiProyecto } from './samiProyecto';
import { handlersSamiTaller } from './samiTaller';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';

// El backend todavía no tiene los sistemas, las preferencias ni la zona: es lo que simula el modo parcial.
// La lectura de fotos, los avisos del motor de monitoreo, las plantillas, «Armalo con Sami» y la charla del taller
// con Sami sí están en el backend, así que sólo se simulan en el modo completo.
const handlersDelSistema = [...handlersSistemas, ...handlersZona];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo')
    return [
      ...handlersAuth,
      ...handlersDelSistema,
      ...handlersImportaciones,
      ...handlersAvisos,
      ...handlersSamiProyecto,
      ...handlersSamiTaller,
    ];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
