import { CATALOG } from './catalogo';
import {
  allBlocks,
  applyLayout,
  id,
  makeBlock,
  moveBlock,
  normalize,
  removeBlock,
  wireSystem,
} from './core';
import type { Bloque, Campo, Distribucion, Sistema, TipoBloque, TipoCampo } from './tipos';

// Las herramientas con las que Sami cambia el sistema del taller. El backend (POST /api/sami/taller) valida lo que
// pidió el modelo contra un catálogo cerrado y devuelve acciones; acá se aplican con las mismas funciones del taller.
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

const TIPOS_DE_PIEZA = new Set<string>(
  (Object.keys(CATALOG) as TipoBloque[]).filter(
    (tipo) => !['summary', 'form', 'note'].includes(tipo),
  ),
);
const TIPOS_DE_CAMPO = new Set<string>([
  'text',
  'number',
  'currency',
  'date',
  'select',
  'email',
  'tel',
  'checkbox',
]);
const DISTRIBUCIONES = new Set<string>(['mosaic', 'single', 'two', 'three']);

const texto = (valor: unknown) => (typeof valor === 'string' ? valor.trim() : '');

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
  switch (herramienta) {
    case 'agregar_pieza': {
      const tipo = texto(argumentos.tipo);
      if (!TIPOS_DE_PIEZA.has(tipo)) return `No existe una pieza «${tipo}».`;
      if (allBlocks(sistema).some((bloque) => bloque.type === tipo)) return 'Esa pieza ya está.';
      const seccion = sistema.sections[0];
      if (!seccion) return 'No hay dónde sumar la pieza.';
      seccion.blocks.push(makeBlock(tipo as TipoBloque, true));
      return hecho(`Sumé ${CATALOG[tipo as TipoBloque].name.toLowerCase()}.`);
    }
    case 'quitar_pieza': {
      const pieza = resolverPieza(sistema, argumentos.pieza);
      if (!pieza) return 'No encontré esa pieza.';
      const quitada = removeBlock(sistema, pieza.id);
      return quitada.removed
        ? hecho(`Quité ${pieza.name.toLowerCase()}.`)
        : 'No se pudo quitar esa pieza.';
    }
    case 'renombrar_pieza': {
      const pieza = resolverPieza(sistema, argumentos.pieza);
      const nombre = texto(argumentos.nombre).slice(0, 60);
      if (!pieza || !nombre) return 'No pude renombrar esa pieza.';
      pieza.name = nombre;
      return hecho(`Renombré la pieza a ${nombre}.`);
    }
    case 'renombrar_sistema': {
      const nombre = texto(argumentos.nombre).slice(0, 100);
      if (!nombre) return 'Falta el nombre.';
      sistema.name = nombre;
      return hecho(`Llamé al sistema ${nombre}.`);
    }
    case 'agregar_campo':
      return agregarCampo(sistema, argumentos, hecho);
    case 'quitar_campo': {
      const pieza = resolverPieza(sistema, argumentos.pieza);
      const etiqueta = normalize(texto(argumentos.campo));
      const campo = pieza?.fields.find((candidato) => normalize(candidato.label) === etiqueta);
      if (!pieza || !campo) return 'No encontré ese dato.';
      if (campo.required || campo.type === 'relation')
        return `«${campo.label}» no se puede quitar.`;
      pieza.fields = pieza.fields.filter((candidato) => candidato.key !== campo.key);
      for (const fila of pieza.rows) Reflect.deleteProperty(fila, campo.key);
      return hecho(`Quité ${campo.label} de ${pieza.name.toLowerCase()}.`);
    }
    case 'separar_en_seccion': {
      const pieza = resolverPieza(sistema, argumentos.pieza);
      const nombre = texto(argumentos.seccion).slice(0, 60);
      if (!pieza || !nombre) return 'No pude separar esa pieza.';
      let destino = sistema.sections.find(
        (seccion) => normalize(seccion.name) === normalize(nombre),
      );
      if (!destino) {
        destino = { id: id(), name: nombre, blocks: [] };
        sistema.sections.push(destino);
      }
      return moveBlock(sistema, pieza.id, destino.id)
        ? hecho(`Pasé ${pieza.name.toLowerCase()} a la sección ${destino.name}.`)
        : 'No se pudo mover esa pieza.';
    }
    case 'cambiar_distribucion': {
      const distribucion = texto(argumentos.layout);
      if (!DISTRIBUCIONES.has(distribucion)) return 'No conozco esa distribución.';
      applyLayout(sistema, distribucion as Distribucion);
      return hecho('Cambié la distribución.');
    }
    default:
      return `No sé hacer «${herramienta}».`;
  }
}

