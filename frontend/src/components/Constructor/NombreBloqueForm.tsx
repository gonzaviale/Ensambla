import { useState, type FormEvent } from 'react';
import { Button, InputField } from '@/components/ui';
import { ubicarErrores, type ErrorServidor } from '@/hooks/errores';
import { MAX_NOMBRE_BLOQUE, nombreBloqueSchema } from '@/types/bloque';

type Props = {
  etiqueta: string;
  nombreInicial?: string;
  textoGuardar: string;
  ocupado: boolean;
  errorServidor?: ErrorServidor;
  onGuardar: (nombre: string) => void;
  onCancelar: () => void;
};

export function NombreBloqueForm({
  etiqueta,
  nombreInicial = '',
  textoGuardar,
  ocupado,
  errorServidor,
  onGuardar,
  onCancelar,
}: Props) {
  const [nombre, setNombre] = useState(nombreInicial);
  const [errorLocal, setErrorLocal] = useState<string>();
  const { porInput, general } = ubicarErrores(errorServidor, ['nombre']);

  function enviar(event: FormEvent) {
    event.preventDefault();
    const datos = nombreBloqueSchema.safeParse({ nombre });
    if (!datos.success) {
      setErrorLocal(datos.error.issues[0]?.message);
      return;
    }
    setErrorLocal(undefined);
    onGuardar(datos.data.nombre);
  }

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-3">
      <InputField
        label={etiqueta}
        hint="Por ejemplo: Productos, Clientes o Pedidos."
        value={nombre}
        onChange={(event) => setNombre(event.target.value)}
        error={errorLocal ?? porInput.nombre}
        maxLength={MAX_NOMBRE_BLOQUE}
        disabled={ocupado}
        autoComplete="off"
        autoFocus
        onKeyDown={(event) => {
          if (event.key === 'Escape' && !ocupado) onCancelar();
        }}
      />
      {general && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {general}
        </p>
      )}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" variant="solid" loading={ocupado}>
          {ocupado ? 'Guardando…' : textoGuardar}
        </Button>
        <Button onClick={onCancelar} disabled={ocupado}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}
