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
import { handlersPromociones } from './promociones';
import { handlersReportes } from './reportes';
import { handlersReposicion } from './reposicion';
import { handlersWhatsapp } from './whatsapp';

// Las rutas actuales ya existen en el backend. Sólo el modo completo las simula para tests y demos.
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
      ...handlersReportes,
      ...handlersReposicion,
      ...handlersPromociones,
      ...handlersWhatsapp,
      ...handlersSamiProyecto,
      ...handlersSamiTaller,
    ];
  if (modo === 'parcial') return []; // Las rutas ya implementadas usan el backend real.
  return [];
}
