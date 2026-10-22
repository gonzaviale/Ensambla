import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersAvisos } from './avisos';
import { handlersImportaciones } from './importaciones';
import { handlersSamiProyecto } from './samiProyecto';
import { handlersSamiTaller } from './samiTaller';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';
import { handlersUbicacion } from './ubicacion';
import { handlersNegocio } from './negocio';
import { handlersPlanes } from './planes';

// Los sistemas, las preferencias y la zona ya están en el backend (/api/sistemas, /api/preferencias, /api/zona):
// el modo parcial los simula igual para trabajar la pantalla sin levantar el servidor.
// La lectura de fotos, los avisos, la búsqueda de ubicación y rubros, las plantillas, «Armalo con Sami» y la charla
// del taller con Sami sí están en el backend, así que sólo se simulan en el modo completo.
const handlersDelSistema = [...handlersSistemas, ...handlersZona];

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
      ...handlersPlanes,
      ...handlersSamiProyecto,
      ...handlersSamiTaller,
    ];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
