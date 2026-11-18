import type { Sistema } from '../tipos';
import type { ResultadoDeAcciones } from './tipos';

/** Lo que comparte cada herramienta en un turno de Sami: el sistema, lo que va pasando y los registros que cargó. */
export type Turno = {
  sistema: Sistema;
  resultado: ResultadoDeAcciones;
  /** Los registros reales que cargó Sami en este turno: sus vínculos llegan por nombre y se resuelven al final. */
  registrosNuevos: Set<string>;
  /** Anota lo que se hizo y devuelve `undefined`, que es lo que devuelve una herramienta que se pudo aplicar. */
  hecho: (descripcion: string) => undefined;
};

/** Devuelve el motivo si no se pudo aplicar; si se aplicó, anota qué se hizo con `hecho`. */
export type Herramienta = (turno: Turno, args: Record<string, unknown>) => string | undefined;

export function nuevoTurno(sistema: Sistema): Turno {
  const resultado: ResultadoDeAcciones = {
    aplicadas: [],
    omitidas: [],
    pendientes: [],
    sinStock: [],
  };
  const hecho = (descripcion: string) => {
    resultado.aplicadas.push(descripcion);
    return undefined;
  };
  return { sistema, resultado, registrosNuevos: new Set(), hecho };
}
