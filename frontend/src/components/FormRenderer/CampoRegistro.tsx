import { useId } from 'react';
import { InputField, SelectField } from '@/components/ui';
import { controlOpcion, tarjetaOpcion } from '@/components/Constructor/opcionStyles';
import { cn } from '@/lib/cn';
import type { Campo } from '@/types/proyecto';
import { SelectorRelacion } from './SelectorRelacion';

type CampoRegistroProps = {
  campo: Campo;
  valor: string;
  onChange: (valor: string) => void;
  error?: string;
  /** Nombre de la tabla con la que se conecta (sólo para RELACION). */
  nombreTabla?: string;
};

const RESPUESTAS_SI_NO = [
  { valor: 'true', texto: 'Sí' },
  { valor: 'false', texto: 'No' },
  { valor: '', texto: 'Sin responder' },
];

export function CampoRegistro({ campo, valor, onChange, error, nombreTabla }: CampoRegistroProps) {
  if (campo.tipo === 'RELACION') {
    return (
      <SelectorRelacion
        campo={campo}
        nombreTabla={nombreTabla ?? 'la tabla conectada'}
        valor={valor}
        onChange={onChange}
        error={error}
      />
    );
  }
  if (campo.tipo === 'SI_NO') {
    return <CampoSiNo campo={campo} valor={valor} onChange={onChange} error={error} />;
  }

  const props = {
    label: campo.nombre,
    name: campo.id,
    value: valor,
    required: campo.obligatorio,
    error,
  };
  if (campo.tipo === 'OPCION') {
    return (
      <SelectField {...props} onChange={(event) => onChange(event.target.value)}>
        <option value="">Elegí una opción</option>
        {campo.opciones.map((opcion) => (
          <option key={opcion} value={opcion}>
            {opcion}
          </option>
        ))}
      </SelectField>
    );
  }
  // Los números se escriben como texto con teclado decimal: acepta coma o punto (12,5 o 12.5).
  return (
    <InputField
      {...props}
      type={campo.tipo === 'FECHA' ? 'date' : 'text'}
      inputMode={campo.tipo === 'NUMERO' ? 'decimal' : undefined}
      maxLength={campo.tipo === 'TEXTO' ? 1000 : undefined}
      onChange={(event) => onChange(event.target.value)}
    />
  );
}

// Sí / No / Sin responder: tres botones grandes en lugar de una lista desplegable.
function CampoSiNo({ campo, valor, onChange, error }: CampoRegistroProps) {
  const id = useId();
  return (
    <fieldset
      className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0"
      aria-describedby={error ? `${id}-error` : undefined}
    >
      <legend className="mb-1 flex flex-wrap items-baseline gap-x-2 p-0 text-base font-semibold">
        {campo.nombre}
        {campo.obligatorio && (
          <span aria-hidden className="text-hint font-normal text-muted">
            (obligatorio)
          </span>
        )}
      </legend>
      <div className="grid grid-cols-3 gap-2">
        {RESPUESTAS_SI_NO.map((respuesta) => (
          <label
            key={respuesta.valor || 'sin'}
            className={cn(tarjetaOpcion, 'min-h-14 items-center justify-center text-center')}
          >
            <input
              type="radio"
              name={campo.id}
              value={respuesta.valor}
              checked={valor === respuesta.valor}
              onChange={() => onChange(respuesta.valor)}
              className={cn(controlOpcion, 'sr-only mt-0')}
            />
            <span className="font-semibold">{respuesta.texto}</span>
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
