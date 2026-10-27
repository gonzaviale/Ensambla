import { CATALOG, FIELD_TYPES } from './catalogo';
import {
  allBlocks,
  applyLayout,
  clone,
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
// carga filas de ejemplo. Con una foto (de un cuaderno, una lista) también carga registros reales, que se guardan.
// Ver docs/sami-asistente.md.

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
const MAXIMO_DE_REGISTROS = 30;
const MAXIMO_DE_OPCIONES = 6;
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
  // Los registros reales que cargó Sami en este turno: sus vínculos llegan por nombre y se resuelven al final.
  const registrosNuevos = new Set<string>();
  for (const accion of acciones) {
    const motivo = aplicarUna(sistema, accion, resultado, registrosNuevos);
    if (motivo) resultado.omitidas.push(motivo);
  }
  soltarVinculosSinDestino(sistema);
  wireSystem(sistema);
  resolverVinculosPorNombre(sistema, registrosNuevos);
  return resultado;
}

/**
 * `removeBlock` quita la pieza pero no los vínculos que apuntaban a ella. Un vínculo sin destino vuelve a ser como en
 * el catálogo (el «Cliente» de una venta, texto) y pierde los valores, que eran ids de filas que ya no están; si
 * después hay con qué, `wireSystem` lo vuelve a conectar.
 */
function soltarVinculosSinDestino(sistema: Sistema) {
  const ids = new Set(allBlocks(sistema).map((bloque) => bloque.id));
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields) {
      if (!campo.target || ids.has(campo.target)) continue;
      delete campo.target;
      const base = CATALOG[bloque.type].fields.find((item) => item.key === campo.key);
      campo.type = base?.type === 'relation' ? 'relation' : 'text';
      for (const fila of bloque.rows) Reflect.deleteProperty(fila, campo.key);
    }
  }
}

// Devuelve el motivo si no se pudo aplicar; si se aplicó, anota qué se hizo y devuelve `undefined`.
function aplicarUna(
  sistema: Sistema,
  { herramienta, argumentos }: AccionDeSami,
  resultado: ResultadoDeAcciones,
  registrosNuevos: Set<string>,
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
    case 'cargar_registros': {
      // Lo que Sami leyó de una foto: se suma a los registros que haya y se guarda con el sistema. Las filas de
      // ejemplo de esa pieza se reemplazan: no se mezclan con los datos reales.
      // Antes se conecta lo que el taller ya sabe conectar, así un «Cliente» por nombre se busca en los clientes.
      wireSystem(sistema);
      const pieza = resolverPieza(sistema, args.pieza);
      if (!pieza) return 'No encontré esa pieza.';
      if (!pieza.fields.length) return `«${pieza.name}» no lleva filas.`;
      const filas = filasDeSami(pieza, args.filas, MAXIMO_DE_REGISTROS, false);
      if (!filas.length) return `No pude cargar esos registros en ${pieza.name.toLowerCase()}.`;
      pieza.rows = [...pieza.rows.filter((fila) => !fila.__example), ...filas];
      for (const fila of filas) registrosNuevos.add(fila.id);
      const cuantos = filas.length === 1 ? 'un registro' : `${filas.length} registros`;
      return hecho(`Cargué ${cuantos} en ${pieza.name.toLowerCase()}.`);
    }
    case 'borrar_registros':
      return borrarRegistros(sistema, args, hecho);
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
      return hecho(`Llamé al cuaderno ${nombre}.`);
    }
    case 'agregar_campo':
      return agregarCampo(sistema, args, hecho);
    case 'editar_campo':
      return editarCampo(sistema, args, hecho);
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

/** El dato de la pieza que Sami nombra por su etiqueta (o por su clave). */
const campoNombrado = (pieza: Bloque, etiqueta: string) =>
  buscarCampo(pieza, etiqueta) ?? pieza.fields.find((campo) => campo.key === etiqueta);

