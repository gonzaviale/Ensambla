import type { ChangeEvent } from 'react';
import { findBlock } from '@/lib/sistema/core';
import type { ControlProps } from './tiposDeControl';

type Cambio = ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>;

function atributosComunes({ campo, id, name, etiqueta, invalido }: ControlProps) {
  return {
    id,
    name,
    required: campo.required,
    'aria-describedby': `${id}-error`,
    'aria-label': etiqueta,
    'aria-invalid': invalido || undefined,
  };
}

function atributosDeValor({ valor, onCambio }: ControlProps) {
  const texto = valor == null ? '' : String(valor);
  if (!onCambio) return { defaultValue: texto };
  return { value: texto, onChange: (evento: Cambio) => onCambio(evento.target.value) };
}

export function ControlDeSeleccion(props: ControlProps) {
  return (
    <select {...atributosComunes(props)} {...atributosDeValor(props)}>
      <option value="">Elegir…</option>
      {(props.campo.options ?? []).map((opcion) => (
        <option key={opcion} value={opcion}>
          {opcion}
        </option>
      ))}
    </select>
  );
}

export function ControlDeVinculo(props: ControlProps) {
  const objetivo = findBlock(props.sistema, props.campo.target);
  const hayDatos = Boolean(objetivo?.rows.length);
  return (
    <select {...atributosComunes(props)} {...atributosDeValor(props)}>
      <option value="">
        {hayDatos
          ? `Elegir ${objetivo?.name.toLowerCase()}…`
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

export function ControlDeTextoLargo(props: ControlProps) {
  return <textarea {...atributosComunes(props)} {...atributosDeValor(props)} maxLength={2500} />;
}

function atributosDeCasilla({ valor, onCambio }: ControlProps) {
  if (!onCambio) return { defaultChecked: Boolean(valor) };
  return {
    checked: Boolean(valor),
    onChange: (evento: ChangeEvent<HTMLInputElement>) => onCambio(evento.target.checked),
  };
}

export function ControlDeCasilla(props: ControlProps) {
  return (
    <label className="check-row">
      <input type="checkbox" {...atributosComunes(props)} {...atributosDeCasilla(props)} />
      Sí
    </label>
  );
}

type Entrada = { tipoHtml: string; numerica?: boolean; conLargoMaximo?: boolean };

function ControlDeEntrada({
  tipoHtml,
  numerica = false,
  conLargoMaximo = true,
  ...props
}: ControlProps & Entrada) {
  return (
    <input
      className="input"
      {...atributosComunes(props)}
      {...atributosDeValor(props)}
      type={tipoHtml}
      step={numerica ? 'any' : undefined}
      maxLength={conLargoMaximo ? 500 : undefined}
    />
  );
}

export const ControlDeTexto = (props: ControlProps) => <ControlDeEntrada {...props} tipoHtml="text" />;
export const ControlNumerico = (props: ControlProps) => (
  <ControlDeEntrada {...props} tipoHtml="number" numerica conLargoMaximo={false} />
);
export const ControlDeFecha = (props: ControlProps) => (
  <ControlDeEntrada {...props} tipoHtml="date" conLargoMaximo={false} />
);
export const ControlDeHora = (props: ControlProps) => <ControlDeEntrada {...props} tipoHtml="time" />;
export const ControlDeCorreo = (props: ControlProps) => <ControlDeEntrada {...props} tipoHtml="email" />;
export const ControlDeTelefono = (props: ControlProps) => <ControlDeEntrada {...props} tipoHtml="tel" />;
