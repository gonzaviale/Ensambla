import { CaretRightIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import type { Bloque } from '@/types/bloque';

type Props = {
  bloques: Bloque[];
  seleccionadoId?: string;
  onSeleccionar: (bloque: Bloque) => void;
};

// Cada tabla es un botón grande; la elegida se marca con color, borde y aria-current.
export function ListaBloques({ bloques, seleccionadoId, onSeleccionar }: Props) {
  return (
    <nav aria-label="Tablas del sistema">
      <ul className="m-0 flex list-none flex-col gap-2 p-0">
        {bloques.map((bloque) => {
          const elegido = bloque.id === seleccionadoId;
          return (
            <li key={bloque.id}>
              <button
                type="button"
                data-bloque-id={bloque.id}
                aria-current={elegido ? 'true' : undefined}
                onClick={() => onSeleccionar(bloque)}
                className={cn(
                  'flex min-h-14 w-full cursor-pointer items-center justify-between gap-3 rounded-lg border-2 px-4 py-3 text-left text-body font-semibold text-fg shadow-sm transition-colors hover:border-accent-700',
                  elegido ? 'border-accent-700 bg-accent-100' : 'border-line bg-surface',
                )}
              >
                <span className="min-w-0 break-words">{bloque.nombre}</span>
                <CaretRightIcon aria-hidden size={22} className="shrink-0 text-accent-800" />
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
