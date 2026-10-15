import type { KeyboardEvent, ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { idPestana, idPanel } from './ayudas';

export type Pestana<Id extends string> = { id: Id; etiqueta: string; icono?: ReactNode };

type PestanasProps<Id extends string> = {
  pestanas: Pestana<Id>[];
  activa: Id;
  onCambiar: (id: Id) => void;
  /** Prefijo para enlazar cada pestaña con su panel (`<prefijo>-pestana-<id>` / `-panel-<id>`). */
  prefijo: string;
  label: string;
};

// Pestañas con teclado completo: flechas, Inicio y Fin mueven y activan; sólo la activa entra con Tab.
export function Pestanas<Id extends string>({
  pestanas,
  activa,
  onCambiar,
  prefijo,
  label,
}: PestanasProps<Id>) {
  function alTeclear(event: KeyboardEvent<HTMLButtonElement>, indice: number) {
    const ultimo = pestanas.length - 1;
    const destino =
      event.key === 'ArrowRight'
        ? (indice + 1) % pestanas.length
        : event.key === 'ArrowLeft'
          ? (indice - 1 + pestanas.length) % pestanas.length
          : event.key === 'Home'
            ? 0
            : event.key === 'End'
              ? ultimo
              : null;
    if (destino === null) return;
    event.preventDefault();
    const pestana = pestanas[destino];
    if (!pestana) return;
    onCambiar(pestana.id);
    document.getElementById(idPestana(prefijo, pestana.id))?.focus();
  }

  return (
    <div role="tablist" aria-label={label} className="flex gap-1 border-b border-line">
      {pestanas.map((pestana, indice) => {
        const seleccionada = pestana.id === activa;
        return (
          <button
            key={pestana.id}
            type="button"
            role="tab"
            id={idPestana(prefijo, pestana.id)}
            data-recorrido={`${prefijo}-pestana-${pestana.id}`}
            aria-selected={seleccionada}
            aria-controls={idPanel(prefijo, pestana.id)}
            tabIndex={seleccionada ? 0 : -1}
            onClick={() => onCambiar(pestana.id)}
            onKeyDown={(event) => alTeclear(event, indice)}
            className={cn(
              '-mb-px inline-flex min-h-12 flex-1 cursor-pointer items-center justify-center gap-2 border-b-4 px-4 py-2 text-base font-semibold sm:flex-none',
              seleccionada
                ? 'border-accent text-fg'
                : 'border-transparent text-soft hover:border-fg/20 hover:text-fg',
            )}
          >
            {pestana.icono && (
              <span aria-hidden className="flex shrink-0">
                {pestana.icono}
              </span>
            )}
            {pestana.etiqueta}
          </button>
        );
      })}
    </div>
  );
}
