import { useId } from 'react';
import { cn } from '@/lib/cn';
import { MESES_PAGOS_ANUAL } from '@/lib/planes/planes';
import type { PeriodoPlan } from '@/types/negocio';

type SelectorPeriodoProps = {
  value: PeriodoPlan;
  onChange: (periodo: PeriodoPlan) => void;
  /** Muestra la aclaración «pagás 10 meses, usás 12» debajo. */
  conAclaracion?: boolean;
};

const OPCIONES: { valor: PeriodoPlan; etiqueta: string }[] = [
  { valor: 'mensual', etiqueta: 'Mensual' },
  { valor: 'anual', etiqueta: 'Anual' },
];

// Dos opciones excluyentes, con radios nativos (teclado y lectores de pantalla gratis).
export function SelectorPeriodo({ value, onChange, conAclaracion = true }: SelectorPeriodoProps) {
  const nombre = useId();
  return (
    <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
      <legend className="sr-only">Período de pago</legend>
      <div className="inline-flex w-fit max-w-full rounded-md border border-fg/30 bg-surface p-1">
        {OPCIONES.map((opcion) => (
          <label
            key={opcion.valor}
            className={cn(
              'flex min-h-11 cursor-pointer items-center rounded-md px-5 text-base font-semibold has-[:focus-visible]:outline-3 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-accent-700',
              value === opcion.valor ? 'bg-accent text-fg' : 'text-soft hover:bg-fg/6',
            )}
          >
            <input
              type="radio"
              name={nombre}
              value={opcion.valor}
              checked={value === opcion.valor}
              onChange={() => onChange(opcion.valor)}
              className="sr-only"
            />
            {opcion.etiqueta}
          </label>
        ))}
      </div>
      {conAclaracion && (
        <p className="m-0 text-hint text-muted">
          Con el plan anual pagás {MESES_PAGOS_ANUAL} meses y usás 12.
        </p>
      )}
    </fieldset>
  );
}
