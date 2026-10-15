import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type OpcionRadioProps = {
  name: string;
  value: string;
  checked: boolean;
  onChange: (value: string) => void;
  titulo: ReactNode;
  detalle?: ReactNode;
  icono?: ReactNode;
  className?: string;
};

// Opción de un grupo de radios, grande y con borde: la elegida se ve por el contorno, el fondo y el
// círculo marcado (no sólo por el color). Siempre va dentro de un <fieldset> con <legend>.
export function OpcionRadio({
  name,
  value,
  checked,
  onChange,
  titulo,
  detalle,
  icono,
  className,
}: OpcionRadioProps) {
  return (
    <label
      className={cn(
        'flex min-h-14 cursor-pointer items-start gap-3 rounded-lg border-2 bg-surface p-3.5 hover:border-fg/60 has-[:focus-visible]:outline-3 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-accent-700',
        checked ? 'border-accent-700 bg-accent-100' : 'border-fg/25',
        className,
      )}
    >
      <input
        type="radio"
        name={name}
        value={value}
        checked={checked}
        onChange={() => onChange(value)}
        className="mt-1 size-5 shrink-0 cursor-pointer accent-accent-700"
      />
      {icono && (
        <span aria-hidden className="mt-0.5 flex shrink-0 text-fg">
          {icono}
        </span>
      )}
      <span className="flex min-w-0 flex-col gap-0.5">
        <span className="text-body font-semibold text-fg">{titulo}</span>
        {detalle && <span className="text-hint text-soft">{detalle}</span>}
      </span>
    </label>
  );
}
