import { PRESETS_DE_SECCION } from './catalogo';
import { id as nuevoId, makeBlock, makeSystem, wireSystem } from './core';
import type { Bloque, Seccion, Sistema, TipoBloque } from './tipos';

// Lo que se le hace a las pestañas y a los bloques de un sistema, sin saber de dónde sale: lo usan el editor de la
// sección (sobre el sistema activo) y el taller de Sami (sobre su propuesta). Todas cambian el sistema en el lugar.

/** Suma un bloque nuevo a la pestaña (antes de `antesDeId`, o al final) y lo conecta con los demás. */
export function sumarBloque(
  sistema: Sistema,
  tipo: TipoBloque,
  seccionId: string,
  {
    antesDeId = null,
    conEjemplos = false,
  }: { antesDeId?: string | null; conEjemplos?: boolean } = {},
): Bloque | undefined {
  const seccion = sistema.sections.find((item) => item.id === seccionId);
  if (!seccion) return undefined;
  const creado = makeBlock(tipo, conEjemplos);
  const lugar = antesDeId ? seccion.blocks.findIndex((bloque) => bloque.id === antesDeId) : -1;
  seccion.blocks.splice(lugar < 0 ? seccion.blocks.length : lugar, 0, creado);
  wireSystem(sistema);
  return creado;
}

/** Arma una pestaña (vacía o con las piezas de un preset), sin ponerla todavía en el sistema. */
export function armarSeccion(nombre: string, inicio: string): Seccion {
  const preset = PRESETS_DE_SECCION[inicio];
  return {
    id: nuevoId(),
    name: nombre || preset?.name || 'Nueva pestaña',
    blocks: preset ? (makeSystem('_', preset.types).sections[0]?.blocks ?? []) : [],
  };
}

export function renombrarSeccionesEn(sistema: Sistema, nombres: [string, string][]) {
  for (const [seccionId, nombre] of nombres) {
    const seccion = sistema.sections.find((item) => item.id === seccionId);
    if (seccion && nombre) seccion.name = nombre;
  }
}

export function moverSeccionEn(sistema: Sistema, seccionId: string, direccion: 'arriba' | 'abajo') {
  const indice = sistema.sections.findIndex((seccion) => seccion.id === seccionId);
  const destino = indice + (direccion === 'arriba' ? -1 : 1);
  if (indice < 0 || destino < 0 || destino >= sistema.sections.length) return;
  const [seccion] = sistema.sections.splice(indice, 1);
  if (seccion) sistema.sections.splice(destino, 0, seccion);
}

/** La pestaña que recibe los bloques de la que se quita: la primera de las otras. */
export const seccionReceptora = (sistema: Sistema, seccionId: string) =>
  sistema.sections.find((seccion) => seccion.id !== seccionId);

/** Quita la pestaña y pasa sus bloques (con sus datos) a la receptora; devuelve su id, o `undefined` si no se pudo. */
export function quitarSeccionDe(sistema: Sistema, seccionId: string): string | undefined {
  if (sistema.sections.length < 2) return undefined;
  const receptora = seccionReceptora(sistema, seccionId);
  const origen = sistema.sections.find((seccion) => seccion.id === seccionId);
  if (!receptora || !origen) return undefined;
  receptora.blocks.push(...origen.blocks);
  sistema.sections = sistema.sections.filter((seccion) => seccion.id !== seccionId);
  return receptora.id;
}
