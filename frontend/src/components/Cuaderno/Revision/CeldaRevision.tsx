import { useId } from 'react';
import { cn } from '@/lib/cn';
import {
  errorDeCelda,
  esDudosa,
  opcionesLimpias,
  type CeldaRevision as Celda,
  type ColumnaRevision,
} from '@/lib/revisionLectura';

type Props = {
  id: string;
  columna: ColumnaRevision;
  celda: Celda;
  numeroFila: number;
  /** Error que devolvió el servidor al guardar para este dato. */
  errorServidor?: string;
  onCambiar: (texto: string) => void;
  onEnfocar: () => void;
};

const ES_FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/;

const controlBase =
  'w-full min-w-28 rounded-md border bg-canvas px-2 py-1.5 text-sm text-fg caret-accent focus-visible:border-accent focus-visible:outline-none';

// Un control por tipo de dato: la persona corrige sin escribir de más.
export function CeldaRevision({
  id,
  columna,
  celda,
  numeroFila,
  errorServidor,
  onCambiar,
  onEnfocar,
}: Props) {
  const avisoId = useId();
  const error = errorDeCelda(columna, celda) ?? errorServidor;
  const dudosa = esDudosa(celda);
  const aviso = error ?? (dudosa ? 'No estamos seguros de este dato.' : undefined);
  const propiedades = {
    id,
    'aria-label': `${columna.nombre}, fila ${numeroFila}`,
    'aria-invalid': error ? (true as const) : undefined,
    'aria-describedby': aviso ? avisoId : undefined,
    className: cn(
      controlBase,
      error ? 'border-bad' : dudosa ? 'border-warn bg-warn/10' : 'border-fg/16',
    ),
    onFocus: onEnfocar,
  };

  return (
    <div className="flex flex-col gap-1">
      <Control columna={columna} celda={celda} onCambiar={onCambiar} propiedades={propiedades} />
      {aviso && (
        <p id={avisoId} className={cn('m-0 text-caption', error ? 'text-bad' : 'text-warn')}>
          {aviso}
        </p>
      )}
    </div>
  );
}

type PropiedadesControl = {
  id: string;
  'aria-label': string;
  'aria-invalid': true | undefined;
  'aria-describedby': string | undefined;
  className: string;
  onFocus: () => void;
};

function Control({
  columna,
  celda,
  onCambiar,
  propiedades,
}: {
  columna: ColumnaRevision;
  celda: Celda;
  onCambiar: (texto: string) => void;
  propiedades: PropiedadesControl;
}) {
  const { texto } = celda;

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
