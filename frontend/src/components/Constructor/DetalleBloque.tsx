import { useEffect, useRef, useState } from 'react';
import { ArrowLeftIcon, PencilSimpleIcon, PlusIcon, TrashIcon } from '@phosphor-icons/react';
import { Button, Panel } from '@/components/ui';
import { useEliminarBloque, useRenombrarBloque } from '@/hooks/bloques/mutations';
import { useCampos } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { cn } from '@/lib/cn';
import type { Bloque } from '@/types/bloque';
import { CampoForm } from './CampoForm';
import { ConfirmarEliminacion } from './ConfirmarEliminacion';
import { ListaCampos } from './ListaCampos';
import { NombreBloqueForm } from './NombreBloqueForm';

type Props = {
  proyectoId: string;
  bloque: Bloque;
  /** Lleva el foco al título al aparecer (cuando la persona eligió el bloque). */
  enfocar: boolean;
  onVolver: () => void;
  onEliminado: () => void;
  className?: string;
};

type Modo = 'viendo' | 'renombrando' | 'eliminando' | 'agregandoDato';

export function DetalleBloque({
  proyectoId,
  bloque,
  enfocar,
  onVolver,
  onEliminado,
  className,
}: Props) {
  const [modo, setModo] = useState<Modo>('viendo');
  const titulo = useRef<HTMLHeadingElement>(null);
  const campos = useCampos(bloque.id);
  const renombrar = useRenombrarBloque(proyectoId);
  const eliminar = useEliminarBloque(proyectoId);

  useEffect(() => {
    if (enfocar) titulo.current?.focus();
  }, [enfocar]);

  function volverAVer() {
    setModo('viendo');
    renombrar.reset();
    eliminar.reset();
    requestAnimationFrame(() => titulo.current?.focus());
  }

  // Se usa mutateAsync para eliminar: al refrescarse la lista este panel se desmonta y los
  // callbacks de mutate() ya no se llamarían.
  const cantidad = campos.data?.length;
  const errorEliminar = leerErrorServidor(eliminar.error);

  return (
    <Panel
      aria-labelledby={`bloque-${bloque.id}`}
      className={cn('flex min-w-0 flex-col gap-5 shadow-sm', className)}
    >
      <Button variant="ghost" className="self-start text-base lg:hidden" onClick={onVolver}>
        <ArrowLeftIcon aria-hidden size={18} />
        Ver todos los bloques
      </Button>

      <header className="flex flex-col gap-3">
        <p className="m-0 text-[11px] font-bold tracking-widest text-accent-800 uppercase">
          Bloque
        </p>
        <h2
          ref={titulo}
          id={`bloque-${bloque.id}`}
          tabIndex={-1}
          className="m-0 text-2xl break-words focus:outline-none focus-visible:outline-2"
        >
          {bloque.nombre}
        </h2>

        {modo === 'renombrando' ? (
          <NombreBloqueForm
            etiqueta="Nuevo nombre del bloque"
            nombreInicial={bloque.nombre}
            textoGuardar="Guardar nombre"
            ocupado={renombrar.isPending}
            errorServidor={leerErrorServidor(renombrar.error)}
            onCancelar={volverAVer}
            onGuardar={(nombre) =>
              renombrar.mutate({ bloqueId: bloque.id, nombre }, { onSuccess: volverAVer })
            }
          />
        ) : (
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => setModo('renombrando')} disabled={modo === 'eliminando'}>
              <PencilSimpleIcon aria-hidden size={18} />
              Cambiar nombre
            </Button>
            <Button onClick={() => setModo('eliminando')} disabled={modo === 'eliminando'}>
              <TrashIcon aria-hidden size={18} />
              Eliminar bloque
            </Button>
          </div>
        )}

        {modo === 'eliminando' && (
          <ConfirmarEliminacion
            titulo={`¿Eliminar el bloque «${bloque.nombre}»?`}
            confirmar="Sí, eliminar bloque"
            conservar="No, conservarlo"
            ocupado={eliminar.isPending}
            onConservar={volverAVer}
            onConfirmar={() =>
              void eliminar.mutateAsync(bloque.id).then(onEliminado, () => undefined)
            }
          >
            <p className="m-0">
              Se van a borrar {cantidad ? `sus ${cantidad} datos` : 'sus datos'} y todos los
              registros que tengas cargados en este bloque. No se puede deshacer.
            </p>
            {errorEliminar && (
              <p role="alert" className="mt-2 mb-0 text-bad">
                {errorEliminar.mensaje}
              </p>
            )}
          </ConfirmarEliminacion>
        )}
      </header>

      <section aria-labelledby={`datos-${bloque.id}`} className="flex flex-col gap-3">
        <div>
          <h3 id={`datos-${bloque.id}`} className="m-0 text-lg">
            Datos que guarda este bloque
          </h3>
          <p className="m-0 text-[13px] text-soft">
            Cada dato es una columna de la tabla, como Nombre, Precio o Teléfono.
          </p>
        </div>

        <ListaCampos proyectoId={proyectoId} bloqueId={bloque.id} />

        {modo === 'agregandoDato' ? (
          <CampoForm
            proyectoId={proyectoId}
            bloqueId={bloque.id}
            onListo={volverAVer}
            onCancelar={volverAVer}
          />
        ) : (
          campos.isSuccess && (
            <Button
              variant="primary"
              className="self-start text-base"
              onClick={() => setModo('agregandoDato')}
            >
              <PlusIcon aria-hidden size={20} />
              Agregar dato
            </Button>
          )
        )}
      </section>
    </Panel>
  );
}
