import { useCallback, useEffect, useReducer } from 'react';
import {
  borrarBorrador,
  guardarBorrador,
  iniciarBorrador,
  leerBorradorGuardado,
  reducirBorrador,
  type AccionRevision,
  type BorradorGuardado,
  type Destino,
} from '@/lib/revisionLectura';
import type { PropuestaLectura } from '@/types/lectura';

type Accion = AccionRevision | { tipo: 'cambiarDestino'; destino: Destino };

function reducir(estado: BorradorGuardado, accion: Accion): BorradorGuardado {
  if (accion.tipo === 'cambiarDestino') return { ...estado, destino: accion.destino };
  return { ...estado, borrador: reducirBorrador(estado.borrador, accion) };
}

// Si la IA reconoció una tabla que ya existe se ofrece sumar las filas ahí; si no, un proyecto nuevo.
function iniciar(loteId: string, propuesta: PropuestaLectura): BorradorGuardado {
  return (
    leerBorradorGuardado(loteId) ?? {
      borrador: iniciarBorrador(propuesta),
      destino: propuesta.bloqueExistenteId
        ? { modo: 'bloque', bloqueId: propuesta.bloqueExistenteId }
        : { modo: 'nuevo', nombreProyecto: propuesta.nombre?.trim() ?? '' },
    }
  );
}

// El borrador de la revisión: se corrige en memoria y se conserva si se recarga la página.
export function useRevisionLectura(loteId: string, propuesta: PropuestaLectura) {
  const [estado, dispatch] = useReducer(reducir, undefined, () => iniciar(loteId, propuesta));

  useEffect(() => guardarBorrador(loteId, estado), [loteId, estado]);

  const cambiarDestino = useCallback(
    (destino: Destino) => dispatch({ tipo: 'cambiarDestino', destino }),
    [],
  );
  const descartar = useCallback(() => borrarBorrador(loteId), [loteId]);

  return { borrador: estado.borrador, destino: estado.destino, dispatch, cambiarDestino, descartar };
}
