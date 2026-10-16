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

// Pocas tablas: se ven todas como botones grandes. Son radios nativos (un grupo con su leyenda),
// así el teclado funciona como siempre: Tab entra al grupo y las flechas cambian de tabla.
export function SelectorBloque({ bloques, seleccionadoId, disabled, onElegir }: Props) {
  const nombre = useId();
  return (
    <fieldset disabled={disabled} className="m-0 min-w-0 border-0 p-0">
      <legend className="mb-1.5 p-0 text-base font-semibold">Tabla</legend>
      <div className="flex flex-wrap gap-2">
        {bloques.map((bloque) => (
          <label
            key={bloque.id}
            className={cn(
              'relative inline-flex min-h-12 cursor-pointer items-center rounded-xl border border-line bg-surface px-4 py-2 text-base font-semibold text-fg transition-colors',
              'hover:border-petrol/50 has-checked:border-petrol has-checked:bg-petrol-100 has-checked:text-petrol',
              'has-focus-visible:outline-3 has-focus-visible:outline-offset-2 has-focus-visible:outline-petrol',
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
              <CheckIcon aria-hidden size={20} weight="bold" className="mr-1.5 text-petrol" />
            )}
            {bloque.nombre}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
