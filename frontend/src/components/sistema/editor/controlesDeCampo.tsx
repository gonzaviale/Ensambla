import type { ChangeEvent, ReactElement } from 'react';
import { findBlock } from '@/lib/sistema/core';
import type { Bloque, Campo, Sistema, TipoCampo, Valor } from '@/lib/sistema/tipos';

type CambioDeControl = (valor: string | boolean) => void;

type PropiedadesComunes = {
  id: string;
  name?: string;
  required: boolean;
  'aria-describedby': string;
  'aria-label'?: string;
  'aria-invalid'?: true;
};

type ValorDelControl =
  | {
      value: string;
      onChange: (
        evento: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
      ) => void;
    }
  | { defaultValue: string };

export type PedidoDeControl = {
  sistema: Sistema | undefined;
  campo: Campo;
  valor: Valor;
  onCambio?: CambioDeControl;
  comunes: PropiedadesComunes;
  valorDelControl: ValorDelControl;
};

type ControlDeUnTipo = (pedido: PedidoDeControl) => ReactElement;

type FormaDeEntrada = { tipo: string; paso?: string; largoMaximo?: number };

export function propiedadesComunes(
  campo: Campo,
  id: string,
  name: string | undefined,
  etiqueta: string | undefined,
  invalido: boolean,
): PropiedadesComunes {
  return {
    id,
    name,
    required: campo.required,
    'aria-describedby': `${id}-error`,
    'aria-label': etiqueta,
    'aria-invalid': invalido || undefined,
  };
}

/** Con `onCambio` el control es controlado; sin él, el formulario lee los valores al enviarse. */
export function valorDelControl(valor: Valor, onCambio?: CambioDeControl): ValorDelControl {
  const texto = valor == null ? '' : String(valor);
  if (!onCambio) return { defaultValue: texto };
  return { value: texto, onChange: (evento) => onCambio(evento.target.value) };
}

function entrada({ tipo, paso, largoMaximo }: FormaDeEntrada): ControlDeUnTipo {
  return ({ comunes, valorDelControl: delValor }) => (
    <input
      className="input"
      {...comunes}
      {...delValor}
      type={tipo}
      step={paso}
      maxLength={largoMaximo}
    />
  );
}

function controlDeLista({ campo, comunes, valorDelControl: delValor }: PedidoDeControl) {
  return (
    <select {...comunes} {...delValor}>
      <option value="">Elegir…</option>
      {(campo.options ?? []).map((opcion) => (
        <option key={opcion} value={opcion}>
          {opcion}
        </option>
      ))}
    </select>
  );
}

function textoDelVinculoVacio(objetivo: Bloque | undefined) {
  return objetivo?.rows.length
    ? `Elegir ${objetivo.name.toLowerCase()}…`
    : 'Primero cargá datos en el bloque vinculado';
}

function controlDeVinculo({ sistema, campo, comunes, valorDelControl: delValor }: PedidoDeControl) {
  const objetivo = findBlock(sistema, campo.target);
  return (
    <select {...comunes} {...delValor}>
      <option value="">{textoDelVinculoVacio(objetivo)}</option>
      {(objetivo?.rows ?? []).map((fila) => (
        <option key={fila.id} value={fila.id}>
          {String(fila[objetivo?.fields[0]?.key ?? ''] ?? '')}
        </option>
      ))}
    </select>
  );
}

function controlDeTextoLargo({ comunes, valorDelControl: delValor }: PedidoDeControl) {
  return <textarea {...comunes} {...delValor} maxLength={2500} />;
}

function controlDeCasilla({ valor, onCambio, comunes }: PedidoDeControl) {
  const estado = onCambio
    ? {
        checked: Boolean(valor),
        onChange: (evento: ChangeEvent<HTMLInputElement>) => onCambio(evento.target.checked),
      }
    : { defaultChecked: Boolean(valor) };
  return (
    <label className="check-row">
      <input type="checkbox" {...comunes} {...estado} />
      Sí
    </label>
  );
}

const LARGO_DE_TEXTO = 500;

const controles: Record<TipoCampo, ControlDeUnTipo> = {
  text: entrada({ tipo: 'text', largoMaximo: LARGO_DE_TEXTO }),
  number: entrada({ tipo: 'number', paso: 'any' }),
  currency: entrada({ tipo: 'number', paso: 'any' }),
  date: entrada({ tipo: 'date' }),
  time: entrada({ tipo: 'time', largoMaximo: LARGO_DE_TEXTO }),
  email: entrada({ tipo: 'email', largoMaximo: LARGO_DE_TEXTO }),
  tel: entrada({ tipo: 'tel', largoMaximo: LARGO_DE_TEXTO }),
  select: controlDeLista,
  relation: controlDeVinculo,
  textarea: controlDeTextoLargo,
  checkbox: controlDeCasilla,
};

/** Cada tipo de campo sabe qué control dibuja: agregar un tipo sin su control no compila. */
export function controlDeCampo(pedido: PedidoDeControl): ReactElement {
  return controles[pedido.campo.type](pedido);
}
