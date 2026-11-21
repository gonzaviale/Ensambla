import { allBlocks, camposDeVinculo } from '../core';
import type { Bloque, Campo, Fila, Seccion, Sistema, Valor } from '../tipos';
import { MAXIMO_DE_NOMBRES, MAXIMO_DE_REGISTROS, estaVacio } from './argumentos';
import { datoPrincipal, esVinculo, nombreVinculado } from './piezasDelSistema';
import type { DatosParaSami, EstadoCompacto, PiezaCompacta } from './tipos';

type QueResumir = {
  sistema: Sistema;
  destinos: ReadonlySet<string>;
};

/** El sistema como lo ve Sami: su estructura y los registros reales (también los ya guardados), sin los de ejemplo. */
export function resumirSistema(sistema: Sistema): EstadoCompacto {
  const queResumir = { sistema, destinos: destinosDeLosVinculos(sistema) };
  return {
    nombre: sistema.name,
    distribucion: sistema.layout ?? 'mosaic',
    piezas: sistema.sections.flatMap((seccion) =>
      seccion.blocks.map((bloque) => piezaCompacta(queResumir, seccion, bloque)),
    ),
  };
}

const destinosDeLosVinculos = (sistema: Sistema) =>
  new Set(
    allBlocks(sistema).flatMap((bloque) =>
      camposDeVinculo(bloque).flatMap((campo) => (campo.target ? [campo.target] : [])),
    ),
  );

function piezaCompacta(queResumir: QueResumir, seccion: Seccion, bloque: Bloque): PiezaCompacta {
  return {
    id: bloque.id,
    tipo: bloque.type,
    nombre: bloque.name,
    seccion: seccion.name,
    campos: bloque.fields.map((campo) => ({ etiqueta: campo.label, tipo: campo.type })),
    registros: registrosDeLaPieza(queResumir, bloque),
    ...(queResumir.destinos.has(bloque.id) && { nombres: nombresDe(bloque) }),
  };
}

const esNombre = (valor: Valor): valor is string | number =>
  typeof valor === 'string' || typeof valor === 'number';

/** Los nombres (el dato principal) de los registros reales de una pieza, también los guardados, sin repetir. */
function nombresDe(bloque: Bloque): string[] {
  const principal = datoPrincipal(bloque);
  if (!principal) return [];
  const nombres = bloque.rows
    .filter((fila) => !fila.__example)
    .map((fila) => fila[principal])
    .filter(esNombre)
    .map((valor) => String(valor).trim())
    .filter(Boolean);
  return [...new Set(nombres)].slice(0, MAXIMO_DE_NOMBRES);
}

/** Los registros reales de una pieza (los últimos, si son muchos), con sus datos por etiqueta. */
const registrosDeLaPieza = ({ sistema }: QueResumir, bloque: Bloque) =>
  bloque.rows
    .filter((fila) => !fila.__example)
    .slice(-MAXIMO_DE_REGISTROS)
    .map((fila) => ({ id: fila.id, datos: datosParaSami(sistema, bloque, fila) }));

function datosParaSami(sistema: Sistema, bloque: Bloque, fila: Fila): DatosParaSami {
  const datos: DatosParaSami = {};
  for (const campo of bloque.fields) {
    const valor = valorParaSami(sistema, campo, fila[campo.key]);
    if (valor !== undefined) datos[campo.label] = valor;
  }
  return datos;
}

/** Un vínculo guarda el id de la fila vinculada: a Sami le llega su nombre. Lo vacío no viaja. */
function valorParaSami(
  sistema: Sistema,
  campo: Campo,
  valor: Valor,
): string | number | boolean | undefined {
  if (estaVacio(valor)) return undefined;
  if (!esVinculo(campo)) return valor;
  const nombre = nombreVinculado(sistema, campo, valor);
  return estaVacio(nombre) ? undefined : nombre;
}