/**
 * Si el valor guardado en la fila es el que Sami nombra. Un vínculo guarda el id de la fila vinculada: se compara
 * con el nombre de esa fila. El valor pedido se pasa antes al tipo del dato (`0,5`, «sí», una fecha de cuaderno).
 */
function coincide(sistema: Sistema, campo: Campo, guardado: Valor, pedido: unknown): boolean {
  if (guardado === undefined || guardado === null || guardado === '') return false;
  if (campo.type === 'relation') {
    const destino = findBlock(sistema, campo.target);
    const principal = destino?.fields[0]?.key;
    const vinculada = destino?.rows.find((fila) => fila.id === guardado);
    const nombre = principal && vinculada ? vinculada[principal] : undefined;
    return nombre !== undefined && normalize(nombre) === normalize(pedido);
  }
  const convertido = valorDeEjemplo(campo, pedido);
  return convertido !== undefined && normalize(guardado) === normalize(convertido);
}

/**
 * Borra de una pieza los registros que coinciden con lo que Sami indica en `donde`: un criterio (`{Cliente: 'Ana'}`,
 * coinciden todos sus datos) o una lista de criterios (basta con que coincida uno). Nunca borra de más: un criterio
 * vacío, o con un dato que la pieza no tiene, no vale y no se usa; si ninguno vale, no se borra nada. No toca otras
 * piezas, salvo para vaciar los vínculos que apuntaban a lo borrado.
 */
function borrarRegistros(
  sistema: Sistema,
  args: Record<string, unknown>,
  hecho: (descripcion: string) => undefined,
): string | undefined {
  const pieza = resolverPieza(sistema, args.pieza);
  if (!pieza) return 'No encontré esa pieza.';
  const pedidos = esObjeto(args.donde) ? [args.donde] : lista(args.donde);
  const criterios = pedidos.filter(esObjeto).flatMap((pedido) => {
    const datos = Object.entries(pedido).map(
      ([etiqueta, valor]) => [campoNombrado(pieza, etiqueta), valor] as const,
    );
    const validos = datos.flatMap(([campo, valor]) => (campo ? [{ campo, valor }] : []));
    return datos.length && validos.length === datos.length ? [validos] : [];
  });
  if (!criterios.length)
    return `Para borrar registros de ${pieza.name.toLowerCase()} necesito saber cuáles.`;
  const sobran = pieza.rows.filter((fila) =>
    criterios.some((criterio) =>
      criterio.every(({ campo, valor }) => coincide(sistema, campo, fila[campo.key], valor)),
    ),
  );
  if (!sobran.length) return `No encontré registros de ${pieza.name.toLowerCase()} que coincidan.`;
  const ids = new Set(sobran.map((fila) => fila.id));
  pieza.rows = pieza.rows.filter((fila) => !ids.has(fila.id));
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields.filter(
      (item) => item.type === 'relation' && item.target === pieza.id,
    )) {
      for (const fila of bloque.rows) {
        if (ids.has(String(fila[campo.key]))) fila[campo.key] = '';
      }
    }
  }
  const cuantos = sobran.length === 1 ? 'un registro' : `${sobran.length} registros`;
  return hecho(`Borré ${cuantos} de ${pieza.name.toLowerCase()}.`);
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

/**
 * Cambia un dato que ya existe: el nombre (la clave no cambia, así las filas no se pierden), el tipo, las opciones,
 * si es obligatorio y su lugar en la pieza. Es todo o nada: si algo de lo pedido no vale, no se cambia nada (salvo el
 * tipo de un vínculo o del primer dato, que se ignora). Si no cambia nada, se omite.
 */
