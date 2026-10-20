import { CATALOG } from './catalogo';
import {
  allBlocks,
  applyLayout,
  findBlock,
  id,
  makeBlock,
  moveBlock,
  normalize,
  removeBlock,
  wireSystem,
} from './core';
import type {
  Bloque,
  Campo,
  Distribucion,
  Fila,
  Seccion,
  Sistema,
  TipoBloque,
  TipoCampo,
  Valor,
} from './tipos';

// Las herramientas con las que Sami arma y cambia el sistema del taller. El backend (POST /api/sami/taller) valida lo
// que pidió el modelo contra el catálogo de componentes y devuelve acciones; acá se aplican con las mismas funciones
// del taller. Sami ensambla: suma componentes del catálogo (con nombre, datos y ejemplos propios), los conecta y les
// carga filas de ejemplo. Ver docs/sami-asistente.md.

export type AccionDeSami = { herramienta: string; argumentos: Record<string, unknown> };

/** El sistema resumido que se manda en cada turno: sin filas ni datos de ejemplo, para gastar pocos tokens. */
export type EstadoCompacto = {
  nombre: string;
  distribucion: string;
  piezas: {
    id: string;
    tipo: string;
    nombre: string;
    seccion: string;
    campos: { etiqueta: string; tipo: string }[];
  }[];
};

export type ResultadoDeAcciones = {
  /** Lo que se hizo, en palabras. */
  aplicadas: string[];
  /** Lo que se pidió y no se pudo hacer, y por qué. */
  omitidas: string[];
};

export function resumirSistema(sistema: Sistema): EstadoCompacto {
  return {
    nombre: sistema.name,
    distribucion: sistema.layout ?? 'mosaic',
    piezas: sistema.sections.flatMap((seccion) =>
      seccion.blocks.map((bloque) => ({
        id: bloque.id,
        tipo: bloque.type,
        nombre: bloque.name,
        seccion: seccion.name,
        campos: bloque.fields.map((campo) => ({ etiqueta: campo.label, tipo: campo.type })),
      })),
    ),
  };
}

const TIPOS_DE_PIEZA = new Set<string>(Object.keys(CATALOG));
/** La tabla a medida: es la única pieza que puede estar más de una vez (cada una con su nombre). */
const PIEZA_A_MEDIDA: TipoBloque = 'form';
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
const DISTRIBUCIONES = new Set<string>(['mosaic', 'single', 'two', 'three']);
const MAXIMO_DE_EJEMPLOS = 5;
const LARGO_DE_NOMBRE = 60;
const LARGO_DE_VALOR = 200;

const texto = (valor: unknown) => (typeof valor === 'string' ? valor.trim() : '');
const lista = (valor: unknown): unknown[] => (Array.isArray(valor) ? valor : []);
const esObjeto = (valor: unknown): valor is Record<string, unknown> =>
  typeof valor === 'object' && valor !== null && !Array.isArray(valor);

function resolverPieza(sistema: Sistema, referencia: unknown): Bloque | undefined {
  const buscada = texto(referencia);
  if (!buscada) return undefined;
  const bloques = allBlocks(sistema);
  return (
    bloques.find((bloque) => bloque.id === buscada) ??
    bloques.find((bloque) => bloque.type === buscada) ??
    bloques.find((bloque) => normalize(bloque.name) === normalize(buscada))
  );
}

const claveDeCampo = (bloque: Bloque, etiqueta: string) => {
  const base =
    normalize(etiqueta)
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'dato';
  let clave = base;
  for (let n = 2; bloque.fields.some((campo) => campo.key === clave); n++) clave = `${base}_${n}`;
  return clave;
};

const buscarCampo = (bloque: Bloque, etiqueta: string) =>
  bloque.fields.find((campo) => normalize(campo.label) === normalize(etiqueta));

/**
 * Aplica sobre el sistema (en el lugar, como el resto de `core`) las acciones que Sami pidió. Lo que no se pueda
 * hacer se omite sin romper nada y se cuenta en `omitidas`.
 */
export function aplicarAcciones(sistema: Sistema, acciones: AccionDeSami[]): ResultadoDeAcciones {
  const resultado: ResultadoDeAcciones = { aplicadas: [], omitidas: [] };
  for (const accion of acciones) {
    const motivo = aplicarUna(sistema, accion, resultado);
    if (motivo) resultado.omitidas.push(motivo);
  }
  wireSystem(sistema);
  resolverVinculosDeEjemplo(sistema);
  return resultado;
}

