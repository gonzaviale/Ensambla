import { CheckIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

type StepperProps = { pasos: string[]; actual: number; label?: string };

// Pasos de un flujo corto ("1 Revisá · 2 ¿Dónde guardamos? · 3 Guardar"). `actual` es 0-based.
export function Stepper({ pasos, actual, label = 'Pasos' }: StepperProps) {
  return (
    <ol aria-label={label} className="m-0 flex list-none flex-wrap gap-x-5 gap-y-2 p-0">
      {pasos.map((paso, i) => {
        const hecho = i < actual;
        const enCurso = i === actual;
        return (
          <li
            key={paso}
            aria-current={enCurso ? 'step' : undefined}
            className={cn(
              'flex items-center gap-2 text-hint',
              enCurso ? 'font-bold text-fg' : hecho ? 'text-soft' : 'text-muted',
            )}
          >
            <span
              aria-hidden
              className={cn(
                'flex size-7 shrink-0 items-center justify-center rounded-full text-caption font-bold',
                enCurso
                  ? 'bg-accent text-fg'
                  : hecho
                    ? 'bg-ok text-white'
                    : 'border border-fg/30 bg-surface text-muted',
              )}
            >
              {hecho ? <CheckIcon size={16} weight="bold" /> : i + 1}
            </span>
            <span>
              {paso}
              {hecho && <span className="sr-only"> (listo)</span>}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
