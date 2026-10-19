import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersAvisos } from './avisos';
import { handlersImportaciones } from './importaciones';
import { handlersSami } from './sami';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';
import { handlersUbicacion } from './ubicacion';
import { handlersNegocio } from './negocio';

// El backend todavía no tiene los sistemas, las preferencias, a Sami ni la zona: es lo que simula el modo parcial.
// La lectura de fotos, los avisos y la búsqueda de ubicación y rubros usan el backend en modo parcial.
const handlersDelSistema = [...handlersSistemas, ...handlersSami, ...handlersZona];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo')
    return [
      ...handlersAuth,
      ...handlersDelSistema,
      ...handlersImportaciones,
      ...handlersAvisos,
      ...handlersUbicacion,
      ...handlersNegocio,
    ];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
