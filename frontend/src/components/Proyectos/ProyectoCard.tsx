import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { HammerIcon, PencilSimpleIcon, TableIcon, TrashIcon } from '@phosphor-icons/react';
import {
  Button,
  ConfirmDialog,
  InputField,
  Isotipo,
  Menu,
  Panel,
  Sheet,
  Skeleton,
  buttonClasses,
} from '@/components/ui';
import { useEliminarProyecto, useRenombrarProyecto } from '@/hooks/proyectos/mutations';
import { haceCuanto } from '@/lib/fechas';
import { renombrarProyectoSchema, type Bloque, type Proyecto } from '@/types/proyecto';

type Props = {
  proyecto: Proyecto;
  /** Estructura ya cargada (o `undefined` mientras llega). */
  bloques: Bloque[] | undefined;
  cargando: boolean;
};

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`;

function resumir(bloques: Bloque[], actualizadoEn: string) {
  const columnas = bloques.reduce((total, bloque) => total + bloque.campos.length, 0);
  const cuando = haceCuanto(actualizadoEn);
  return [
    plural(bloques.length, 'tabla', 'tablas'),
    plural(columnas, 'columna', 'columnas'),
    cuando && `usado ${cuando}`,
  ]
    .filter(Boolean)
    .join(' · ');
}

// Tarjeta de Mis sistemas con anatomía fija (mismo alto en toda la fila): nombre, resumen de una
// línea, acción principal (Abrir) y secundaria (Armar tablas). Renombrar y eliminar viven en el
// menú "Más acciones" y abren una hoja o una confirmación: la tarjeta nunca se expande.
export function ProyectoCard({ proyecto, bloques, cargando }: Props) {
  const [renombrando, setRenombrando] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [nombre, setNombre] = useState(proyecto.nombre);
  const [errorNombre, setErrorNombre] = useState<string>();
  const renombrar = useRenombrarProyecto();
  const eliminar = useEliminarProyecto();
  const sinTablas = bloques?.length === 0;

  async function guardar(event: FormEvent) {
    event.preventDefault();
    const datos = renombrarProyectoSchema.safeParse({ nombre });
    if (!datos.success) {
      setErrorNombre(datos.error.issues[0]?.message);
      return;
    }
    try {
      await renombrar.mutateAsync({ id: proyecto.id, ...datos.data });
      setRenombrando(false);
    } catch {
      /* El error se muestra dentro de la hoja. */
    }
  }

  return (
    <Panel interactive className="flex h-full flex-col gap-4 p-4 sm:p-5">
      <div className="flex items-start gap-3">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-md bg-sand-100">
          <Isotipo className="size-8" />
        </span>
        <h3 className="m-0 line-clamp-2 min-w-0 flex-1 pt-1 text-h3 leading-tight break-words">
          {proyecto.nombre}
        </h3>
        <Menu
          className="-mt-1 -mr-2 shrink-0"
          label={`Más acciones de ${proyecto.nombre}`}
          items={[
            {
              label: 'Cambiar nombre',
              icon: <PencilSimpleIcon size={22} />,
              onSelect: () => {
                setNombre(proyecto.nombre);
                setErrorNombre(undefined);
                renombrar.reset();
                setRenombrando(true);
              },
            },
            {
              label: 'Eliminar sistema',
              icon: <TrashIcon size={22} />,
              tone: 'danger',
              onSelect: () => {
                eliminar.reset();
                setEliminando(true);
              },
            },
          ]}
        />
      </div>

      <div className="min-h-6 text-hint text-soft">
        {cargando ? (
          <Skeleton className="h-5 w-3/4" />
        ) : bloques ? (
          <p className="m-0">
            {sinTablas
              ? 'Todavía no tiene tablas: armalas para empezar a cargarlo.'
              : resumir(bloques, proyecto.actualizadoEn)}
          </p>
        ) : (
          <p className="m-0">No pudimos ver qué tiene adentro.</p>
        )}
      </div>

      <div className="mt-auto flex flex-col gap-2">
        {sinTablas ? (
          <Link
            to="/proyectos/$proyectoId/construir"
            params={{ proyectoId: proyecto.id }}
            className={buttonClasses({ variant: 'solid', fullWidth: true })}
          >
            <HammerIcon aria-hidden size={22} />
            Armar tablas<span className="sr-only"> de {proyecto.nombre}</span>
          </Link>
        ) : (
          <>
            <Link
              to="/proyectos/$proyectoId/usar"
              params={{ proyectoId: proyecto.id }}
              className={buttonClasses({ variant: 'solid', fullWidth: true })}
            >
              <TableIcon aria-hidden size={22} />
              Abrir<span className="sr-only"> {proyecto.nombre}</span>
            </Link>
            <Link
              to="/proyectos/$proyectoId/construir"
              params={{ proyectoId: proyecto.id }}
              className={buttonClasses({ fullWidth: true })}
            >
              <HammerIcon aria-hidden size={22} />
              Armar tablas<span className="sr-only"> de {proyecto.nombre}</span>
            </Link>
          </>
        )}
      </div>

      <Sheet
        open={renombrando}
        onClose={() => setRenombrando(false)}
        title="Cambiar nombre"
        description={`Ahora se llama «${proyecto.nombre}».`}
        locked={renombrar.isPending}
      >
        <form onSubmit={(e) => void guardar(e)} className="flex flex-col gap-4">
          <InputField
            label="Nuevo nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            error={errorNombre}
            maxLength={120}
            required
            disabled={renombrar.isPending}
          />
          {renombrar.error && (
            <p role="alert" className="m-0 font-semibold text-bad">
              {renombrar.error.message}
            </p>
          )}
          <div className="flex flex-wrap justify-end gap-3">
            <Button onClick={() => setRenombrando(false)} disabled={renombrar.isPending}>
              Cancelar
            </Button>
            <Button type="submit" variant="solid" loading={renombrar.isPending}>
              Guardar nombre
            </Button>
          </div>
        </form>
      </Sheet>

      <ConfirmDialog
        open={eliminando}
        titulo={`¿Eliminar «${proyecto.nombre}»?`}
        confirmar="Sí, eliminar sistema"
        conservar="Conservar sistema"
        ocupado={eliminar.isPending}
        error={eliminar.error?.message}
        onConfirmar={() => eliminar.mutate(proyecto.id, { onSuccess: () => setEliminando(false) })}
        onConservar={() => setEliminando(false)}
      >
        <p className="m-0">
          Se eliminan{' '}
          {bloques
            ? `sus ${plural(bloques.length, 'tabla', 'tablas')}, sus columnas`
            : 'sus tablas, sus columnas'}{' '}
          y todas las filas que cargaste. Esta acción no se puede deshacer.
        </p>
      </ConfirmDialog>
    </Panel>
  );
}
