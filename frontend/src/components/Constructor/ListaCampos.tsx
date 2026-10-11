import { useRef, useState } from 'react';
import { PencilSimpleIcon, TrashIcon } from '@phosphor-icons/react';
import { Button, Spinner } from '@/components/ui';
import { useEliminarCampo } from '@/hooks/bloques/mutations';
import { useCampos } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { describirValorInicial } from '@/lib/campos';
import type { Campo } from '@/types/campo';
import { CampoForm } from './CampoForm';
import { ConfirmarEliminacion } from './ConfirmarEliminacion';
import { TIPOS_DE_DATO } from './tiposDeDato';

type Props = { proyectoId: string; bloqueId: string };

type Accion = { campoId: string; tipo: 'cambiar' | 'eliminar' };

export function ListaCampos({ proyectoId, bloqueId }: Props) {
  const campos = useCampos(bloqueId);
  const [accion, setAccion] = useState<Accion>();
  const lista = useRef<HTMLUListElement>(null);
  const eliminar = useEliminarCampo(proyectoId, bloqueId);

  function terminar(enfocarId?: string) {
    setAccion(undefined);
    eliminar.reset();
    requestAnimationFrame(() => {
      const boton = enfocarId
        ? lista.current?.querySelector<HTMLElement>(`[data-cambiar="${enfocarId}"]`)
        : null;
      (boton ?? lista.current)?.focus();
    });
  }

  if (campos.isPending) return <Spinner label="Cargando los datos del bloque…" />;

  if (campos.isError) {
    return (
      <div
        role="alert"
        className="flex flex-col items-start gap-2 rounded-md border border-bad/30 p-3"
      >
        <p className="m-0">No pudimos cargar los datos de este bloque.</p>
        <p className="m-0 text-[13px] text-soft">{leerErrorServidor(campos.error)?.mensaje}</p>
        <Button variant="primary" onClick={() => void campos.refetch()}>
          Reintentar
        </Button>
      </div>
    );
  }

  if (campos.data.length === 0) {
    return (
      <p className="m-0 rounded-md border border-dashed border-fg/25 p-4 text-soft">
        Este bloque todavía no tiene datos. Agregá el primero, por ejemplo «Nombre».
      </p>
    );
  }

  const errorEliminar = leerErrorServidor(eliminar.error);

  return (
    <ul
      ref={lista}
      tabIndex={-1}
      aria-label="Datos del bloque"
      className="m-0 flex list-none flex-col gap-2 p-0 focus:outline-none"
    >
      {campos.data.map((campo) => (
        <li key={campo.id}>
          {accion?.campoId === campo.id && accion.tipo === 'cambiar' ? (
            <CampoForm
              proyectoId={proyectoId}
              bloqueId={bloqueId}
              campo={campo}
              onListo={() => terminar(campo.id)}
              onCancelar={() => terminar(campo.id)}
            />
          ) : (
            <div className="flex flex-col gap-3 rounded-md border border-line bg-surface p-3 sm:flex-row sm:items-center sm:justify-between">
              <ResumenCampo campo={campo} />
              <div className="flex shrink-0 flex-wrap gap-2">
                <Button
                  data-cambiar={campo.id}
                  aria-label={`Cambiar ${campo.nombre}`}
                  disabled={accion !== undefined}
                  onClick={() => setAccion({ campoId: campo.id, tipo: 'cambiar' })}
                >
                  <PencilSimpleIcon aria-hidden size={18} />
                  Cambiar
                </Button>
                <Button
                  aria-label={`Eliminar ${campo.nombre}`}
                  disabled={accion !== undefined}
                  onClick={() => setAccion({ campoId: campo.id, tipo: 'eliminar' })}
                >
                  <TrashIcon aria-hidden size={18} />
                  Eliminar
                </Button>
              </div>
            </div>
          )}

          {accion?.campoId === campo.id && accion.tipo === 'eliminar' && (
            <div className="mt-2">
              <ConfirmarEliminacion
                titulo={`¿Eliminar el dato «${campo.nombre}»?`}
                confirmar="Sí, eliminar dato"
                conservar="No, conservarlo"
                ocupado={eliminar.isPending}
                onConservar={() => terminar(campo.id)}
                onConfirmar={() => eliminar.mutate(campo.id, { onSuccess: () => terminar() })}
              >
                <p className="m-0">
                  Se va a borrar esta columna y lo que tengas anotado en ella en cada registro. No
                  se puede deshacer.
                </p>
                {errorEliminar && (
                  <p role="alert" className="mt-2 mb-0 text-bad">
                    {errorEliminar.mensaje}
                  </p>
                )}
              </ConfirmarEliminacion>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}

function ResumenCampo({ campo }: { campo: Campo }) {
  const valor = describirValorInicial(campo.tipo, campo.valorInicial);
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <p className="m-0 flex flex-wrap items-center gap-2 text-base font-semibold break-words">
        <span>{campo.nombre}</span>
        {campo.obligatorio && (
          <span className="rounded-sm bg-accent-100 px-1.5 py-0.5 text-[12px] font-bold text-accent-900">
            Obligatorio
          </span>
        )}
      </p>
      <p className="m-0 text-[13px] text-soft">
        {TIPOS_DE_DATO[campo.tipo].nombre}
        {campo.tipo === 'OPCION' && `: ${campo.opciones.join(', ')}`}
      </p>
      {valor !== null && <p className="m-0 text-[13px] text-soft">Valor inicial: {valor}</p>}
    </div>
  );
}
