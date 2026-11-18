import { FIELD_TYPES } from '../catalogo';
import { comportamientoDe } from '../comportamiento';
import { normalize } from '../core';
import type { Bloque, Campo, TipoCampo } from '../tipos';
import { MAXIMO_DE_OPCIONES, lista, llego, nombreCorto, texto, valorDeEjemplo } from './argumentos';
import {
  borrarElDatoDeLasFilas,
  buscarCampo,
  claveDeCampo,
  enMinuscula,
  esVinculo,
  resolverPieza,
} from './piezasDelSistema';
import type { Turno } from './turno';

const TIPOS_DE_CAMPO = new Set<string>([
  'text',
  'textarea',
  'number',
  'currency',
  'date',
  'time',
  'select',
  'email',
  'tel',
  'checkbox',
]);
const NOMBRE_DE_TIPO: Partial<Record<TipoCampo, string>> = { select: 'lista de opciones' };
const UNA_LISTA_NECESITA_OPCIONES = 'Una lista necesita al menos dos opciones.';
const NO_ENCONTRE_EL_DATO = 'No encontré ese dato.';

type Anotar = Turno['hecho'];

/** Lo que se pidió cambiar de un dato. Cada parte está sólo si de verdad cambia algo. */
type CambioDeCampo = {
  tipo: TipoCampo;
  etiqueta?: string;
  opciones?: string[];
  obligatorio?: boolean;
  lugar?: number;
  tipoIgnorado?: string;
};

const esTipoDeCampo = (tipo: string): tipo is TipoCampo => TIPOS_DE_CAMPO.has(tipo);

const llevaOpciones = (tipo: TipoCampo) => comportamientoDe(tipo).llevaOpciones();

const nombreDeTipo = (tipo: TipoCampo) => NOMBRE_DE_TIPO[tipo] ?? FIELD_TYPES[tipo].toLowerCase();

/** Suma un dato a la pieza. Devuelve el motivo si no se pudo. */
export function sumarCampo(pieza: Bloque, argumentos: Record<string, unknown>): string | undefined {
  const etiqueta = nombreCorto(argumentos.etiqueta);
  const tipo = texto(argumentos.tipo);
  if (!etiqueta || !esTipoDeCampo(tipo)) return 'No pude sumar ese dato.';
  if (buscarCampo(pieza, etiqueta)) return `«${etiqueta}» ya está.`;
  const opciones = lista(argumentos.opciones).map(texto).filter(Boolean);
  if (llevaOpciones(tipo) && opciones.length < 2) return UNA_LISTA_NECESITA_OPCIONES;
  pieza.fields.push(campoNuevo(pieza, etiqueta, tipo, opciones));
  return undefined;
}

const campoNuevo = (
  pieza: Bloque,
  etiqueta: string,
  tipo: TipoCampo,
  opciones: string[],
): Campo => ({
  key: claveDeCampo(pieza, etiqueta),
  label: etiqueta,
  type: tipo,
  required: false,
  ...(llevaOpciones(tipo) && { options: opciones }),
});

