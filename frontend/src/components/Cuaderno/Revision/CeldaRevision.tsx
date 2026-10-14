import type { Conexion } from '@/hooks/lotes/useConexiones';
import { cn } from '@/lib/cn';
import {
  opcionesLimpias,
  type CeldaRevision as Celda,
  type ColumnaRevision,
} from '@/lib/revisionLectura';

import { CeldaConexion } from './CeldaConexion';

type Props = {
  id: string;
  columna: ColumnaRevision;
  celda: Celda;
  numeroFila: number;
  /** Hay un error que impide guardar este dato. */
  invalida: boolean;
  /** La lectura no estuvo segura de este dato y todavía nadie lo miró. */
  dudosa: boolean;
  /** Id del aviso que explica la duda o el error (para lectores de pantalla). */
  avisoId?: string;
  /** "compacta" en celdas de tabla de escritorio (40 px); "normal" en tarjetas (48 px). */
  tamano?: 'compacta' | 'normal';
  /** A qué tabla apunta la columna; sólo en las columnas de conexión. */
  conexion?: Conexion;
  onCambiar: (texto: string) => void;
  onEnfocar: () => void;
};

const ES_FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/;

const controlBase =
  'w-full rounded-md border bg-surface px-3 text-fg caret-accent hover:border-fg disabled:cursor-not-allowed';

// Un control por tipo de dato: la persona corrige sin escribir de más. Los avisos los muestra quien
// lo usa (tarjeta o fila de la tabla), así la celda no cambia de alto.
export function CeldaRevision({
  id,
  columna,
  celda,
  numeroFila,
  invalida,
  dudosa,
  avisoId,
  tamano = 'normal',
  conexion,
  onCambiar,
  onEnfocar,
}: Props) {
  const propiedades = {
    id,
    'aria-label': `${columna.nombre}, fila ${numeroFila}`,
    'aria-invalid': invalida ? (true as const) : undefined,
    'aria-describedby': avisoId,
    className: cn(
      controlBase,
      tamano === 'compacta' ? 'min-h-10 py-1.5 text-base' : 'min-h-12 py-2.5 text-body',
      invalida
        ? 'border-2 border-bad'
        : dudosa
          ? 'border-2 border-warn bg-amber-100'
          : 'border-fg/55',
      columna.tipo === 'FECHA' && 'min-w-44',
    ),
    onFocus: onEnfocar,
  };
  const { texto } = celda;

  if (columna.tipo === 'RELACION') {
    return (
      <CeldaConexion
        id={id}
        columna={columna}
        celda={celda}
        numeroFila={numeroFila}
        conexion={conexion}
        invalida={invalida}
        dudosa={dudosa}
        avisoId={avisoId}
        tamano={tamano}
        onCambiar={onCambiar}
        onEnfocar={onEnfocar}
      />
    );
  }

  if (columna.tipo === 'OPCION' || columna.tipo === 'SI_NO') {
    const opciones =
      columna.tipo === 'SI_NO'
        ? [
            { valor: 'true', etiqueta: 'Sí' },
            { valor: 'false', etiqueta: 'No' },
          ]
        : opcionesLimpias(columna).map((opcion) => ({ valor: opcion, etiqueta: opcion }));
    const fueraDeLista = texto.trim() !== '' && !opciones.some((o) => o.valor === texto.trim());
    return (
      <select {...propiedades} value={texto.trim()} onChange={(e) => onCambiar(e.target.value)}>
        <option value="">—</option>
        {fueraDeLista && <option value={texto.trim()}>{texto.trim()} (corregir)</option>}
        {opciones.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>
            {opcion.etiqueta}
          </option>
        ))}
      </select>
    );
  }

  // Un selector de fecha no puede mostrar lo que no es una fecha: ahí se deja ver el texto leído.
  const esFecha = columna.tipo === 'FECHA' && (texto === '' || ES_FECHA_ISO.test(texto));
  return (
    <input
      {...propiedades}
      type={esFecha ? 'date' : 'text'}
      inputMode={columna.tipo === 'NUMERO' ? 'decimal' : undefined}
      placeholder={columna.tipo === 'FECHA' ? 'AAAA-MM-DD' : undefined}
      value={texto}
      onChange={(e) => onCambiar(e.target.value)}
    />
  );
}
