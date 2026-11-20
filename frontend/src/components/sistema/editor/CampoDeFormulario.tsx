import { comportamientoDe } from '@/lib/sistema/comportamiento';
import type { Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { controlDe } from './registroDeControles';
import type { ControlProps } from './tiposDeControl';

// El control de un campo según su tipo: lo que `control()` del mockup escribía como HTML.
export function ControlDeCampo(props: ControlProps) {
  const Control = controlDe(props.campo.type);
  return <Control {...props} />;
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
  const larga = comportamientoDe(campo.type).ocupaTodoElAncho();
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
