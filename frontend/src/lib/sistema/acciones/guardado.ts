import { allBlocks, camposDeVinculo, clone } from '../core';
import type { Bloque, Sistema } from '../tipos';
import { vaciarLosValoresQueApuntanA } from './piezasDelSistema';
import { quitarLasMarcasDeStock } from './stock';

/** Dos sistemas son iguales si se guardarían igual: sirve para saber si un ajuste cambió algo. */
export const mismoSistema = (a: Sistema, b: Sistema) =>
  JSON.stringify({ ...a, updatedAt: '' }) === JSON.stringify({ ...b, updatedAt: '' });

/**
 * El sistema que se guarda después de ajustarlo con Sami. Como al crear uno desde el taller, las filas de ejemplo que
 * se sumaron para imaginarlo no se guardan; las que el sistema ya tenía (las reales y sus propios ejemplos), sí. Un
 * vínculo que apuntaba a un ejemplo que no se guarda queda vacío.
 */
export function ajusteParaGuardar(original: Sistema, borrador: Sistema): Sistema {
  const sistema = clone(borrador);
  sistema.id = original.id;
  sistema.createdAt = original.createdAt;
  sistema.name = sistema.name.trim() || original.name;
  descartarEjemplos(sistema, idsDeLasFilas(original));
  return sistema;
}

const idsDeLasFilas = (sistema: Sistema) =>
  new Set(allBlocks(sistema).flatMap((bloque) => bloque.rows.map((fila) => fila.id)));

/**
 * El sistema que se crea con la propuesta del taller. Las filas de ejemplo, que estaban para imaginarlo, no pasan; los
 * registros reales (los que Sami cargó de una foto), sí. Un vínculo que apuntaba a un ejemplo queda vacío.
 */
export function sistemaParaCrear(borrador: Sistema): Sistema {
  const sistema = clone(borrador);
  sistema.name = sistema.name.trim() || 'Mi sección';
  descartarEjemplos(sistema, new Set());
  return sistema;
}

/**
 * Quita (en el lugar) las filas de ejemplo que no están en `conservar` y vacía los vínculos que apuntaban a ellas.
 * También saca las marcas con las que el taller sigue el stock de las ventas de Sami: no se guardan.
 */
function descartarEjemplos(sistema: Sistema, conservar: ReadonlySet<string>) {
  const quitadas = new Set<string>();
  for (const bloque of allBlocks(sistema)) quitarLosEjemplosDe(bloque, conservar, quitadas);
  for (const bloque of allBlocks(sistema)) {
    for (const campo of camposDeVinculo(bloque))
      vaciarLosValoresQueApuntanA(bloque, campo, quitadas);
  }
}

function quitarLosEjemplosDe(
  bloque: Bloque,
  conservar: ReadonlySet<string>,
  quitadas: Set<string>,
) {
  for (const fila of bloque.rows) {
    if (fila.__example && !conservar.has(fila.id)) quitadas.add(fila.id);
    quitarLasMarcasDeStock(fila);
  }
  bloque.rows = bloque.rows.filter((fila) => !quitadas.has(fila.id));
}
