import { useState, type FormEvent } from 'react';
import { Button } from '@/components/ui';
import { CampoRegistro } from './CampoRegistro';
import {
  validarFormulario,
  valoresIniciales,
  valoresParaGuardar,
  type ErroresFormulario,
} from '@/lib/formularioRegistro';
import type { Campo } from '@/types/proyecto';
import type { ValoresRegistro } from '@/types/registro';

type FormRendererProps = {
  campos: readonly Campo[];
  onSubmit: (valores: ValoresRegistro) => void;
};

export function FormRenderer({ campos, onSubmit }: FormRendererProps) {
  const [valores, setValores] = useState(() => valoresIniciales(campos));
  const [errores, setErrores] = useState<ErroresFormulario>({});
  const camposOrdenados = [...campos].sort((a, b) => a.posicion - b.posicion);

  function guardar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nuevosErrores = validarFormulario(campos, valores);
    for (const campo of campos) {
      const control = event.currentTarget.elements.namedItem(campo.id);
      if (control instanceof HTMLInputElement && control.validity.badInput)
        nuevosErrores[campo.id] =
          campo.tipo === 'NUMERO' ? 'Tiene que ser un número' : 'Tiene que ser una fecha válida';
    }
    setErrores(nuevosErrores);
    const primero = camposOrdenados.find((campo) => nuevosErrores[campo.id]);
    if (primero) {
      const control = event.currentTarget.elements.namedItem(primero.id);
      if (control instanceof HTMLElement) control.focus();
      return;
    }
    onSubmit(valoresParaGuardar(campos, valores));
  }

  if (campos.length === 0) {
    return <p className="m-0 text-soft">Este bloque todavía no tiene campos para cargar datos.</p>;
  }

  return (
    <form noValidate onSubmit={guardar} className="flex flex-col gap-5">
      <p className="m-0 text-sm text-muted">Completá los datos marcados como obligatorios.</p>
      {Object.keys(errores).length > 0 && (
        <p role="alert" className="m-0 text-sm text-bad">
          Revisá los campos marcados para guardar.
        </p>
      )}
      {camposOrdenados.map((campo) => (
        <CampoRegistro
          key={campo.id}
          campo={campo}
          valor={valores[campo.id] ?? ''}
          error={errores[campo.id]}
          onChange={(valor) => {
            setValores((actuales) => ({ ...actuales, [campo.id]: valor }));
            setErrores((actuales) =>
              Object.fromEntries(Object.entries(actuales).filter(([id]) => id !== campo.id)),
            );
          }}
        />
      ))}
      <Button type="submit" variant="solid" className="self-start">
        Guardar registro
      </Button>
    </form>
  );
}