function editarCampo(
  sistema: Sistema,
  args: Record<string, unknown>,
  hecho: (descripcion: string) => undefined,
): string | undefined {
  const pieza = resolverPieza(sistema, args.pieza);
  const campo = pieza && buscarCampo(pieza, texto(args.campo));
  if (!pieza || !campo) return 'No encontré ese dato.';
  const enLaPieza = pieza.name.toLowerCase();
  const indice = pieza.fields.indexOf(campo);

  const etiqueta = texto(args.etiqueta).slice(0, LARGO_DE_NOMBRE);
  const renombra = Boolean(etiqueta) && etiqueta !== campo.label;
  if (
    renombra &&
    pieza.fields.some((otro) => otro !== campo && normalize(otro.label) === normalize(etiqueta))
  ) {
    return `Ya hay un dato «${etiqueta}» en ${enLaPieza}.`;
  }

  // El tipo de un vínculo o del primer dato (el que nombra cada fila) no cambia: se ignora, como en el backend.
  const tipoPedido = texto(args.tipo);
  let tipoIgnorado: string | undefined;
  let tipo = campo.type;
  if (tipoPedido && campo.type === 'relation') {
    tipoIgnorado = `«${campo.label}» conecta con otra pieza: no le puedo cambiar el tipo.`;
  } else if (tipoPedido && indice === 0) {
    tipoIgnorado = `«${campo.label}» es el dato principal de ${enLaPieza}: no le puedo cambiar el tipo.`;
  } else if (tipoPedido) {
    if (!TIPOS_DE_CAMPO.has(tipoPedido)) return `No conozco el tipo «${tipoPedido}».`;
    tipo = tipoPedido as TipoCampo;
  }
  const cambiaTipo = tipo !== campo.type;

  let opciones: string[] | undefined;
  if (tipo === 'select' && llego(args.opciones)) {
    opciones = lista(args.opciones)
      .map(texto)
      .filter(
        (opcion, indice, todas) =>
          opcion && todas.findIndex((otra) => normalize(otra) === normalize(opcion)) === indice,
      )
      .slice(0, MAXIMO_DE_OPCIONES);
    if (opciones.length < 2) return 'Una lista necesita al menos dos opciones.';
  }
  if (cambiaTipo && tipo === 'select' && !opciones) {
    return 'Una lista necesita al menos dos opciones.';
  }
  const cambianOpciones =
    opciones !== undefined && JSON.stringify(opciones) !== JSON.stringify(campo.options ?? []);

  const obligatorio = llego(args.obligatorio) ? args.obligatorio : undefined;
  if (obligatorio !== undefined && typeof obligatorio !== 'boolean') {
    return `No entendí si «${campo.label}» es obligatorio.`;
  }
  const cambiaObligatorio = obligatorio !== undefined && obligatorio !== campo.required;

  const lugar = posicionPedida(args.posicion, pieza.fields.length);
  if (lugar === null) return 'No entendí a qué lugar mover ese dato.';
  const mueve = lugar !== undefined && lugar - 1 !== indice;

  if (!renombra && !cambiaTipo && !cambianOpciones && !cambiaObligatorio && !mueve) {
    return tipoIgnorado ?? `«${campo.label}» ya estaba así.`;
  }

  const nombreAntes = campo.label;
  if (renombra) {
    campo.label = etiqueta;
    hecho(`Renombré ${nombreAntes} a ${etiqueta} en ${enLaPieza}.`);
  }
  if (cambiaTipo || cambianOpciones) {
    campo.type = tipo;
    if (tipo === 'select') campo.options = opciones ?? campo.options;
    else delete campo.options;
    if (tipo !== 'number' && tipo !== 'currency') delete campo.min;
    // Los valores que ya había se pasan al tipo nuevo; lo que no sirve se borra.
    for (const fila of pieza.rows) {
      if (!(campo.key in fila)) continue;
      const convertido = valorDeEjemplo(campo, fila[campo.key]);
      if (convertido === undefined) Reflect.deleteProperty(fila, campo.key);
      else fila[campo.key] = convertido;
    }
    hecho(
      cambiaTipo
        ? `Cambié ${campo.label} a ${nombreDeTipo(tipo)} en ${enLaPieza}.`
        : `Cambié las opciones de ${campo.label} en ${enLaPieza}.`,
    );
  }
  if (cambiaObligatorio) {
    campo.required = Boolean(obligatorio);
    hecho(
      campo.required
        ? `Hice obligatorio ${campo.label} en ${enLaPieza}.`
        : `${campo.label} ya no es obligatorio en ${enLaPieza}.`,
    );
  }
  if (mueve && lugar !== undefined) {
    pieza.fields.splice(indice, 1);
    pieza.fields.splice(lugar - 1, 0, campo);
    hecho(`Moví ${campo.label} al lugar ${lugar} en ${enLaPieza}.`);
  }
  return undefined;
}

