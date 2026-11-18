import { wireSystem } from '../core';
import type { Sistema } from '../tipos';
import { aplicarUna } from './herramientas';
import { sincronizarStock } from './stock';
import type { AccionDeSami, ResultadoDeAcciones } from './tipos';
import { nuevoTurno, type Turno } from './turno';
import { resolverVinculosPorNombre, soltarVinculosSinDestino } from './vinculos';

// Las herramientas con las que Sami arma y cambia el sistema del taller. El backend (POST /api/sami/taller) valida lo
// que pidió el modelo contra el catálogo de componentes y devuelve acciones; acá se aplican con las mismas funciones
// del taller. Sami ensambla: suma componentes del catálogo (con nombre, datos y ejemplos propios), los conecta y les
// carga filas de ejemplo. Con una foto (de un cuaderno, una lista) también carga registros reales, que se guardan.
// Ver docs/sami-asistente.md.

export type {
  AccionDeSami,
  EstadoCompacto,
  ResultadoDeAcciones,
  SinStock,
  VinculoPendiente,
} from './tipos';
export { ajusteParaGuardar, mismoSistema, sistemaParaCrear } from './guardado';
export { resumirSistema } from './resumen';
export { sincronizarStock } from './stock';
export { pendientesVigentes, sumarPendientes } from './vinculos';

/**
 * Aplica sobre el sistema (en el lugar, como el resto de `core`) las acciones que Sami pidió. Lo que no se pueda
 * hacer se omite sin romper nada y se cuenta en `omitidas`.
 */
export function aplicarAcciones(sistema: Sistema, acciones: AccionDeSami[]): ResultadoDeAcciones {
  const turno = nuevoTurno(sistema);
  for (const accion of acciones) anotarSiSeOmitio(turno, aplicarUna(turno, accion));
  return cerrarElTurno(turno);
}

const anotarSiSeOmitio = ({ resultado }: Turno, motivo: string | undefined) => {
  if (motivo) resultado.omitidas.push(motivo);
};

function cerrarElTurno({ sistema, resultado, registrosNuevos }: Turno): ResultadoDeAcciones {
  soltarVinculosSinDestino(sistema);
  wireSystem(sistema);
  resultado.pendientes = resolverVinculosPorNombre(sistema, registrosNuevos);
  resultado.sinStock = sincronizarStock(sistema, registrosNuevos);
  return resultado;
}
