import { useId } from 'react';
import { TIPOS_CAMPO, type TipoCampo } from '@/types/campo';
import { controlOpcion, tarjetaOpcion } from './opcionStyles';
import { TIPOS_DE_DATO } from './tiposDeDato';

type Props = {
  valor: TipoCampo;
  onCambiar: (tipo: TipoCampo) => void;
  disabled?: boolean;
  error?: string;
  legend?: string;
  /** Una columna en lugar de dos (para paneles angostos). */
  unaColumna?: boolean;
  /** Tipos que se ofrecen, en este orden (por defecto, todos). */
  tipos?: readonly TipoCampo[];
};

// Tarjetas de radio para elegir qué tipo de dato guarda una columna, con un ejemplo de cada uno.
export function TipoDeDatoCards({
  valor,
  onCambiar,
  disabled,
  error,
  legend = '¿Qué tipo de dato es?',
  unaColumna,
  tipos = TIPOS_CAMPO,
}: Props) {
  const id = useId();

  return (
    <fieldset
      className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0"
      aria-describedby={error ? `${id}-error` : undefined}
    >
      <legend className="mb-2 p-0 text-base font-semibold">{legend}</legend>
      <div className={unaColumna ? 'grid gap-2' : 'grid gap-2 sm:grid-cols-2'}>
        {tipos.map((tipo) => (
          <label key={tipo} className={tarjetaOpcion}>
            <input
              type="radio"
              name={`${id}-tipo`}
              value={tipo}
              checked={valor === tipo}
              onChange={() => onCambiar(tipo)}
              disabled={disabled}
              aria-labelledby={`${id}-${tipo}`}
              aria-describedby={`${id}-${tipo}-ayuda`}
              className={controlOpcion}
            />
            <span className="flex min-w-0 flex-col">
              <span id={`${id}-${tipo}`} className="font-semibold">
                {TIPOS_DE_DATO[tipo].nombre}
              </span>
              <span id={`${id}-${tipo}-ayuda`} className="text-hint text-soft">
                {TIPOS_DE_DATO[tipo].ayuda}
              </span>
            </span>
          </label>
        ))}
      </div>
      {error && (
        <p id={`${id}-error`} className="m-0 text-hint font-semibold text-bad">
          {error}
        </p>
      )}
    </fieldset>
  );
}