export function agregarCampo({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  if (!pieza) return 'No pude sumar ese dato.';
  return (
    sumarCampo(pieza, args) ?? hecho(`Sumé ${nombreCorto(args.etiqueta)} a ${enMinuscula(pieza)}.`)
  );
}

export function quitarCampo({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  const campo = pieza && buscarCampo(pieza, texto(args.campo));
  if (!pieza || !campo) return NO_ENCONTRE_EL_DATO;
  if (campo.required || esVinculo(campo)) return `«${campo.label}» no se puede quitar.`;
  pieza.fields = pieza.fields.filter((candidato) => candidato.key !== campo.key);
  borrarElDatoDeLasFilas(pieza.rows, campo.key);
  return hecho(`Quité ${campo.label} de ${enMinuscula(pieza)}.`);
}

/**
 * Cambia un dato que ya existe: el nombre (la clave no cambia, así las filas no se pierden), el tipo, las opciones,
 * si es obligatorio y su lugar en la pieza. Es todo o nada: si algo de lo pedido no vale, no se cambia nada (salvo el
 * tipo de un vínculo o del primer dato, que se ignora). Si no cambia nada, se omite.
 */
export function editarCampo({ sistema, hecho }: Turno, args: Record<string, unknown>) {
  const pieza = resolverPieza(sistema, args.pieza);
  const campo = pieza && buscarCampo(pieza, texto(args.campo));
  if (!pieza || !campo) return NO_ENCONTRE_EL_DATO;
  const cambio = cambioPedido(pieza, campo, args);
  if (typeof cambio === 'string') return cambio;
  if (noCambiaNada(campo, cambio)) return cambio.tipoIgnorado ?? `«${campo.label}» ya estaba así.`;
  aplicarElCambio(pieza, campo, cambio, hecho);
  return undefined;
}

function cambioPedido(
  pieza: Bloque,
  campo: Campo,
  args: Record<string, unknown>,
): CambioDeCampo | string {
  const cambio: CambioDeCampo = { tipo: campo.type };
  return (
    pedirEtiqueta(cambio, pieza, campo, args) ??
    pedirTipo(cambio, pieza, campo, args) ??
    pedirOpciones(cambio, campo, args) ??
    pedirObligatorio(cambio, campo, args) ??
    pedirLugar(cambio, pieza, campo, args) ??
    cambio
  );
}

function pedirEtiqueta(
  cambio: CambioDeCampo,
  pieza: Bloque,
  campo: Campo,
  args: Record<string, unknown>,
) {
  const etiqueta = nombreCorto(args.etiqueta);
  if (!etiqueta || etiqueta === campo.label) return undefined;
  const repetida = pieza.fields.some(
    (otro) => otro !== campo && normalize(otro.label) === normalize(etiqueta),
  );
  if (repetida) return `Ya hay un dato «${etiqueta}» en ${enMinuscula(pieza)}.`;
  cambio.etiqueta = etiqueta;
  return undefined;
}

// El tipo de un vínculo o del primer dato (el que nombra cada fila) no cambia: se ignora, como en el backend.
function pedirTipo(
  cambio: CambioDeCampo,
  pieza: Bloque,
  campo: Campo,
  args: Record<string, unknown>,
) {
  const tipoPedido = texto(args.tipo);
  if (!tipoPedido) return undefined;
  cambio.tipoIgnorado = motivoParaNoCambiarElTipo(pieza, campo);
  if (cambio.tipoIgnorado) return undefined;
  if (!esTipoDeCampo(tipoPedido)) return `No conozco el tipo «${tipoPedido}».`;
  cambio.tipo = tipoPedido;
  return undefined;
}

function motivoParaNoCambiarElTipo(pieza: Bloque, campo: Campo) {
  if (esVinculo(campo))
    return `«${campo.label}» conecta con otra pieza: no le puedo cambiar el tipo.`;
  if (pieza.fields[0] !== campo) return undefined;
  return `«${campo.label}» es el dato principal de ${enMinuscula(pieza)}: no le puedo cambiar el tipo.`;
}

function pedirOpciones(cambio: CambioDeCampo, campo: Campo, args: Record<string, unknown>) {
  if (!llevaOpciones(cambio.tipo)) return undefined;
  if (llego(args.opciones)) cambio.opciones = opcionesSinRepetir(args.opciones);
  const faltanOpciones = cambio.opciones ? cambio.opciones.length < 2 : cambio.tipo !== campo.type;
  return faltanOpciones ? UNA_LISTA_NECESITA_OPCIONES : undefined;
}

const opcionesSinRepetir = (crudas: unknown) =>
  lista(crudas)
    .map(texto)
    .filter(
      (opcion, indice, todas) =>
        opcion && todas.findIndex((otra) => normalize(otra) === normalize(opcion)) === indice,
    )
    .slice(0, MAXIMO_DE_OPCIONES);

function pedirObligatorio(cambio: CambioDeCampo, campo: Campo, args: Record<string, unknown>) {
  if (!llego(args.obligatorio)) return undefined;
  if (typeof args.obligatorio !== 'boolean')
    return `No entendí si «${campo.label}» es obligatorio.`;
  if (args.obligatorio !== campo.required) cambio.obligatorio = args.obligatorio;
  return undefined;
}

function pedirLugar(
  cambio: CambioDeCampo,
  pieza: Bloque,
  campo: Campo,
  args: Record<string, unknown>,
) {
  const lugar = posicionPedida(args.posicion, pieza.fields.length);
  if (lugar === null) return 'No entendí a qué lugar mover ese dato.';
  if (lugar !== undefined && lugar - 1 !== pieza.fields.indexOf(campo)) cambio.lugar = lugar;
  return undefined;
}

/** El lugar pedido (desde 1), acotado a los que hay. `undefined` si no se pidió; `null` si no es un número. */
function posicionPedida(valor: unknown, cantidad: number): number | null | undefined {
  if (!llego(valor)) return undefined;
  const numero = typeof valor === 'number' || typeof valor === 'string' ? Number(valor) : NaN;
  if (!Number.isFinite(numero)) return null;
  return Math.min(Math.max(Math.round(numero), 1), cantidad);
}

const cambianLasOpciones = (campo: Campo, cambio: CambioDeCampo) =>
  cambio.opciones !== undefined &&
  JSON.stringify(cambio.opciones) !== JSON.stringify(campo.options ?? []);

const noCambiaNada = (campo: Campo, cambio: CambioDeCampo) =>
  cambio.etiqueta === undefined &&
  cambio.tipo === campo.type &&
  !cambianLasOpciones(campo, cambio) &&
  cambio.obligatorio === undefined &&
  cambio.lugar === undefined;

function aplicarElCambio(pieza: Bloque, campo: Campo, cambio: CambioDeCampo, hecho: Anotar) {
  const enLaPieza = enMinuscula(pieza);
  const cambiaTipo = cambio.tipo !== campo.type;
  const cambianOpciones = cambianLasOpciones(campo, cambio);
  if (cambio.etiqueta !== undefined) renombrarElCampo(campo, cambio.etiqueta, enLaPieza, hecho);
  if (cambiaTipo || cambianOpciones) {
    cambiarElTipo(pieza, campo, cambio);
    hecho(descripcionDelCambioDeTipo(campo, cambiaTipo, enLaPieza));
  }
  if (cambio.obligatorio !== undefined)
    cambiarSiEsObligatorio(campo, cambio.obligatorio, enLaPieza, hecho);
  if (cambio.lugar !== undefined) moverElCampo(pieza, campo, cambio.lugar, hecho);
}

const descripcionDelCambioDeTipo = (campo: Campo, cambiaTipo: boolean, enLaPieza: string) =>
  cambiaTipo
    ? `Cambié ${campo.label} a ${nombreDeTipo(campo.type)} en ${enLaPieza}.`
    : `Cambié las opciones de ${campo.label} en ${enLaPieza}.`;

function renombrarElCampo(campo: Campo, etiqueta: string, enLaPieza: string, hecho: Anotar) {
  const nombreAntes = campo.label;
  campo.label = etiqueta;
  hecho(`Renombré ${nombreAntes} a ${etiqueta} en ${enLaPieza}.`);
}

function cambiarElTipo(pieza: Bloque, campo: Campo, cambio: CambioDeCampo) {
  campo.type = cambio.tipo;
  if (llevaOpciones(campo.type)) campo.options = cambio.opciones ?? campo.options;
  else delete campo.options;
  if (!comportamientoDe(campo.type).admiteMinimo()) delete campo.min;
  convertirLosValoresAlTipoNuevo(pieza, campo);
}

// Los valores que ya había se pasan al tipo nuevo; lo que no sirve se borra.
function convertirLosValoresAlTipoNuevo(pieza: Bloque, campo: Campo) {
  for (const fila of pieza.rows.filter((item) => campo.key in item)) {
    const convertido = valorDeEjemplo(campo, fila[campo.key]);
    if (convertido === undefined) Reflect.deleteProperty(fila, campo.key);
    else fila[campo.key] = convertido;
  }
}

function cambiarSiEsObligatorio(
  campo: Campo,
  obligatorio: boolean,
  enLaPieza: string,
  hecho: Anotar,
) {
  campo.required = obligatorio;
  hecho(
    obligatorio
      ? `Hice obligatorio ${campo.label} en ${enLaPieza}.`
      : `${campo.label} ya no es obligatorio en ${enLaPieza}.`,
  );
}

function moverElCampo(pieza: Bloque, campo: Campo, lugar: number, hecho: Anotar) {
  pieza.fields.splice(pieza.fields.indexOf(campo), 1);
  pieza.fields.splice(lugar - 1, 0, campo);
  hecho(`Moví ${campo.label} al lugar ${lugar} en ${enMinuscula(pieza)}.`);
}
