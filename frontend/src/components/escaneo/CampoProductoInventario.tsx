import type { Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { ControlDeCampo } from '@/components/sistema/editor/CampoDeFormulario';
import { Icon } from '@/components/sistema/Icon';

export function CampoProductoInventario({
  campo,
  valor,
  sistema,
  error,
  onCambiar,
}: {
  campo: Campo;
  valor: Valor;
  sistema: Sistema;
  error?: string;
  onCambiar: (valor: Valor) => void;
}) {
  const desplegable = campo.type === 'select' || campo.type === 'relation';
  return (
    <div
      className={`field escaneo-campo ${error ? 'invalid' : ''}`}
      data-clave={campo.key}
      data-tipo={campo.type}
    >
      <div className="escaneo-etiqueta-campo">
        <label htmlFor={`escaneo-${campo.key}`}>
          {campo.label}
          {campo.required && <span aria-hidden="true"> *</span>}
        </label>
      </div>
      <div className={`escaneo-control ${desplegable ? 'escaneo-control-elegir' : ''}`}>
        <ControlDeCampo
          sistema={sistema}
          campo={campo}
          valor={valor}
          id={`escaneo-${campo.key}`}
          onCambio={onCambiar}
          invalido={!!error}
        />
        {desplegable && <Icon name="down" />}
      </div>
      <span className="field-error" id={`escaneo-${campo.key}-error`}>
        {error}
      </span>
    </div>
  );
}