// Devuelve el motivo si no se pudo aplicar; si se aplicó, anota qué se hizo y devuelve `undefined`.
function aplicarUna(
  sistema: Sistema,
  { herramienta, argumentos }: AccionDeSami,
  resultado: ResultadoDeAcciones,
): string | undefined {
  const hecho = (descripcion: string) => {
    resultado.aplicadas.push(descripcion);
    return undefined;
  };
  const args = esObjeto(argumentos) ? argumentos : {};
  switch (herramienta) {
    case 'agregar_pieza':
      return agregarPieza(sistema, args, hecho, resultado);
    case 'cargar_ejemplos': {
      const pieza = resolverPieza(sistema, args.pieza);
      if (!pieza) return 'No encontré esa pieza.';
      if (!pieza.fields.length) return `«${pieza.name}» no lleva filas.`;
      const filas = filasDeEjemplo(pieza, args.filas);
      if (!filas.length) return `No pude cargar esos ejemplos en ${pieza.name.toLowerCase()}.`;
      pieza.rows = [...pieza.rows.filter((fila) => !fila.__example), ...filas];
      const cuantos = filas.length === 1 ? 'un ejemplo' : `${filas.length} ejemplos`;
      return hecho(`Cargué ${cuantos} en ${pieza.name.toLowerCase()}.`);
    }
    case 'conectar':
      return conectar(sistema, args, hecho);
    case 'quitar_pieza': {
      const pieza = resolverPieza(sistema, args.pieza);
      if (!pieza) return 'No encontré esa pieza.';
      const quitada = removeBlock(sistema, pieza.id);
      return quitada.removed
        ? hecho(`Quité ${pieza.name.toLowerCase()}.`)
        : 'No se pudo quitar esa pieza.';
    }
    case 'renombrar_pieza': {
      const pieza = resolverPieza(sistema, args.pieza);
      const nombre = texto(args.nombre).slice(0, LARGO_DE_NOMBRE);
      if (!pieza || !nombre) return 'No pude renombrar esa pieza.';
      pieza.name = nombre;
      return hecho(`Renombré la pieza a ${nombre}.`);
    }
    case 'renombrar_sistema': {
      const nombre = texto(args.nombre).slice(0, 100);
      if (!nombre) return 'Falta el nombre.';
      sistema.name = nombre;
      return hecho(`Llamé al sistema ${nombre}.`);
    }
    case 'agregar_campo':
      return agregarCampo(sistema, args, hecho);
    case 'quitar_campo': {
      const pieza = resolverPieza(sistema, args.pieza);
      const campo = pieza && buscarCampo(pieza, texto(args.campo));
      if (!pieza || !campo) return 'No encontré ese dato.';
      if (campo.required || campo.type === 'relation')
        return `«${campo.label}» no se puede quitar.`;
      pieza.fields = pieza.fields.filter((candidato) => candidato.key !== campo.key);
      for (const fila of pieza.rows) Reflect.deleteProperty(fila, campo.key);
      return hecho(`Quité ${campo.label} de ${pieza.name.toLowerCase()}.`);
    }
    case 'separar_en_seccion': {
      const pieza = resolverPieza(sistema, args.pieza);
      const nombre = texto(args.seccion).slice(0, LARGO_DE_NOMBRE);
      if (!pieza || !nombre) return 'No pude separar esa pieza.';
      const destino = seccionLlamada(sistema, nombre);
      return moveBlock(sistema, pieza.id, destino.id)
        ? hecho(`Pasé ${pieza.name.toLowerCase()} a la sección ${destino.name}.`)
        : 'No se pudo mover esa pieza.';
    }
    case 'cambiar_distribucion': {
      const distribucion = texto(args.layout);
      if (!DISTRIBUCIONES.has(distribucion)) return 'No conozco esa distribución.';
      applyLayout(sistema, distribucion as Distribucion);
      return hecho('Cambié la distribución.');
    }
    default:
      return `No sé hacer «${herramienta}».`;
  }
}

/** La sección con ese nombre (sin importar mayúsculas ni tildes); si no existe, la crea al final. */
function seccionLlamada(sistema: Sistema, nombre: string): Seccion {
  let seccion = sistema.sections.find((item) => normalize(item.name) === normalize(nombre));
  if (!seccion) {
    seccion = { id: id(), name: nombre, blocks: [] };
    sistema.sections.push(seccion);
  }
  return seccion;
}

