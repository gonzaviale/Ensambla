import { useId } from 'react';
import { PlusIcon, XIcon } from '@phosphor-icons/react';
import { Button, InputField } from '@/components/ui';
import { cn } from '@/lib/cn';

type Props = {
  opciones: string[];
  onCambiar: (opciones: string[]) => void;
  disabled?: boolean;
  error?: string;
};

// Una opción por input (no un cuadro de texto con renglones): quitar y agregar son botones claros.
export function OpcionesEditor({ opciones, onCambiar, disabled, error }: Props) {
  const id = useId();

  return (
    <fieldset
      className="m-0 flex min-w-0 flex-col gap-3 rounded-md border border-line p-3"
      aria-describedby={cn(`${id}-ayuda`, error && `${id}-error`)}
    >
      <legend className="px-1 text-base font-semibold">Opciones de la lista</legend>
      <p id={`${id}-ayuda`} className="m-0 text-hint text-soft">
        Escribí una opción en cada renglón. Los renglones vacíos no se guardan.
      </p>
      {opciones.map((opcion, indice) => (
        <div key={indice} className="flex items-end gap-2">
          <div className="min-w-0 flex-1">
            <InputField
              label={`Opción ${indice + 1}`}
              value={opcion}
              onChange={(event) =>
                onCambiar(opciones.map((actual, i) => (i === indice ? event.target.value : actual)))
              }
              aria-invalid={error ? true : undefined}
              disabled={disabled}
              autoComplete="off"
            />
          </div>
          <Button
            variant="ghost"
            iconOnly
            aria-label={`Quitar opción ${indice + 1}`}
            disabled={disabled || opciones.length === 1}
            onClick={() => onCambiar(opciones.filter((_, i) => i !== indice))}
          >
            <XIcon aria-hidden size={22} />
          </Button>
        </div>
      ))}
      <Button
        className="self-start"
        disabled={disabled}
        onClick={() => onCambiar([...opciones, ''])}
      >
        <PlusIcon aria-hidden size={20} />
        Agregar otra opción
      </Button>
      {error && (
        <p id={`${id}-error`} className="m-0 text-hint font-semibold text-bad">
          {error}
        </p>
      )}
    </fieldset>
  );
}
