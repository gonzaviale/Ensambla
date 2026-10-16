import { useRef, useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { Button, ErrorState, InputField, Panel, Skeleton, buttonClasses } from '@/components/ui';
import { useUsuario } from '@/hooks/auth/queries';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';
import { useRenombrarProyecto } from '@/hooks/proyectos/mutations';
import { renombrarProyectoSchema } from '@/types/proyecto';
import { enfocarPrimerError } from './enfocarPrimerError';

type TuNegocioProps = { onGuardado: (mensaje: string) => void };

// «Tu negocio»: quién ingresó y cómo se llama el sistema (el más reciente). Se puede renombrar.
export function TuNegocio({ onGuardado }: TuNegocioProps) {
  const usuario = useUsuario();
  const { cargando, error, sistema, hayNegocio } = useMiNegocio();

  return (
    <Panel
      aria-labelledby="cuenta-negocio"
      data-recorrido="cuenta-negocio"
      className="flex flex-col gap-4"
    >
      <h2 id="cuenta-negocio" className="m-0 text-h3">
        Tu negocio
      </h2>
      <dl className="m-0 grid gap-x-4 gap-y-1 text-body sm:grid-cols-[auto_1fr]">
        <dt className="font-semibold">Ingresaste como</dt>
        <dd className="m-0 break-words text-soft">{usuario.nombre}</dd>
        <dt className="font-semibold">Correo</dt>
        <dd className="m-0 break-words text-soft">{usuario.email}</dd>
      </dl>

      {cargando ? (
        <div role="status" aria-label="Cargando tu negocio…" className="flex flex-col gap-3">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-40" />
        </div>
      ) : error ? (
        <ErrorState
          title="No pudimos cargar tu negocio"
          message="Probá de nuevo en unos minutos."
        />
      ) : hayNegocio && sistema ? (
        <FormularioNombre
          key={sistema.id}
          id={sistema.id}
          nombreActual={sistema.nombre}
          onGuardado={onGuardado}
        />
      ) : (
        <div className="flex flex-col items-start gap-3">
          <p className="m-0 text-body text-soft">
            Todavía no armaste tu sistema. Contanos qué hacés y Ensambla lo arma por vos.
          </p>
          <Link to="/bienvenida" className={buttonClasses({ variant: 'solid' })}>
            Armar mi sistema
          </Link>
        </div>
      )}
    </Panel>
  );
}

type FormularioNombreProps = {
  id: string;
  nombreActual: string;
  onGuardado: (mensaje: string) => void;
};

function FormularioNombre({ id, nombreActual, onGuardado }: FormularioNombreProps) {
  const renombrar = useRenombrarProyecto();
  const formulario = useRef<HTMLFormElement>(null);
  const [nombre, setNombre] = useState(nombreActual);
  const [error, setError] = useState<string | undefined>();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const validado = renombrarProyectoSchema.safeParse({ nombre });
    if (!validado.success) {
      setError(nombre.trim() ? 'Usá hasta 120 letras.' : 'Escribí cómo se llama tu negocio.');
      enfocarPrimerError(formulario.current);
      return;
    }
    try {
      const guardado = await renombrar.mutateAsync({ id, nombre: validado.data.nombre });
      setNombre(guardado.nombre);
      onGuardado('Listo, cambiamos el nombre de tu negocio.');
    } catch {
      setError('No pudimos guardar el nombre. Probá de nuevo en unos minutos.');
      enfocarPrimerError(formulario.current);
    }
  }

  return (
    <form ref={formulario} noValidate onSubmit={enviar} className="flex flex-col gap-4">
      <InputField
        label="Nombre de tu negocio"
        required
        autoComplete="organization"
        maxLength={120}
        hint="Es el nombre de tu sistema: así lo ves en la pantalla de inicio."
        value={nombre}
        error={error}
        onChange={(evento) => {
          setError(undefined);
          setNombre(evento.target.value);
        }}
      />
      <Button type="submit" variant="solid" size="lg" fullWidth loading={renombrar.isPending}>
        Guardar el nombre
      </Button>
    </form>
  );
}
