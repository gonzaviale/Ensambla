import { esObjeto } from './argumentos';
import { agregarCampo, editarCampo, quitarCampo } from './campos';
import {
  agregarPieza,
  cambiarDistribucion,
  conectar,
  quitarPieza,
  renombrarPieza,
  renombrarSistema,
  separarEnSeccion,
} from './piezas';
import { borrarRegistros, cargarEjemplos, cargarRegistros, editarRegistros } from './registros';
import type { AccionDeSami } from './tipos';
import type { Herramienta, Turno } from './turno';

const HERRAMIENTAS = new Map<string, Herramienta>([
  ['agregar_pieza', agregarPieza],
  ['cargar_ejemplos', cargarEjemplos],
  ['cargar_registros', cargarRegistros],
  ['editar_registros', editarRegistros],
  ['borrar_registros', borrarRegistros],
  ['conectar', conectar],
  ['quitar_pieza', quitarPieza],
  ['renombrar_pieza', renombrarPieza],
  ['renombrar_sistema', renombrarSistema],
  ['agregar_campo', agregarCampo],
  ['editar_campo', editarCampo],
  ['quitar_campo', quitarCampo],
  ['separar_en_seccion', separarEnSeccion],
  ['cambiar_distribucion', cambiarDistribucion],
]);

/** Aplica una acción con su herramienta. Devuelve el motivo si no se pudo aplicar. */
export function aplicarUna(
  turno: Turno,
  { herramienta, argumentos }: AccionDeSami,
): string | undefined {
  const aplicar = HERRAMIENTAS.get(herramienta);
  if (!aplicar) return `No sé hacer «${herramienta}».`;
  return aplicar(turno, esObjeto(argumentos) ? argumentos : {});
}
