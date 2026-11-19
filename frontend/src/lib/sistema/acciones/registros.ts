import { allBlocks, camposDeVinculo, id, normalize, wireSystem } from '../core';
import type { Bloque, Campo, Fila, Sistema, Valor } from '../tipos';
import {
  MAXIMO_DE_EJEMPLOS,
  MAXIMO_DE_REGISTROS,
  cantidadDeRegistros,
  cuantos,
  esObjeto,
  estaVacio,
  lista,
  texto,
  valorDeEjemplo,
} from './argumentos';
import {
  NO_ENCONTRE_LA_PIEZA,
  campoNombrado,
  enMinuscula,
  esVinculo,
  nombreVinculado,
  resolverPieza,
  vaciarLosValoresQueApuntanA,
} from './piezasDelSistema';
import { devolverStockAlBorrar } from './stock';
import type { Turno } from './turno';

type Criterio = { campo: Campo; valor: unknown }[];

type Correccion = { corregidas: Set<string>; registrosNuevos: Set<string> };

export const filasDeEjemplo = (pieza: Bloque, crudas: unknown) =>
  filasDeSami(pieza, crudas, MAXIMO_DE_EJEMPLOS, true);

/**
 * Las filas que mandó Sami (cada valor bajo la etiqueta de su dato), listas para la pieza: de ejemplo o registros
 * reales. Una fila sin el dato principal (el primero: Producto, Nombre, Pedido…) se descarta: en la tabla se vería
 * vacía.
 */
function filasDeSami(pieza: Bloque, crudas: unknown, maximo: number, deEjemplo: boolean): Fila[] {
  const principal = pieza.fields[0]?.key;
  const filas: Fila[] = [];
  for (const cruda of lista(crudas).filter(esObjeto)) {
    if (filas.length >= maximo) break;
    const fila = filaDeSami(pieza, cruda, deEjemplo);
    if (principal && fila[principal] !== undefined) filas.push(fila);
  }
  return filas;
}

function filaDeSami(pieza: Bloque, cruda: Record<string, unknown>, deEjemplo: boolean): Fila {
  const fila: Fila = deEjemplo ? { id: id(), __example: true } : { id: id() };
  for (const [etiqueta, valor] of Object.entries(cruda)) {
    const dato = datoConvertido(pieza, etiqueta, valor);
    if (dato) fila[dato.campo.key] = dato.valor;
  }
  return fila;
}

function datoConvertido(pieza: Bloque, etiqueta: string, valor: unknown) {
  const campo = campoNombrado(pieza, etiqueta);
  const convertido = campo ? valorDeEjemplo(campo, valor) : undefined;
  return campo && convertido !== undefined ? { campo, valor: convertido } : undefined;
}

function piezaQueLlevaFilas(sistema: Sistema, referencia: unknown): Bloque | string {
  const pieza = resolverPieza(sistema, referencia);
  if (!pieza) return NO_ENCONTRE_LA_PIEZA;
  return pieza.fields.length ? pieza : `«${pieza.name}» no lleva filas.`;
}

const reemplazarLosEjemplos = (pieza: Bloque, filas: Fila[]) => {
  pieza.rows = [...pieza.rows.filter((fila) => !fila.__example), ...filas];
};