/** Si llegó un argumento optativo: un nulo, un texto en blanco o una lista vacía no cuentan (como en el backend). */
const llego = (valor: unknown) =>
  valor !== undefined &&
  valor !== null &&
  !(typeof valor === 'string' && !valor.trim()) &&
  !(Array.isArray(valor) && !valor.length);

const nombreDeTipo = (tipo: TipoCampo) =>
  tipo === 'select' ? 'lista de opciones' : FIELD_TYPES[tipo].toLowerCase();

/** El lugar pedido (desde 1), acotado a los que hay. `undefined` si no se pidió; `null` si no es un número. */
function posicionPedida(valor: unknown, cantidad: number): number | null | undefined {
  if (!llego(valor)) return undefined;
  const numero = typeof valor === 'number' || typeof valor === 'string' ? Number(valor) : NaN;
  if (!Number.isFinite(numero)) return null;
  return Math.min(Math.max(Math.round(numero), 1), cantidad);
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

const filasDeEjemplo = (pieza: Bloque, crudas: unknown) =>
  filasDeSami(pieza, crudas, MAXIMO_DE_EJEMPLOS, true);

/**
 * Las filas que mandó Sami (cada valor bajo la etiqueta de su dato), listas para la pieza: de ejemplo o registros
 * reales. Una fila sin el dato principal (el primero: Producto, Nombre, Pedido…) se descarta: en la tabla se vería
 * vacía.
 */
function filasDeSami(pieza: Bloque, crudas: unknown, maximo: number, deEjemplo: boolean): Fila[] {
  const principal = pieza.fields[0]?.key;
  const filas: Fila[] = [];
  for (const cruda of lista(crudas)) {
    if (filas.length >= maximo) break;
    if (!esObjeto(cruda)) continue;
    const fila: Fila = deEjemplo ? { id: id(), __example: true } : { id: id() };
    for (const [etiqueta, valor] of Object.entries(cruda)) {
      const campo =
        buscarCampo(pieza, etiqueta) ?? pieza.fields.find((item) => item.key === etiqueta);
      const convertido = campo ? valorDeEjemplo(campo, valor) : undefined;
      if (!campo || convertido === undefined) continue;
      fila[campo.key] = convertido;
    }
    if (principal && fila[principal] !== undefined) filas.push(fila);
  }
  return filas;
}

/** El valor de ejemplo convertido al tipo del dato; `undefined` si no sirve. */
/**
 * Una fecha como se anota en un cuaderno, en AAAA-MM-DD: acepta AAAA-MM-DD, DD/MM/AAAA, DD/MM/AA y DD/MM (sin año,
 * el del año en curso). Una fecha que no existe (31/02) no sirve.
 */
function fechaDeCuaderno(texto: string): string | undefined {
  if (/^\d{4}-\d{2}-\d{2}$/.test(texto)) return texto;
  const partes = /^(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2}|\d{4}))?$/.exec(texto);
  if (!partes) return undefined;
  const [, dia = '', mes = '', anio] = partes;
  const completo = !anio
    ? new Date().getFullYear()
    : anio.length === 2
      ? 2000 + Number(anio)
      : Number(anio);
  const fecha = new Date(Date.UTC(completo, Number(mes) - 1, Number(dia)));
  if (fecha.getUTCMonth() !== Number(mes) - 1 || fecha.getUTCDate() !== Number(dia))
    return undefined;
  return fecha.toISOString().slice(0, 10);
}

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
      return typeof crudo === 'string' ? fechaDeCuaderno(crudo) : undefined;
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