function agregarCampo(
  sistema: Sistema,
  argumentos: Record<string, unknown>,
  hecho: (descripcion: string) => undefined,
): string | undefined {
  const pieza = resolverPieza(sistema, argumentos.pieza);
  const etiqueta = texto(argumentos.etiqueta).slice(0, 60);
  const tipo = texto(argumentos.tipo);
  if (!pieza || !etiqueta || !TIPOS_DE_CAMPO.has(tipo)) return 'No pude sumar ese dato.';
  if (pieza.fields.some((campo) => normalize(campo.label) === normalize(etiqueta))) {
    return `«${etiqueta}» ya está.`;
  }
  const opciones = Array.isArray(argumentos.opciones)
    ? argumentos.opciones.map(texto).filter(Boolean)
    : [];
  if (tipo === 'select' && opciones.length < 2) return 'Una lista necesita al menos dos opciones.';
  const campo: Campo = {
    key: claveDeCampo(pieza, etiqueta),
    label: etiqueta,
    type: tipo as TipoCampo,
    required: false,
    ...(tipo === 'select' && { options: opciones }),
  };
  pieza.fields.push(campo);
  return hecho(`Sumé ${etiqueta} a ${pieza.name.toLowerCase()}.`);
}

// Palabras que el motor de reglas del taller reconoce de un negocio. Si no aparece ninguna, no hay con qué armar una
// base sin preguntar: ahí conviene que Sami pregunte.
const PALABRAS_DE_NEGOCIO =
  /vend|stock|producto|pedido|encargo|deposito|bodega|mercader|cliente|fiad|gasto|kiosc|almacen|despensa|minimercado|supermercado|tienda|local|ropa|negocio|comercio|insumo|alquiler|proveedor/;

/** `true` si la descripción alcanza para que el motor de reglas arme una base sin consultar a la IA. */
export const entiendeElNegocio = (descripcion: string) =>
  PALABRAS_DE_NEGOCIO.test(normalize(descripcion));

// Las órdenes que el motor de reglas del taller entiende sin equivocarse: un verbo y una pieza («sumá gastos»,
// «sacá los clientes»). Cualquier otra cosa («agregá un campo de proveedor») la interpreta la IA: las reglas buscan
// palabras sueltas y podrían confundir un dato con una pieza.
const ORDEN_SIMPLE =
  /^(suma|sumame|agrega|agregame|pone|poneme|saca|sacame|quita|quitame|separa|separame)\s+((los|las|el|la|un|una)\s+)?(gastos?|ventas?|clientes?|pedidos?|stock|inventario|entradas?|salidas?)$/;

/** `true` si el mensaje es una orden simple que el motor de reglas resuelve sin consultar a la IA. */
export const esOrdenSimple = (mensaje: string) =>
  ORDEN_SIMPLE.test(
    normalize(mensaje)
      .replace(/[¡!¿?.,;:]/g, '')
      .replace(/\s+/g, ' ')
      .trim(),
  );

/** Dos sistemas son iguales si se guardarían igual: sirve para saber si un ajuste cambió algo. */
export const mismoSistema = (a: Sistema, b: Sistema) =>
  JSON.stringify({ ...a, updatedAt: '' }) === JSON.stringify({ ...b, updatedAt: '' });
