import type { ChangeEvent } from 'react';
import type { Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { findBlock } from '@/lib/sistema/core';

type ControlProps = {
  sistema: Sistema | undefined;
  campo: Campo;
  valor: Valor;
  id: string;
  name?: string;
  /** Con `onCambio` el control es controlado; sin él, el formulario lee los valores al enviarse. */
  onCambio?: (valor: string | boolean) => void;
  etiqueta?: string;
};

// El control de un campo según su tipo: lo que `control()` del mockup escribía como HTML.
export function ControlDeCampo({
  sistema,
  campo,
  valor,
  id,
  name,
  onCambio,
  etiqueta,
}: ControlProps) {
  const comunes = {
    id,
    name,
    required: campo.required,
    'aria-describedby': `${id}-error`,
    'aria-label': etiqueta,
  };
  const texto = valor == null ? '' : String(valor);
  const alCambiar = (
    evento: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) => onCambio?.(evento.target.value);
  const valorDelControl = onCambio
    ? { value: texto, onChange: alCambiar }
    : { defaultValue: texto };

  if (campo.type === 'select') {
    return (
      <select {...comunes} {...valorDelControl}>
        <option value="">Elegir…</option>
        {(campo.options ?? []).map((opcion) => (
          <option key={opcion} value={opcion}>
            {opcion}
          </option>
        ))}
      </select>
    );
  }
  if (campo.type === 'relation') {
    const objetivo = findBlock(sistema, campo.target);
    return (
      <select {...comunes} {...valorDelControl}>
        <option value="">
          {objetivo?.rows.length
            ? `Elegir ${objetivo.name.toLowerCase()}…`
            : 'Primero cargá datos en el bloque vinculado'}
        </option>
        {(objetivo?.rows ?? []).map((fila) => (
          <option key={fila.id} value={fila.id}>
            {String(fila[objetivo?.fields[0]?.key ?? ''] ?? '')}
          </option>
        ))}
      </select>
    );
  }
  if (campo.type === 'textarea') {
    return <textarea {...comunes} {...valorDelControl} maxLength={2500} />;
  }
  if (campo.type === 'checkbox') {
    return (
      <label className="check-row">
        <input
          type="checkbox"
          {...comunes}
          {...(onCambio
            ? { checked: Boolean(valor), onChange: (evento) => onCambio(evento.target.checked) }
            : { defaultChecked: Boolean(valor) })}
        />
        Sí
      </label>
    );
  }
  const numerico = campo.type === 'number' || campo.type === 'currency';
  const tipo = numerico
    ? 'number'
    : ['date', 'time', 'email', 'tel'].includes(campo.type)
      ? campo.type
      : 'text';
  return (
    <input
      className="input"
      {...comunes}
      {...valorDelControl}
      type={tipo}
      step={numerico ? 'any' : undefined}
      maxLength={numerico || campo.type === 'date' ? undefined : 500}
    />
  );
}

type CampoProps = {
  sistema: Sistema | undefined;
  campo: Campo;
  valor: Valor;
  /** Prefijo para el id del control (`record`, o el id del bloque en el formulario del bloque). */
  prefijo: string;
  error?: string;
};

// Un campo de formulario completo: etiqueta, control y mensaje de error.
export function CampoDeFormulario({ sistema, campo, valor, prefijo, error }: CampoProps) {
  const id = `${prefijo}-${campo.key}`;
  const larga = campo.type === 'textarea' || campo.type === 'checkbox';
  return (
    <div
      className={`field ${larga ? 'long' : ''} ${error ? 'invalid' : ''}`.trim()}
      data-field-key={campo.key}
    >
      <label htmlFor={id}>
        {campo.label}
        {campo.required && (
          <>
            {' '}
            <span title="Obligatorio">*</span>
          </>
        )}
      </label>
      <ControlDeCampo sistema={sistema} campo={campo} valor={valor} id={id} name={campo.key} />
      <span className="field-error" id={`${id}-error`}>
        {error}
      </span>
    </div>
  );
}
