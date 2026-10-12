import { useRef, useState } from 'react';
import { PencilSimpleIcon, TrashIcon } from '@phosphor-icons/react';
import { Badge, Button, ConfirmDialog, Sheet, SkeletonCards } from '@/components/ui';
import { useEliminarCampo } from '@/hooks/bloques/mutations';
import { useBloques, useCampos } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { describirValorInicial } from '@/lib/campos';
import type { Campo } from '@/types/campo';
import { CampoForm } from './CampoForm';
import { TIPOS_DE_DATO } from './tiposDeDato';

type Props = { proyectoId: string; bloqueId: string };

type Accion = { campoId: string; tipo: 'cambiar' | 'eliminar' };

export function ListaCampos({ proyectoId, bloqueId }: Props) {
  const campos = useCampos(bloqueId);
  const tablas = useBloques(proyectoId);
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

  if (campos.isPending) return <SkeletonCards cantidad={2} />;

  if (campos.isError) {
    return (
      <div
        role="alert"
        className="flex flex-col items-start gap-2 rounded-md border border-bad/30 p-3"
      >
        <p className="m-0">No pudimos cargar las columnas de esta tabla.</p>
        <p className="m-0 text-hint text-soft">{leerErrorServidor(campos.error)?.mensaje}</p>
        <Button variant="solid" onClick={() => void campos.refetch()}>
          Reintentar
        </Button>
      </div>
    );
  }

  if (campos.data.length === 0) {
    return (
      <p className="m-0 rounded-md border border-dashed border-fg/30 p-4 text-body text-soft">
        Esta tabla todavía no tiene columnas. Agregá la primera, por ejemplo «Nombre».
      </p>
    );
  }

  const errorEliminar = leerErrorServidor(eliminar.error);
  const aCambiar = campos.data.find((c) => c.id === accion?.campoId && accion.tipo === 'cambiar');
  const aEliminar = campos.data.find((c) => c.id === accion?.campoId && accion.tipo === 'eliminar');

  return (
    <>
      <ul
        ref={lista}
        tabIndex={-1}
        aria-label="Columnas de la tabla"
        className="m-0 flex list-none flex-col gap-3 p-0 focus:outline-none"
      >
        {campos.data.map((campo) => (
          <li key={campo.id}>
            <div className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4 sm:flex-row sm:items-center sm:justify-between">
              <ResumenCampo
                campo={campo}
                tablaConectada={
                  tablas.data?.find((tabla) => tabla.id === campo.bloqueRelacionadoId)?.nombre
                }
              />
              <div className="flex shrink-0 flex-wrap gap-2">
                <Button
                  data-cambiar={campo.id}
                  aria-label={`Cambiar ${campo.nombre}`}
                  onClick={() => setAccion({ campoId: campo.id, tipo: 'cambiar' })}
                >
                  <PencilSimpleIcon aria-hidden size={20} />
                  Cambiar
                </Button>
                <Button
                  variant="danger"
                  aria-label={`Eliminar ${campo.nombre}`}
                  onClick={() => setAccion({ campoId: campo.id, tipo: 'eliminar' })}
                >
                  <TrashIcon aria-hidden size={20} />
                  Eliminar
                </Button>
              </div>
            </div>
          </li>
        ))}
      </ul>

      <Sheet
        open={aCambiar !== undefined}
        onClose={() => terminar(aCambiar?.id)}
        title="Cambiar columna"
        description={aCambiar ? `«${aCambiar.nombre}»` : undefined}
      >
        {aCambiar && (
          <CampoForm
            proyectoId={proyectoId}
            bloqueId={bloqueId}
            campo={aCambiar}
            onListo={() => terminar(aCambiar.id)}
            onCancelar={() => terminar(aCambiar.id)}
          />
        )}
      </Sheet>

      <ConfirmDialog
        open={aEliminar !== undefined}
        titulo={`¿Eliminar la columna «${aEliminar?.nombre ?? ''}»?`}
        confirmar="Sí, eliminar columna"
        conservar="No, conservarla"
        ocupado={eliminar.isPending}
        error={errorEliminar?.mensaje}
        onConservar={() => terminar(aEliminar?.id)}
        onConfirmar={() =>
          aEliminar && eliminar.mutate(aEliminar.id, { onSuccess: () => terminar() })
        }
      >
        <p className="m-0">
          Se va a borrar esta columna y lo que tengas anotado en ella en cada fila. No se puede
          deshacer.
        </p>
      </ConfirmDialog>
    </>
  );
}

function ResumenCampo({ campo, tablaConectada }: { campo: Campo; tablaConectada?: string }) {
  const valor = describirValorInicial(campo.tipo, campo.valorInicial);
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <p className="m-0 flex flex-wrap items-center gap-2 text-body font-semibold break-words">
        <span>{campo.nombre}</span>
        {campo.obligatorio && <Badge tone="accent">Obligatorio</Badge>}
      </p>
      <p className="m-0 text-hint text-soft">
        {TIPOS_DE_DATO[campo.tipo].nombre}
        {campo.tipo === 'OPCION' && `: ${campo.opciones.join(', ')}`}
      </p>
      {campo.tipo === 'RELACION' && tablaConectada && (
        <p className="m-0 text-hint text-soft">Se conecta con «{tablaConectada}»</p>
      )}
      {valor !== null && <p className="m-0 text-hint text-soft">Valor inicial: {valor}</p>}
    </div>
  );
}
