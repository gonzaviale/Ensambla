import type { ModoMocks } from '@/config/env';
import { handlersAuth } from './auth';
import { handlersImportaciones } from './importaciones';
import { handlersSami } from './sami';
import { handlersSamiProyecto } from './samiProyecto';
import { handlersSistemas } from './sistemas';
import { handlersZona } from './zona';

// El backend todavía no tiene los sistemas, las preferencias, a Sami, la lectura de fotos ni la zona: es lo que simula el modo parcial.
const handlersDelSistema = [
  ...handlersSistemas,
  ...handlersSami,
  ...handlersImportaciones,
  ...handlersZona,
];

// completo: todo simulado | parcial: sólo lo que el backend todavía no tiene | ninguno: backend real.
// Las plantillas y «Armalo con Sami» ya existen en el backend: sólo se simulan en el modo completo.
export function handlersPara(modo: ModoMocks) {
  if (modo === 'completo') return [...handlersAuth, ...handlersDelSistema, ...handlersSamiProyecto];
  if (modo === 'parcial') return handlersDelSistema;
  return [];
}
