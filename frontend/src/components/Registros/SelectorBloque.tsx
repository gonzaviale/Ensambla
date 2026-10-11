import { useId } from 'react';
import { CheckIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import type { Bloque } from '@/types/proyecto';

type Props = {
  bloques: Bloque[];
  seleccionadoId: string;
  disabled?: boolean;
  onElegir: (bloqueId: string) => void;
};

// Pocos bloques: se ven todos como botones grandes. Son radios nativos (un grupo con su leyenda),
// así el teclado funciona como siempre: Tab entra al grupo y las flechas cambian de bloque.
export function SelectorBloque({ bloques, seleccionadoId, disabled, onElegir }: Props) {
  const nombre = useId();
  return (
    <fieldset disabled={disabled} className="m-0 min-w-0 border-0 p-0">
      <legend className="mb-1.5 p-0 text-hint text-soft">Bloque</legend>
      <div className="flex flex-wrap gap-2">
        {bloques.map((bloque) => (
          <label
            key={bloque.id}
            className={cn(
              'relative inline-flex min-h-12 cursor-pointer items-center rounded-md border-2 border-line bg-surface px-4 py-2 text-base font-semibold text-fg transition-colors',
              'hover:border-accent-700 has-checked:border-accent-700 has-checked:bg-accent-100',
              'has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-accent-700',
              'has-disabled:cursor-not-allowed has-disabled:opacity-60',
            )}
          >
            <input
              type="radio"
              name={nombre}
              value={bloque.id}
              checked={bloque.id === seleccionadoId}
              onChange={() => onElegir(bloque.id)}
              className="sr-only"
            />
            {bloque.id === seleccionadoId && (
              <CheckIcon aria-hidden size={18} weight="bold" className="mr-1.5 text-accent-800" />
            )}
            {bloque.nombre}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