function agregarPieza(
  sistema: Sistema,
  args: Record<string, unknown>,
  hecho: (descripcion: string) => undefined,
  resultado: ResultadoDeAcciones,
): string | undefined {
  const tipo = texto(args.tipo);
  if (!TIPOS_DE_PIEZA.has(tipo)) return `No existe una pieza «${tipo}».`;
  const tipoDePieza = tipo as TipoBloque;
  const bloques = allBlocks(sistema);
  if (tipoDePieza !== PIEZA_A_MEDIDA && bloques.some((bloque) => bloque.type === tipoDePieza)) {
    return 'Esa pieza ya está.';
  }
  const nombre = texto(args.nombre).slice(0, LARGO_DE_NOMBRE) || CATALOG[tipoDePieza].name;
  if (bloques.some((bloque) => normalize(bloque.name) === normalize(nombre))) {
    return `Ya hay una pieza «${nombre}».`;
  }
  const nombreDeSeccion = texto(args.seccion).slice(0, LARGO_DE_NOMBRE);
  const seccion = nombreDeSeccion ? seccionLlamada(sistema, nombreDeSeccion) : sistema.sections[0];
  if (!seccion) return 'No hay dónde sumar la pieza.';

  const conEjemplos = lista(args.ejemplos).length > 0;
  const pieza = makeBlock(tipoDePieza, !conEjemplos);
  pieza.name = nombre;
  // El resumen y la nota no llevan datos propios: sólo se suman con su nombre.
  if (pieza.fields.length) {
    for (const campo of lista(args.campos)) {
      const motivo = esObjeto(campo) ? sumarCampo(pieza, campo) : 'No pude sumar ese dato.';
      if (motivo) resultado.omitidas.push(motivo);
    }
    if (conEjemplos) pieza.rows = filasDeEjemplo(pieza, args.ejemplos);
  }
  seccion.blocks.push(pieza);
  return hecho(
    nombreDeSeccion
      ? `Sumé ${nombre.toLowerCase()} en la sección ${seccion.name}.`
      : `Sumé ${nombre.toLowerCase()}.`,
  );
}

/** Suma un dato a la pieza. Devuelve el motivo si no se pudo. */
function sumarCampo(pieza: Bloque, argumentos: Record<string, unknown>): string | undefined {
  const etiqueta = texto(argumentos.etiqueta).slice(0, LARGO_DE_NOMBRE);
  const tipo = texto(argumentos.tipo);
  if (!etiqueta || !TIPOS_DE_CAMPO.has(tipo)) return 'No pude sumar ese dato.';
  if (buscarCampo(pieza, etiqueta)) return `«${etiqueta}» ya está.`;
  const opciones = lista(argumentos.opciones).map(texto).filter(Boolean);
  if (tipo === 'select' && opciones.length < 2) return 'Una lista necesita al menos dos opciones.';
  const campo: Campo = {
    key: claveDeCampo(pieza, etiqueta),
    label: etiqueta,
    type: tipo as TipoCampo,
    required: false,
    ...(tipo === 'select' && { options: opciones }),
  };
  pieza.fields.push(campo);
  return undefined;
}

function agregarCampo(
  sistema: Sistema,
  args: Record<string, unknown>,
  hecho: (descripcion: string) => undefined,
): string | undefined {
  const pieza = resolverPieza(sistema, args.pieza);
  if (!pieza) return 'No pude sumar ese dato.';
  const motivo = sumarCampo(pieza, args);
  if (motivo) return motivo;
  const etiqueta = texto(args.etiqueta).slice(0, LARGO_DE_NOMBRE);
  return hecho(`Sumé ${etiqueta} a ${pieza.name.toLowerCase()}.`);
}

