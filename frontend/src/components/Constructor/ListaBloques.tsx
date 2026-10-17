import { CaretRightIcon, TableIcon } from '@phosphor-icons/react';
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
        {bloques.map((bloque, indice) => {
          const elegido = bloque.id === seleccionadoId;
          return (
            <li key={bloque.id}>
              <button
                type="button"
                data-bloque-id={bloque.id}
                aria-current={elegido ? 'true' : undefined}
                onClick={() => onSeleccionar(bloque)}
                className={cn(
                  'library-table flex min-h-16 w-full cursor-pointer items-center gap-3 rounded-xl border px-3 py-3 text-left text-base font-semibold transition-colors hover:border-petrol/40',
                  elegido
                    ? 'border-petrol bg-petrol-100 text-petrol'
                    : 'border-line bg-surface text-fg',
                )}
              >
                <span className={`workspace-icon workspace-icon-${indice % 4}`}>
                  <TableIcon aria-hidden size={21} />
                </span>
                <span className="min-w-0 flex-1 break-words">{bloque.nombre}</span>
                <CaretRightIcon aria-hidden size={18} className="shrink-0 text-petrol" />
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
