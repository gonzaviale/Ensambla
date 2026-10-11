import { useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { DotsThreeIcon, HammerIcon, TableIcon } from '@phosphor-icons/react';
import { Button, InputField, Panel, Spinner, buttonClasses } from '@/components/ui';
import { useEliminarProyecto, useRenombrarProyecto } from '@/hooks/proyectos/mutations';
import { useEstructuraProyecto } from '@/hooks/proyectos/queries';
import { renombrarProyectoSchema, type Bloque, type Proyecto } from '@/types/proyecto';

const MAX_BLOQUES_VISIBLES = 4;

const contarDatos = (cantidad: number) =>
  cantidad === 0 ? 'sin datos' : cantidad === 1 ? '1 dato' : `${cantidad} datos`;

function ContenidoProyecto({ nombre, bloques }: { nombre: string; bloques: Bloque[] }) {
  const ordenados = [...bloques].sort((a, b) => a.posicion - b.posicion);
  const visibles = ordenados.slice(0, MAX_BLOQUES_VISIBLES);
  const resto = ordenados.length - visibles.length;
  return (
    <ul aria-label={`Bloques de ${nombre}`} className="m-0 flex list-none flex-wrap gap-2 p-0">
      {visibles.map((bloque) => (
        <li
          key={bloque.id}
          className="rounded-full border border-accent-200 bg-accent-100 px-3 py-1 text-[15px] leading-snug"
        >
          <span className="font-semibold">{bloque.nombre}</span>
          <span className="text-soft"> · {contarDatos(bloque.campos.length)}</span>
        </li>
      ))}
      {resto > 0 && (
        <li className="rounded-full border border-line px-3 py-1 text-[15px] leading-snug text-soft">
          {resto === 1 ? 'y 1 bloque más' : `y ${resto} bloques más`}
        </li>
      )}
    </ul>
  );
}

// Tarjeta de Mis proyectos: muestra qué tiene adentro y una sola acción principal (Usar).
// Cambiar la estructura es secundaria; renombrar y eliminar quedan en "Más opciones".
export function ProyectoCard({ proyecto }: { proyecto: Proyecto }) {
  const [editando, setEditando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [opcionesAbiertas, setOpcionesAbiertas] = useState(false);
  const [nombre, setNombre] = useState(proyecto.nombre);
  const [errorNombre, setErrorNombre] = useState<string>();
  const botonOpciones = useRef<HTMLButtonElement>(null);
  const opcionesId = useId();
  const estructura = useEstructuraProyecto(proyecto.id);
  const renombrar = useRenombrarProyecto();
  const eliminar = useEliminarProyecto();
  const ocupado = renombrar.isPending || eliminar.isPending;
  const bloques = estructura.data?.bloques;
  const sinBloques = bloques?.length === 0;

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

  function cerrarOpciones(event: KeyboardEvent) {
    if (event.key !== 'Escape') return;
    setOpcionesAbiertas(false);
    botonOpciones.current?.focus();
  }

  return (
    <Panel className="flex h-full flex-col gap-3 p-4 sm:p-5">
      <h3 className="m-0 text-lg">{proyecto.nombre}</h3>
      {proyecto.descripcion && (
        <p className="m-0 line-clamp-2 text-[15px] text-soft">{proyecto.descripcion}</p>
      )}
      <div className="flex-1">
        {estructura.isPending ? (
          <Spinner label="Mirando qué tiene adentro…" />
        ) : sinBloques ? (
          <p className="m-0 text-[15px]">
            Todavía no tiene bloques: armalos para empezar a usarlo.
          </p>
        ) : bloques ? (
          <ContenidoProyecto nombre={proyecto.nombre} bloques={bloques} />
        ) : (
          <p className="m-0 text-[15px] text-soft">No pudimos ver qué tiene adentro.</p>
        )}
      </div>
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
        <div className="flex flex-col gap-2">
          {sinBloques ? (
            <Link
              to="/proyectos/$proyectoId/construir"
              params={{ proyectoId: proyecto.id }}
              className={buttonClasses({ variant: 'solid', className: 'w-full' })}
            >
              <HammerIcon aria-hidden size={20} />
              Armar bloques<span className="sr-only"> de {proyecto.nombre}</span>
            </Link>
          ) : (
            <Link
              to="/proyectos/$proyectoId/usar"
              params={{ proyectoId: proyecto.id }}
              className={buttonClasses({ variant: 'solid', className: 'w-full' })}
            >
              <TableIcon aria-hidden size={20} />
              Usar<span className="sr-only"> {proyecto.nombre}</span>
            </Link>
          )}
          <div className="flex flex-wrap items-center justify-between gap-2">
            {!sinBloques && (
              <Link
                to="/proyectos/$proyectoId/construir"
                params={{ proyectoId: proyecto.id }}
                className={buttonClasses({ size: 'sm', className: 'min-h-12' })}
              >
                {/* En 320px el ícono no entra junto a "Más opciones": se oculta para no partir la fila. */}
                <HammerIcon aria-hidden size={18} className="max-[359px]:hidden" />
                Cambiar estructura<span className="sr-only"> de {proyecto.nombre}</span>
              </Link>
            )}
            <Button
              ref={botonOpciones}
              variant="ghost"
              aria-label={`Más opciones de ${proyecto.nombre}`}
              title="Más opciones"
              aria-expanded={opcionesAbiertas}
              aria-controls={opcionesId}
              onClick={() => setOpcionesAbiertas((abierto) => !abierto)}
              disabled={ocupado}
              className="ml-auto size-12 px-0"
            >
              <DotsThreeIcon aria-hidden size={28} weight="bold" />
            </Button>
          </div>
          {opcionesAbiertas && (
            <div
              id={opcionesId}
              role="group"
              aria-label={`Más opciones de ${proyecto.nombre}`}
              onKeyDown={cerrarOpciones}
              className="flex flex-wrap justify-end gap-2 rounded-md border border-line p-2"
            >
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setNombre(proyecto.nombre);
                  setErrorNombre(undefined);
                  renombrar.reset();
                  setOpcionesAbiertas(false);
                  setEditando(true);
                }}
                disabled={ocupado}
              >
                Renombrar
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-bad"
                onClick={() => {
                  setOpcionesAbiertas(false);
                  setConfirmando(true);
                }}
                disabled={ocupado}
              >
                Eliminar
              </Button>
            </div>
          )}
        </div>
      )}
      {confirmando && (
        <div
          role="group"
          aria-label={`Eliminar ${proyecto.nombre}`}
          className="rounded-md border border-bad/30 p-3"
        >
          <p className="mt-0">
            Se eliminarán el proyecto, sus bloques, datos y registros. Esta acción no se puede
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
