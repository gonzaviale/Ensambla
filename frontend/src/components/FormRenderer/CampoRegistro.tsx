import { InputField } from '@/components/ui';
import { SelectField } from '@/components/ui/SelectField';
import type { Campo } from '@/types/proyecto';

type CampoRegistroProps = {
  campo: Campo;
  valor: string;
  onChange: (valor: string) => void;
  error?: string;
};

export function CampoRegistro({ campo, valor, onChange, error }: CampoRegistroProps) {
  const props = {
    label: `${campo.nombre}${campo.obligatorio ? ' (obligatorio)' : ''}`,
    name: campo.id,
    value: valor,
    required: campo.obligatorio,
    error,
  };
  if (campo.tipo === 'OPCION' || campo.tipo === 'SI_NO') {
    const opciones =
      campo.tipo === 'SI_NO'
        ? [
            { valor: 'true', nombre: 'Sí' },
            { valor: 'false', nombre: 'No' },
          ]
        : campo.opciones.map((opcion) => ({ valor: opcion, nombre: opcion }));
    return (
      <SelectField {...props} onChange={(event) => onChange(event.target.value)}>
        <option value="">Elegí una opción</option>
        {opciones.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>
            {opcion.nombre}
          </option>
        ))}
      </SelectField>
    );
  }
  return (
    <InputField
      {...props}
      type={{ TEXTO: 'text', NUMERO: 'number', FECHA: 'date' }[campo.tipo]}
      step={campo.tipo === 'NUMERO' ? 'any' : undefined}
      inputMode={campo.tipo === 'NUMERO' ? 'decimal' : undefined}
      maxLength={campo.tipo === 'TEXTO' ? 1000 : undefined}
      onChange={(event) => onChange(event.target.value)}
    />
  );
}
