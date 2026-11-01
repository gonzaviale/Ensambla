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

export const claseDeFiltro = (campo: Campo): ClaseDeFiltro => {
  if (campo.type === 'select') return 'opcion';
  if (campo.type === 'checkbox') return 'si-no';
  if (campo.type === 'date') return 'fecha';
  if (campo.type === 'number' || campo.type === 'currency') return 'numero';
  return 'texto';
};

const vacio = (valor: Valor) => valor === '' || valor == null;

function cumpleElFiltro(sistema: Sistema, campo: Campo, valor: Valor, filtro: FiltroDeColumna) {
  switch (claseDeFiltro(campo)) {
    case 'opcion':
      return !filtro.opcion || String(valor ?? '') === filtro.opcion;
    case 'si-no':
      return !filtro.opcion || Boolean(valor) === (filtro.opcion === 'si');
    case 'fecha': {
      const fecha = vacio(valor) ? '' : String(valor);
      if (filtro.desde && (!fecha || fecha < filtro.desde)) return false;
      if (filtro.hasta && (!fecha || fecha > filtro.hasta)) return false;
      return true;
    }
    case 'numero': {
      const numero = vacio(valor) ? Number.NaN : Number(valor);
      if (filtro.desde && !(numero >= Number(filtro.desde))) return false;
      if (filtro.hasta && !(numero <= Number(filtro.hasta))) return false;
      return true;
    }
    case 'texto':
      return (
        !filtro.texto ||
        normalize(textoPlano(sistema, campo, valor)).includes(normalize(filtro.texto))
      );
  }
}

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
