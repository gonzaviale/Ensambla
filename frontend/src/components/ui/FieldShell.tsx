import { useId, type ReactNode } from 'react';
import { WarningCircleIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

type FieldShellProps = {
  label: string;
  hint?: ReactNode;
  error?: string;
  required?: boolean;
  className?: string;
  children: (control: { id: string; describedBy: string | undefined }) => ReactNode;
};

// Estructura común de los campos: etiqueta de 16 px, "(obligatorio)" visible, ayuda de 15 px y
// error con ícono + texto (el color no es la única señal).
export function FieldShell({ label, hint, error, required, className, children }: FieldShellProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={cn('flex min-w-0 flex-col gap-1.5', className)}>
      <div className="flex flex-wrap items-baseline gap-x-2">
        <label htmlFor={id} className="text-base font-semibold text-fg">
          {label}
        </label>
        {required && (
          <span aria-hidden className="text-hint text-muted">
            (obligatorio)
          </span>
        )}
      </div>
      {children({ id, describedBy })}
      {hint && (
        <p id={hintId} className="m-0 text-hint text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="m-0 flex items-start gap-1.5 text-hint font-semibold text-bad">
          <WarningCircleIcon aria-hidden size={20} className="mt-0.5 shrink-0" weight="fill" />
          {error}
        </p>
      )}
    </div>
  );
}
