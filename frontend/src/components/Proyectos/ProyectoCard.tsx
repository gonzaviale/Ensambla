import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { Button, InputField, Panel, buttonClasses } from '@/components/ui';
import { useEliminarProyecto, useRenombrarProyecto } from '@/hooks/proyectos/mutations';
import { renombrarProyectoSchema, type Proyecto } from '@/types/proyecto';

export function ProyectoCard({ proyecto }: { proyecto: Proyecto }) {
  const [editando, setEditando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [nombre, setNombre] = useState(proyecto.nombre);
  const [errorNombre, setErrorNombre] = useState<string>();
  const renombrar = useRenombrarProyecto();
  const eliminar = useEliminarProyecto();
  const ocupado = renombrar.isPending || eliminar.isPending;
  async function guardar(event: FormEvent) {
    event.preventDefault();
    const datos = renombrarProyectoSchema.safeParse({ nombre });
    if (!datos.success) {
      setErrorNombre(datos.error.issues[0]?.message);
      return;
    }
    try {
      await renombrar.mutateAsync({ id: proyecto.id, ...datos.data });
      setEditando(false);
    } catch {
      /* El error se muestra en la tarjeta. */
    }
  }
  return (
    <Panel className="flex h-full flex-col gap-3">
      <h3 className="m-0 text-lg">{proyecto.nombre}</h3>
      <p className="m-0 flex-1 text-soft">
        {proyecto.descripcion || 'Un espacio para organizar tu negocio.'}
      </p>
      {editando ? (
        <form onSubmit={(e) => void guardar(e)} className="flex flex-col gap-3">
          <InputField
            label="Nuevo nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            error={errorNombre}
            maxLength={120}
            required
            disabled={ocupado}
            autoFocus
          />
          <Button type="submit" disabled={ocupado}>
            Guardar nombre
          </Button>
          <Button onClick={() => setEditando(false)} disabled={ocupado}>
            Cancelar
          </Button>
        </form>
      ) : (
        <div className="flex flex-wrap gap-2">
          <Link
            to="/proyectos/$proyectoId/construir"
            params={{ proyectoId: proyecto.id }}
            className={buttonClasses({ variant: 'primary' })}
            aria-label={`Abrir ${proyecto.nombre}`}
          >
            Abrir proyecto
          </Link>
          <Button
            variant="ghost"
            onClick={() => {
              setNombre(proyecto.nombre);
              setErrorNombre(undefined);
              renombrar.reset();
              setEditando(true);
            }}
            disabled={ocupado}
          >
            Renombrar
          </Button>
          <Button variant="ghost" onClick={() => setConfirmando(true)} disabled={ocupado}>
            Eliminar
          </Button>
        </div>
      )}
      {confirmando && (
        <div
          role="group"
          aria-label={`Eliminar ${proyecto.nombre}`}
          className="rounded-md border border-bad/30 p-3"
        >
          <p className="mt-0">
            Se eliminarán el proyecto, sus bloques, campos y registros. Esta acción no se puede
            deshacer.
          </p>
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => eliminar.mutate(proyecto.id)} disabled={ocupado}>
              Sí, eliminar proyecto
            </Button>
            <Button
              onClick={() => {
                setConfirmando(false);
                eliminar.reset();
              }}
              disabled={ocupado}
            >
              Conservar proyecto
            </Button>
          </div>
        </div>
      )}
      {(renombrar.error || eliminar.error) && (
        <p role="alert" className="text-bad">
          {(renombrar.error || eliminar.error)?.message}
        </p>
      )}
    </Panel>
  );
}
