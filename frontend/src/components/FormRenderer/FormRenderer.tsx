import { useState, type FormEvent } from 'react';
import { Button } from '@/components/ui';
import { CampoRegistro } from './CampoRegistro';
import { valoresIniciales, valoresParaGuardar } from '@/lib/formularioRegistro';
import type { Campo } from '@/types/proyecto';
import type { ValoresRegistro } from '@/types/registro';

type FormRendererProps = {
  campos: readonly Campo[];
  onSubmit: (valores: ValoresRegistro) => void;
};

export function FormRenderer({ campos, onSubmit }: FormRendererProps) {
  const [valores, setValores] = useState(() => valoresIniciales(campos));
  const camposOrdenados = [...campos].sort((a, b) => a.posicion - b.posicion);

  function guardar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit(valoresParaGuardar(campos, valores));
  }

  if (campos.length === 0) {
    return <p className="m-0 text-soft">Este bloque todavía no tiene campos para cargar datos.</p>;
  }

  return (
    <form noValidate onSubmit={guardar} className="flex flex-col gap-5">
      <p className="m-0 text-sm text-muted">Completá los datos marcados como obligatorios.</p>
      {camposOrdenados.map((campo) => (
        <CampoRegistro
          key={campo.id}
          campo={campo}
          valor={valores[campo.id] ?? ''}
          onChange={(valor) => setValores((actuales) => ({ ...actuales, [campo.id]: valor }))}
        />
      ))}
      <Button type="submit" variant="solid" className="self-start">
        Guardar registro
      </Button>
    </form>
  );
}
