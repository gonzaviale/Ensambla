import { useId, useState } from 'react';
import { ArrowCounterClockwiseIcon, BackspaceIcon, PlusIcon } from '@phosphor-icons/react';
import { Button, InputField, SelectField } from '@/components/ui';
import { cn } from '@/lib/cn';
import { describirFormula, piezasDeFormula } from '@/lib/campos';

type ColumnaNumerica = { id: string; nombre: string };

type Props = {
  /** La fórmula como se guarda: piezas separadas por espacios, con las columnas como {id}. */
  formula: string;
  /** Las columnas numéricas de la tabla que se pueden usar en el cálculo. */
  columnas: readonly ColumnaNumerica[];
  onCambiar: (formula: string) => void;
  disabled?: boolean;
  error?: string;
};

const OPERADORES = [
  { pieza: '+', texto: '+', nombre: 'más' },
  { pieza: '-', texto: '−', nombre: 'menos' },
  { pieza: '*', texto: '×', nombre: 'por' },
  { pieza: '/', texto: '÷', nombre: 'dividido' },
  { pieza: '(', texto: '(', nombre: 'abrir paréntesis' },
  { pieza: ')', texto: ')', nombre: 'cerrar paréntesis' },
] as const;

// Armar el cálculo con botones (columna, operador, número) en vez de escribir una expresión: no hay
// sintaxis que recordar ni ids que copiar. El resultado se muestra en palabras, como «Cantidad» × «Precio».
export function EditorFormula({ formula, columnas, onCambiar, disabled, error }: Props) {
  const id = useId();
  const [numero, setNumero] = useState('');
  const [errorNumero, setErrorNumero] = useState<string>();
  const piezas = piezasDeFormula(formula);

  const agregar = (pieza: string) => onCambiar([...piezas, pieza].join(' '));

  function agregarNumero() {
    const texto = numero.trim().replace(',', '.');
    if (!/^\d+(\.\d+)?$/.test(texto)) {
      setErrorNumero('Escribí un número, por ejemplo 2 o 0,5.');
      return;
    }
    setErrorNumero(undefined);
    setNumero('');
    agregar(texto);
  }

  return (
    <fieldset
      className="m-0 flex min-w-0 flex-col gap-3 rounded-md border border-line p-3"
      aria-describedby={cn(`${id}-ayuda`, error && `${id}-error`)}
    >
      <legend className="px-1 text-base font-semibold">Cálculo</legend>
      <p id={`${id}-ayuda`} className="m-0 text-hint text-soft">
        Elegí columnas numéricas y operaciones. El resultado se calcula solo en cada fila.
      </p>

      <p
        aria-live="polite"
        className={cn(
          'm-0 min-h-12 rounded-md bg-canvas px-3 py-2 text-body break-words',
          piezas.length === 0 && 'text-muted',
        )}
      >
        <span className="sr-only">Cálculo armado: </span>
        {piezas.length === 0 ? 'Todavía no armaste el cálculo.' : describirFormula(formula, columnas)}
      </p>

      <SelectField
        label="Agregar una columna"
        hint={
          columnas.length === 0
            ? 'Esta tabla todavía no tiene columnas numéricas para calcular.'
            : undefined
        }
        value=""
        onChange={(event) => event.target.value && agregar(`{${event.target.value}}`)}
        disabled={disabled || columnas.length === 0}
      >
        <option value="">Elegí una columna</option>
        {columnas.map((columna) => (
          <option key={columna.id} value={columna.id}>
            {columna.nombre}
          </option>
        ))}
      </SelectField>

      <div role="group" aria-label="Operaciones" className="flex flex-wrap gap-2">
        {OPERADORES.map((operador) => (
          <Button
            key={operador.pieza}
            aria-label={operador.nombre}
            disabled={disabled}
            onClick={() => agregar(operador.pieza)}
          >
            {operador.texto}
          </Button>
        ))}
      </div>

      <div className="flex items-end gap-2">
        <div className="min-w-0 flex-1">
          <InputField
            label="Agregar un número"
            value={numero}
            onChange={(event) => setNumero(event.target.value)}
            onKeyDown={(event) => {
              if (event.key !== 'Enter') return;
              event.preventDefault();
              agregarNumero();
            }}
            error={errorNumero}
            inputMode="decimal"
            autoComplete="off"
            disabled={disabled}
          />
        </div>
        <Button disabled={disabled} onClick={agregarNumero}>
          <PlusIcon aria-hidden size={20} />
          Agregar número
        </Button>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button
          disabled={disabled || piezas.length === 0}
          onClick={() => onCambiar(piezas.slice(0, -1).join(' '))}
        >
          <BackspaceIcon aria-hidden size={20} />
          Borrar lo último
        </Button>
        <Button disabled={disabled || piezas.length === 0} onClick={() => onCambiar('')}>
          <ArrowCounterClockwiseIcon aria-hidden size={20} />
          Empezar de nuevo
        </Button>
      </div>

      {error && (
        <p id={`${id}-error`} role="alert" className="m-0 text-hint font-semibold text-bad">
          {error}
        </p>
      )}
    </fieldset>
  );
}
