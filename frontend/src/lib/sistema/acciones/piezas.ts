import { CATALOG } from '../catalogo';
import {
  allBlocks,
  applyLayout,
  makeBlock,
  moveBlock,
  normalize,
  removeBlock,
  wireSystem,
} from '../core';
import type { Bloque, Campo, Distribucion, Seccion, Sistema, TipoBloque } from '../tipos';
import {
  LARGO_DE_NOMBRE,
  LARGO_DE_NOMBRE_DE_SISTEMA,
  esObjeto,
  lista,
  nombreCorto,
  texto,
} from './argumentos';
import { sumarCampo } from './campos';
import {
  NO_ENCONTRE_LA_PIEZA,
  buscarCampo,
  claveDeCampo,
  enMinuscula,
  esVinculo,
  resolverPieza,
  seccionLlamada,
} from './piezasDelSistema';
import { filasDeEjemplo } from './registros';
import type { ResultadoDeAcciones } from './tipos';
import type { Turno } from './turno';

const TIPOS_DE_PIEZA = new Set<string>(Object.keys(CATALOG));
/** La tabla a medida: es la única pieza que puede estar más de una vez (cada una con su nombre). */
const PIEZA_A_MEDIDA: TipoBloque = 'form';
const DISTRIBUCIONES = new Set<string>(['mosaic', 'single', 'two', 'three']);

const esTipoDePieza = (tipo: string): tipo is TipoBloque => TIPOS_DE_PIEZA.has(tipo);

const esDistribucion = (distribucion: string): distribucion is Distribucion =>
  DISTRIBUCIONES.has(distribucion);

const yaEstaLaPieza = (sistema: Sistema, tipo: TipoBloque) =>
  tipo !== PIEZA_A_MEDIDA && allBlocks(sistema).some((bloque) => bloque.type === tipo);

const hayUnaPiezaLlamada = (sistema: Sistema, nombre: string) =>
  allBlocks(sistema).some((bloque) => normalize(bloque.name) === normalize(nombre));

export function agregarPieza({ sistema, resultado, hecho }: Turno, args: Record<string, unknown>) {
  const tipo = texto(args.tipo);
  if (!esTipoDePieza(tipo)) return `No existe una pieza «${tipo}».`;
  if (yaEstaLaPieza(sistema, tipo)) return 'Esa pieza ya está.';
  const nombre = nombreCorto(args.nombre) || CATALOG[tipo].name;
  if (hayUnaPiezaLlamada(sistema, nombre)) return `Ya hay una pieza «${nombre}».`;
  const nombreDeSeccion = nombreCorto(args.seccion);
  const seccion = nombreDeSeccion ? seccionLlamada(sistema, nombreDeSeccion) : sistema.sections[0];
  if (!seccion) return 'No hay dónde sumar la pieza.';
  seccion.blocks.push(armarLaPieza(tipo, nombre, args, resultado));
  return hecho(descripcionDeLaPiezaSumada(nombre, nombreDeSeccion, seccion));
}

function armarLaPieza(
  tipo: TipoBloque,
  nombre: string,
  args: Record<string, unknown>,
  resultado: ResultadoDeAcciones,
): Bloque {
  const conEjemplos = lista(args.ejemplos).length > 0;
  const pieza = makeBlock(tipo, !conEjemplos);
  pieza.name = nombre;
  // El resumen y la nota no llevan datos propios: sólo se suman con su nombre.
  if (pieza.fields.length) sumarDatosYEjemplos(pieza, args, conEjemplos, resultado);
  return pieza;
}

function sumarDatosYEjemplos(
  pieza: Bloque,
  args: Record<string, unknown>,
  conEjemplos: boolean,
  resultado: ResultadoDeAcciones,
) {
  for (const campo of lista(args.campos)) {
    const motivo = esObjeto(campo) ? sumarCampo(pieza, campo) : 'No pude sumar ese dato.';
    if (motivo) resultado.omitidas.push(motivo);
  }
  if (conEjemplos) pieza.rows = filasDeEjemplo(pieza, args.ejemplos);
}

