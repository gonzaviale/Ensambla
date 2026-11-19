import { normalize } from './core';
import { textoPlano } from './formato';
import type { Bloque, Campo, Fila, Sistema, Valor } from './tipos';

/**
 * Lo que se filtra en una columna; cuál se usa depende del tipo de campo:
 * lista y sí/no usan `opcion`, fechas y números usan `desde` y `hasta`, y el resto, `texto`.
 */
export type FiltroDeColumna = { texto?: string; opcion?: string; desde?: string; hasta?: string };

/** Los filtros puestos, por clave de campo. */
export type Filtros = Record<string, FiltroDeColumna>;

export type ClaseDeFiltro = 'opcion' | 'si-no' | 'fecha' | 'numero' | 'texto';

const CLASE_POR_TIPO: Partial<Record<Campo['type'], ClaseDeFiltro>> = {
  select: 'opcion',
  checkbox: 'si-no',
  date: 'fecha',
  number: 'numero',
  currency: 'numero',
};

export const claseDeFiltro = (campo: Campo): ClaseDeFiltro => CLASE_POR_TIPO[campo.type] ?? 'texto';

const vacio = (valor: Valor) => valor === '' || valor == null;

type Criterio = (sistema: Sistema, campo: Campo, valor: Valor, filtro: FiltroDeColumna) => boolean;

const cumpleLaOpcion: Criterio = (_sistema, _campo, valor, filtro) =>
  !filtro.opcion || String(valor ?? '') === filtro.opcion;

const cumpleSiNo: Criterio = (_sistema, _campo, valor, filtro) =>
  !filtro.opcion || Boolean(valor) === (filtro.opcion === 'si');

const cumpleElRangoDeFechas: Criterio = (_sistema, _campo, valor, { desde, hasta }) => {
  const fecha = vacio(valor) ? '' : String(valor);
  if (desde && (!fecha || fecha < desde)) return false;
  return !(hasta && (!fecha || fecha > hasta));
};

const cumpleElRangoNumerico: Criterio = (_sistema, _campo, valor, { desde, hasta }) => {
  const numero = vacio(valor) ? Number.NaN : Number(valor);
  if (desde && !(numero >= Number(desde))) return false;
  return !(hasta && !(numero <= Number(hasta)));
};

const cumpleElTexto: Criterio = (sistema, campo, valor, filtro) =>
  !filtro.texto || normalize(textoPlano(sistema, campo, valor)).includes(normalize(filtro.texto));

const CRITERIOS: Record<ClaseDeFiltro, Criterio> = {
  opcion: cumpleLaOpcion,
  'si-no': cumpleSiNo,
  fecha: cumpleElRangoDeFechas,
  numero: cumpleElRangoNumerico,
  texto: cumpleElTexto,
};

const cumpleElFiltro: Criterio = (sistema, campo, valor, filtro) =>
  CRITERIOS[claseDeFiltro(campo)](sistema, campo, valor, filtro);

/** Si hay algo escrito en la búsqueda o en algún filtro. */
export const hayFiltros = (consulta: string, filtros: Filtros) =>
  Boolean(consulta.trim()) ||
  Object.values(filtros).some((filtro) => Object.values(filtro).some(Boolean));

/** Las filas del bloque que cumplen la búsqueda (en cualquier columna) y todos los filtros de columna. */
export function filtrarFilas(
  sistema: Sistema,
  bloque: Bloque,
  consulta: string,
  filtros: Filtros,
): Fila[] {
  const buscado = normalize(consulta);
  return bloque.rows.filter((fila) => {
    if (buscado) {
      const texto = normalize(
        bloque.fields.map((campo) => textoPlano(sistema, campo, fila[campo.key])).join(' '),
      );
      if (!texto.includes(buscado)) return false;
    }
    return bloque.fields.every((campo) =>
      cumpleElFiltro(sistema, campo, fila[campo.key], filtros[campo.key] ?? {}),
    );
  });
}
