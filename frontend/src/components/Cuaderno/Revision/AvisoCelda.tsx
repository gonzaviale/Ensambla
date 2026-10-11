import { WarningCircleIcon, WarningIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { AvisoCelda as Aviso } from './avisosRevision';

type Props = {
  id: string;
  aviso: Aviso;
  /** Qué dato es, para distinguir los botones "Está bien" ("Monto, fila 2"). */
  donde: string;
  /** Mostrar el nombre de la columna antes del mensaje (en la fila de estado de la tabla). */
  prefijo?: string;
  onEstaBien: () => void;
};

// Aviso de una celda: ícono + texto (el color no es la única señal). Una duda se cierra sólo con
// una acción explícita ("Está bien") o corrigiendo el dato.
export function AvisoCelda({ id, aviso, donde, prefijo, onEstaBien }: Props) {
  const esError = aviso.tipo === 'error';
  const Icono = esError ? WarningCircleIcon : WarningIcon;

  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
      <p
        id={id}
        className={cn(
          'm-0 flex min-w-0 items-start gap-1.5 text-hint font-semibold',
          esError ? 'text-bad' : 'text-warn',
        )}
      >
        <Icono aria-hidden size={20} weight="fill" className="mt-0.5 shrink-0" />
        <span className="min-w-0">
          {prefijo && <strong className="font-bold">{prefijo}: </strong>}
          <span>{aviso.mensaje}</span>
        </span>
      </p>
      {!esError && (
        <Button size="compact" variant="primary" onClick={onEstaBien}>
          Está bien<span className="sr-only"> ({donde})</span>
        </Button>
      )}
    </div>
  );
}
