import type { Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { controlDeCampo, propiedadesComunes, valorDelControl } from './controlesDeCampo';

type ControlProps = {
  sistema: Sistema | undefined;
  campo: Campo;
  valor: Valor;
  id: string;
  name?: string;
  /** Con `onCambio` el control es controlado; sin él, el formulario lee los valores al enviarse. */
  onCambio?: (valor: string | boolean) => void;
  etiqueta?: string;
  invalido?: boolean;
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
  invalido = false,
}: ControlProps) {
  return controlDeCampo({
    sistema,
    campo,
    valor,
    onCambio,
    comunes: propiedadesComunes(campo, id, name, etiqueta, invalido),
    valorDelControl: valorDelControl(valor, onCambio),
  });
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