const descripcionDeLaPiezaSumada = (nombre: string, nombreDeSeccion: string, seccion: Seccion) =>
  nombreDeSeccion
    ? `Sumé ${nombre.toLowerCase()} en la pestaña ${seccion.name}.`
    : `Sumé ${nombre.toLowerCase()}.`;

export function quitarPieza({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  if (!pieza) return NO_ENCONTRE_LA_PIEZA;
  const resultado = removeBlock(sistema, pieza.id);
  if (!resultado.removed) return resultado.errors?._form ?? 'No se pudo quitar esa pieza.';
  return hecho(`Quité ${enMinuscula(pieza)}.`);
}

export function renombrarPieza({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  const nombre = nombreCorto(args.nombre);
  if (!pieza || !nombre) return 'No pude renombrar esa pieza.';
  pieza.name = nombre;
  return hecho(`Renombré la pieza a ${nombre}.`);
}

export function renombrarSistema({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const nombre = nombreCorto(args.nombre, LARGO_DE_NOMBRE_DE_SISTEMA);
  if (!nombre) return 'Falta el nombre.';
  sistema.name = nombre;
  return hecho(`Llamé a la sección ${nombre}.`);
}

export function separarEnSeccion({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  const nombre = nombreCorto(args.seccion);
  if (!pieza || !nombre) return 'No pude separar esa pieza.';
  const destino = seccionLlamada(sistema, nombre);
  if (!moveBlock(sistema, pieza.id, destino.id)) return 'No se pudo mover esa pieza.';
  return hecho(`Pasé ${enMinuscula(pieza)} a la pestaña ${destino.name}.`);
}

export function cambiarDistribucion({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const distribucion = texto(args.layout);
  if (!esDistribucion(distribucion)) return 'No conozco esa distribución.';
  applyLayout(sistema, distribucion);
  return hecho('Cambié la distribución.');
}

export function conectar({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  // Primero se conecta lo que el taller ya sabe conectar (clientes con pedidos, productos con ventas), así no se
  // duplica un vínculo que la pieza trae de fábrica.
  wireSystem(sistema);
  const desde = resolverPieza(sistema, args.desde);
  const hacia = resolverPieza(sistema, args.hacia);
  if (!desde || !hacia) return 'No encontré las piezas para conectar.';
  const motivo = motivoParaNoConectar(desde, hacia);
  if (motivo) return motivo;
  const etiqueta = (texto(args.etiqueta) || hacia.name).slice(0, LARGO_DE_NOMBRE);
  return (
    vincularElDato(desde, hacia, etiqueta) ??
    hecho(`Conecté ${enMinuscula(desde)} con ${enMinuscula(hacia)}.`)
  );
}

function motivoParaNoConectar(desde: Bloque, hacia: Bloque) {
  if (desde.id === hacia.id) return 'Una pieza no se conecta consigo misma.';
  if (!desde.fields.length || !hacia.fields.length) return 'Esas piezas no se pueden conectar.';
  const yaConectadas = desde.fields.some((campo) => esVinculo(campo) && campo.target === hacia.id);
  return yaConectadas ? `${desde.name} ya está conectada con ${enMinuscula(hacia)}.` : undefined;
}

function vincularElDato(desde: Bloque, hacia: Bloque, etiqueta: string): string | undefined {
  const existente = buscarCampo(desde, etiqueta);
  if (!existente) {
    desde.fields.push(vinculoNuevo(desde, hacia, etiqueta));
    return undefined;
  }
  if (esVinculo(existente) || existente === desde.fields[0])
    return `«${existente.label}» ya está en ${enMinuscula(desde)}.`;
  convertirEnVinculo(existente, hacia);
  return undefined;
}

const vinculoNuevo = (desde: Bloque, hacia: Bloque, etiqueta: string): Campo => ({
  key: claveDeCampo(desde, etiqueta),
  label: etiqueta,
  type: 'relation',
  required: false,
  target: hacia.id,
});

// Un dato escrito con ese nombre («Cliente» en una tabla de turnos) pasa a ser el vínculo: sus valores de ejemplo
// se buscan por nombre en la otra pieza.
function convertirEnVinculo(campo: Campo, hacia: Bloque) {
  campo.type = 'relation';
  campo.target = hacia.id;
  delete campo.options;
}