const ES_ID_DE_FILA = /^e[0-9a-f]{8}-/;

// En las filas de ejemplo y en los registros que Sami acaba de cargar, un vínculo llega con el nombre del registro
// («Lucía Pérez»): se busca en la pieza a la que apunta y se guarda su id. Si esa pieza no lo tiene, se suma ahí, así
// el vínculo se ve aunque la pieza esté vacía (pasa al ajustar un sistema ya creado, que no tiene ejemplos): como
// ejemplo si la fila es de ejemplo y como registro real si es real. Un registro real sólo se vincula con otro real (los
// ejemplos no se guardan). Si lo que hay es el id de un registro que ya no existe, queda vacío. Las demás filas reales
// no se tocan.
function resolverVinculosPorNombre(sistema: Sistema, registrosNuevos: Set<string>) {
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields.filter((item) => item.type === 'relation')) {
      const destino = findBlock(sistema, campo.target);
      const principal = destino?.fields[0]?.key;
      for (const fila of bloque.rows) {
        const deEjemplo = Boolean(fila.__example);
        if (!deEjemplo && !registrosNuevos.has(fila.id)) continue;
        const valor = fila[campo.key];
        if (valor === undefined || valor === null || valor === '') continue;
        const candidatas = destino?.rows.filter((otra) => deEjemplo || !otra.__example) ?? [];
        if (candidatas.some((otra) => otra.id === valor)) continue;
        if (!destino || !principal) {
          fila[campo.key] = '';
          continue;
        }
        const nombre = String(valor).slice(0, LARGO_DE_VALOR);
        let encontrada = candidatas.find(
          (otra) => normalize(otra[principal]) === normalize(nombre),
        );
        if (!encontrada && !ES_ID_DE_FILA.test(nombre)) {
          encontrada = deEjemplo
            ? { id: id(), [principal]: nombre, __example: true }
            : { id: id(), [principal]: nombre };
          destino.rows.push(encontrada);
        }
        fila[campo.key] = encontrada?.id ?? '';
      }
    }
  }
}

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
  const previas = new Set(
    allBlocks(original).flatMap((bloque) => bloque.rows.map((fila) => fila.id)),
  );
  descartarEjemplos(sistema, previas);
  return sistema;
}

/**
 * El sistema que se crea con la propuesta del taller. Las filas de ejemplo, que estaban para imaginarlo, no pasan; los
 * registros reales (los que Sami cargó de una foto), sí. Un vínculo que apuntaba a un ejemplo queda vacío.
 */
export function sistemaParaCrear(borrador: Sistema): Sistema {
  const sistema = clone(borrador);
  sistema.name = sistema.name.trim() || 'Mi cuaderno';
  descartarEjemplos(sistema, new Set());
  return sistema;
}

/** Quita (en el lugar) las filas de ejemplo que no están en `conservar` y vacía los vínculos que apuntaban a ellas. */
function descartarEjemplos(sistema: Sistema, conservar: Set<string>) {
  const quitadas = new Set<string>();
  for (const bloque of allBlocks(sistema)) {
    for (const fila of bloque.rows) {
      if (fila.__example && !conservar.has(fila.id)) quitadas.add(fila.id);
    }
    bloque.rows = bloque.rows.filter((fila) => !quitadas.has(fila.id));
  }
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields.filter((item) => item.type === 'relation')) {
      for (const fila of bloque.rows) {
        if (quitadas.has(String(fila[campo.key]))) fila[campo.key] = '';
      }
    }
  }
}