function conectar(
  sistema: Sistema,
  args: Record<string, unknown>,
  hecho: (descripcion: string) => undefined,
): string | undefined {
  // Primero se conecta lo que el taller ya sabe conectar (clientes con pedidos, productos con ventas), así no se
  // duplica un vínculo que la pieza trae de fábrica.
  wireSystem(sistema);
  const desde = resolverPieza(sistema, args.desde);
  const hacia = resolverPieza(sistema, args.hacia);
  if (!desde || !hacia) return 'No encontré las piezas para conectar.';
  if (desde.id === hacia.id) return 'Una pieza no se conecta consigo misma.';
  if (!desde.fields.length || !hacia.fields.length) return 'Esas piezas no se pueden conectar.';
  if (desde.fields.some((campo) => campo.type === 'relation' && campo.target === hacia.id)) {
    return `${desde.name} ya está conectada con ${hacia.name.toLowerCase()}.`;
  }
  const descripcion = `Conecté ${desde.name.toLowerCase()} con ${hacia.name.toLowerCase()}.`;
  const etiqueta = (texto(args.etiqueta) || hacia.name).slice(0, LARGO_DE_NOMBRE);
  const existente = buscarCampo(desde, etiqueta);
  if (existente) {
    // Un dato escrito con ese nombre («Cliente» en una tabla de turnos) pasa a ser el vínculo: sus valores de
    // ejemplo se buscan por nombre en la otra pieza.
    if (existente.type === 'relation' || existente === desde.fields[0]) {
      return `«${existente.label}» ya está en ${desde.name.toLowerCase()}.`;
    }
    existente.type = 'relation';
    existente.target = hacia.id;
    delete existente.options;
    return hecho(descripcion);
  }
  desde.fields.push({
    key: claveDeCampo(desde, etiqueta),
    label: etiqueta,
    type: 'relation',
    required: false,
    target: hacia.id,
  });
  return hecho(descripcion);
}

/** Las filas de ejemplo que mandó Sami (cada valor bajo la etiqueta de su dato), listas para la pieza. */
function filasDeEjemplo(pieza: Bloque, crudas: unknown): Fila[] {
  const filas: Fila[] = [];
  for (const cruda of lista(crudas)) {
    if (filas.length >= MAXIMO_DE_EJEMPLOS) break;
    if (!esObjeto(cruda)) continue;
    const fila: Fila = { id: id(), __example: true };
    let conDatos = false;
    for (const [etiqueta, valor] of Object.entries(cruda)) {
      const campo =
        buscarCampo(pieza, etiqueta) ?? pieza.fields.find((item) => item.key === etiqueta);
      const convertido = campo ? valorDeEjemplo(campo, valor) : undefined;
      if (!campo || convertido === undefined) continue;
      fila[campo.key] = convertido;
      conDatos = true;
    }
    if (conDatos) filas.push(fila);
  }
  return filas;
}

/** El valor de ejemplo convertido al tipo del dato; `undefined` si no sirve. */
function valorDeEjemplo(campo: Campo, valor: unknown): Valor {
  if (valor === null || valor === undefined || typeof valor === 'object') return undefined;
  const crudo = typeof valor === 'string' ? valor.trim() : valor;
  switch (campo.type) {
    case 'number':
    case 'currency': {
      const numero = typeof crudo === 'number' ? crudo : Number(String(crudo).replace(',', '.'));
      return crudo !== '' && Number.isFinite(numero) ? numero : undefined;
    }
    case 'checkbox':
      return crudo === true || ['si', 'true', '1', 'x'].includes(normalize(crudo));
    case 'date':
      return typeof crudo === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(crudo) ? crudo : undefined;
    case 'time': {
      const [, horas = '', minutos = ''] = /^(\d{1,2}):(\d{2})$/.exec(String(crudo)) ?? [];
      return horas && Number(horas) < 24 && Number(minutos) < 60
        ? `${horas.padStart(2, '0')}:${minutos}`
        : undefined;
    }
    case 'select':
      return campo.options?.find((opcion) => normalize(opcion) === normalize(crudo));
    default:
      return String(crudo).slice(0, LARGO_DE_VALOR) || undefined;
  }
}

// En las filas de ejemplo, un vínculo llega con el nombre del registro («Lucía Pérez»): se busca en la pieza a la que
// apunta y se guarda su id. Si no está (o el registro ya no existe), queda vacío. Las filas reales no se tocan.
function resolverVinculosDeEjemplo(sistema: Sistema) {
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields.filter((item) => item.type === 'relation')) {
      const destino = findBlock(sistema, campo.target);
      const principal = destino?.fields[0]?.key;
      for (const fila of bloque.rows.filter((item) => item.__example)) {
        const valor = fila[campo.key];
        if (valor === undefined || valor === null || valor === '') continue;
        if (destino?.rows.some((otra) => otra.id === valor)) continue;
        const encontrada =
          destino && principal
            ? destino.rows.find((otra) => normalize(otra[principal]) === normalize(valor))
            : undefined;
        fila[campo.key] = encontrada?.id ?? '';
      }
    }
  }
}

/** Dos sistemas son iguales si se guardarían igual: sirve para saber si un ajuste cambió algo. */
export const mismoSistema = (a: Sistema, b: Sistema) =>
  JSON.stringify({ ...a, updatedAt: '' }) === JSON.stringify({ ...b, updatedAt: '' });
