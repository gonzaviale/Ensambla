import { comportamientoDe } from '../comportamiento';
import { allBlocks, findBlock, id, normalize } from '../core';
import { clavesReservadasDe } from '../estructuraFija';
import type { Bloque, Campo, Fila, Seccion, Sistema, Valor } from '../tipos';
import { texto } from './argumentos';

export const NO_ENCONTRE_LA_PIEZA = 'No encontré esa pieza.';

export const esVinculo = (campo: Campo) => comportamientoDe(campo.type).esVinculo();

export const enMinuscula = (pieza: Bloque) => pieza.name.toLowerCase();

export const datoPrincipal = (pieza: Bloque | undefined) => pieza?.fields[0]?.key;

export function resolverPieza(sistema: Sistema, referencia: unknown): Bloque | undefined {
  const buscada = texto(referencia);
  if (!buscada) return undefined;
  const bloques = allBlocks(sistema);
  return (
    bloques.find((bloque) => bloque.id === buscada) ??
    bloques.find((bloque) => bloque.type === buscada) ??
    bloques.find((bloque) => normalize(bloque.name) === normalize(buscada))
  );
}

export const buscarCampo = (bloque: Bloque, etiqueta: string) =>
  bloque.fields.find((campo) => normalize(campo.label) === normalize(etiqueta));

/** El dato de la pieza que Sami nombra por su etiqueta (o por su clave). */
export const campoNombrado = (pieza: Bloque, etiqueta: string) =>
  buscarCampo(pieza, etiqueta) ?? pieza.fields.find((campo) => campo.key === etiqueta);

const estaUsada = (bloque: Bloque, clave: string, reservadas: string[]) =>
  reservadas.includes(clave) || bloque.fields.some((campo) => campo.key === clave);

export const claveDeCampo = (bloque: Bloque, etiqueta: string) => {
  const base =
    normalize(etiqueta)
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'dato';
  const reservadas = clavesReservadasDe(bloque);
  let clave = base;
  for (let n = 2; estaUsada(bloque, clave, reservadas); n++) clave = `${base}_${n}`;
  return clave;
};

/** La pestaña con ese nombre (sin importar mayúsculas ni tildes); si no existe, la crea al final. */
export function seccionLlamada(sistema: Sistema, nombre: string): Seccion {
  const existente = sistema.sections.find((item) => normalize(item.name) === normalize(nombre));
  if (existente) return existente;
  const nueva: Seccion = { id: id(), name: nombre, blocks: [] };
  sistema.sections.push(nueva);
  return nueva;
}

/** Un vínculo guarda el id de la fila vinculada: esto devuelve el nombre (el dato principal) de esa fila. */
export function nombreVinculado(sistema: Sistema, campo: Campo, guardado: Valor): Valor {
  const destino = findBlock(sistema, campo.target);
  const principal = datoPrincipal(destino);
  const vinculada = destino?.rows.find((fila) => fila.id === guardado);
  return principal && vinculada ? vinculada[principal] : undefined;
}

export function vaciarLosValoresQueApuntanA(
  bloque: Bloque,
  campo: Campo,
  ids: ReadonlySet<string>,
) {
  for (const fila of bloque.rows) {
    if (ids.has(String(fila[campo.key]))) fila[campo.key] = '';
  }
}

export const borrarElDatoDeLasFilas = (filas: Fila[], clave: string) => {
  for (const fila of filas) Reflect.deleteProperty(fila, clave);
};