export function cargarEjemplos({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = piezaQueLlevaFilas(sistema, args.pieza);
  if (typeof pieza === 'string') return pieza;
  const filas = filasDeEjemplo(pieza, args.filas);
  if (!filas.length) return `No pude cargar esos ejemplos en ${enMinuscula(pieza)}.`;
  reemplazarLosEjemplos(pieza, filas);
  return hecho(
    `Cargué ${cuantos(filas.length, 'un ejemplo', 'ejemplos')} en ${enMinuscula(pieza)}.`,
  );
}

/**
 * Lo que Sami leyó de una foto: se suma a los registros que haya y se guarda con el sistema. Las filas de ejemplo de
 * esa pieza se reemplazan: no se mezclan con los datos reales. Antes se conecta lo que el taller ya sabe conectar, así
 * un «Cliente» por nombre se busca en los clientes.
 */
export function cargarRegistros(
  { sistema, hecho, registrosNuevos }: Turno,
  args: Record<string, unknown>,
) {
  wireSystem(sistema);
  const pieza = piezaQueLlevaFilas(sistema, args.pieza);
  if (typeof pieza === 'string') return pieza;
  const filas = filasDeSami(pieza, args.filas, MAXIMO_DE_REGISTROS, false);
  if (!filas.length) return `No pude cargar esos registros en ${enMinuscula(pieza)}.`;
  reemplazarLosEjemplos(pieza, filas);
  for (const fila of filas) registrosNuevos.add(fila.id);
  return hecho(`Cargué ${cantidadDeRegistros(filas.length)} en ${enMinuscula(pieza)}.`);
}

/**
 * Corrige registros reales de una pieza, nombrados por su id: `registros` es una lista de `{id, datos: {Etiqueta:
 * valor}}`. Sólo se pisan los datos que llegan (pasados al tipo de cada dato); una fila de ejemplo, un id que no está
 * o un dato que la pieza no tiene se ignoran. Un vínculo llega por nombre y se resuelve al final, como al cargar.
 */
export function editarRegistros(
  { sistema, hecho, registrosNuevos }: Turno,
  args: Record<string, unknown>,
) {
  const pieza = resolverPieza(sistema, args.pieza);
  if (!pieza) return NO_ENCONTRE_LA_PIEZA;
  const correccion: Correccion = { corregidas: new Set(), registrosNuevos };
  for (const pedido of lista(args.registros).filter(esObjeto))
    corregirElRegistro(pieza, pedido, correccion);
  const corregidas = correccion.corregidas.size;
  if (!corregidas) return `No encontré esos registros en ${enMinuscula(pieza)}.`;
  return hecho(`Corregí ${cantidadDeRegistros(corregidas)} en ${enMinuscula(pieza)}.`);
}

function corregirElRegistro(
  pieza: Bloque,
  pedido: Record<string, unknown>,
  correccion: Correccion,
) {
  const fila = pieza.rows.find((item) => !item.__example && item.id === texto(pedido.id));
  if (!fila || !esObjeto(pedido.datos)) return;
  for (const [etiqueta, valor] of Object.entries(pedido.datos)) {
    corregirElDato(fila, datoConvertido(pieza, etiqueta, valor), correccion);
  }
}

function corregirElDato(
  fila: Fila,
  dato: ReturnType<typeof datoConvertido>,
  { corregidas, registrosNuevos }: Correccion,
) {
  if (!dato) return;
  fila[dato.campo.key] = dato.valor;
  corregidas.add(fila.id);
  if (esVinculo(dato.campo)) registrosNuevos.add(fila.id);
}

/**
 * Borra de una pieza los registros reales que Sami nombra por su id (`ids`) y los que coinciden con lo que indica en
 * `donde`: un criterio (`{Cliente: 'Ana'}`, coinciden todos sus datos) o una lista de criterios (basta con que coincida
 * uno). Nunca borra de más: un criterio vacío, o con un dato que la pieza no tiene, no vale y no se usa; si no queda
 * ni un id ni un criterio, no se borra nada. No toca otras piezas, salvo para vaciar los vínculos que apuntaban a lo
 * borrado.
 */
export function borrarRegistros({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  if (!pieza) return NO_ENCONTRE_LA_PIEZA;
  const criterios = criteriosDeBorrado(pieza, args.donde);
  const porId = new Set(lista(args.ids).map(texto).filter(Boolean));
  if (!criterios.length && !porId.size) {
    return `Para borrar registros de ${enMinuscula(pieza)} necesito saber cuáles.`;
  }
  const sobran = pieza.rows.filter((fila) => seBorra(sistema, fila, porId, criterios));
  if (!sobran.length) return `No encontré registros de ${enMinuscula(pieza)} que coincidan.`;
  borrarLasFilas(sistema, pieza, sobran);
  return hecho(`Borré ${cantidadDeRegistros(sobran.length)} de ${enMinuscula(pieza)}.`);
}

function criteriosDeBorrado(pieza: Bloque, donde: unknown): Criterio[] {
  const pedidos = esObjeto(donde) ? [donde] : lista(donde);
  return pedidos.filter(esObjeto).flatMap((pedido) => criterioCompleto(pieza, pedido));
}

function criterioCompleto(pieza: Bloque, pedido: Record<string, unknown>): Criterio[] {
  const datos = Object.entries(pedido).map(([etiqueta, valor]) => ({
    campo: campoNombrado(pieza, etiqueta),
    valor,
  }));
  const validos = datos.flatMap(({ campo, valor }) => (campo ? [{ campo, valor }] : []));
  return datos.length && validos.length === datos.length ? [validos] : [];
}

const seBorra = (sistema: Sistema, fila: Fila, porId: Set<string>, criterios: Criterio[]) =>
  (!fila.__example && porId.has(fila.id)) ||
  criterios.some((criterio) => cumple(sistema, fila, criterio));

const cumple = (sistema: Sistema, fila: Fila, criterio: Criterio) =>
  criterio.every(({ campo, valor }) => coincide(sistema, campo, fila[campo.key], valor));

/**
 * Si el valor guardado en la fila es el que Sami nombra. Un vínculo guarda el id de la fila vinculada: se compara
 * con el nombre de esa fila. El valor pedido se pasa antes al tipo del dato (`0,5`, «sí», una fecha de sección).
 */
function coincide(sistema: Sistema, campo: Campo, guardado: Valor, pedido: unknown): boolean {
  if (estaVacio(guardado)) return false;
  return esVinculo(campo)
    ? coincideElNombreVinculado(sistema, campo, guardado, pedido)
    : coincideElValor(campo, guardado, pedido);
}

function coincideElNombreVinculado(
  sistema: Sistema,
  campo: Campo,
  guardado: Valor,
  pedido: unknown,
) {
  const nombre = nombreVinculado(sistema, campo, guardado);
  return nombre !== undefined && normalize(nombre) === normalize(pedido);
}

function coincideElValor(campo: Campo, guardado: Valor, pedido: unknown) {
  const convertido = valorDeEjemplo(campo, pedido);
  return convertido !== undefined && normalize(guardado) === normalize(convertido);
}

function borrarLasFilas(sistema: Sistema, pieza: Bloque, sobran: Fila[]) {
  // Una venta (o entrada, o salida) ya había movido el stock: al borrarla, vuelve.
  for (const fila of sobran) devolverStockAlBorrar(sistema, pieza, fila);
  const ids = new Set(sobran.map((fila) => fila.id));
  pieza.rows = pieza.rows.filter((fila) => !ids.has(fila.id));
  for (const bloque of allBlocks(sistema)) {
    for (const campo of camposDeVinculo(bloque).filter((item) => item.target === pieza.id)) {
      vaciarLosValoresQueApuntanA(bloque, campo, ids);
    }
  }
}
