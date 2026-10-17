import { useEffect, useRef, useState } from 'react';
import {
  ArrowLeftIcon,
  PencilSimpleIcon,
  PlusIcon,
  TableIcon,
  TrashIcon,
} from '@phosphor-icons/react';
import { Button, ConfirmDialog, Panel, Sheet } from '@/components/ui';
import { useEliminarBloque, useRenombrarBloque } from '@/hooks/bloques/mutations';
import { useCampos } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { useRegistros } from '@/hooks/registros/queries';
import { cn } from '@/lib/cn';
import type { Bloque } from '@/types/bloque';
import { CampoForm } from './CampoForm';
import { ListaCampos } from './ListaCampos';
import { NombreBloqueForm } from './NombreBloqueForm';

type Props = {
  proyectoId: string;
  bloque: Bloque;
  /** Lleva el foco al título al aparecer (cuando la persona eligió la tabla). */
  enfocar: boolean;
  onVolver: () => void;
  onEliminado: () => void;
  className?: string;
};

type Modo = 'viendo' | 'renombrando' | 'eliminando' | 'agregandoDato';

// «Se van a borrar sus 2 columnas y las 3 filas que cargaste.» con el conteo real de lo que se pierde.
function textoQueSePierde(columnas: number | undefined, filas: number | undefined) {
  const delante =
    columnas === 1 ? 'su única columna' : columnas ? `sus ${columnas} columnas` : 'sus columnas';
  if (filas === undefined)
    return `Se van a borrar ${delante} y todas las filas que tengas cargadas.`;
  if (filas === 0) return `Se van a borrar ${delante}. Todavía no tiene filas cargadas.`;
  if (filas === 1) return `Se van a borrar ${delante} y la fila que cargaste.`;
  return `Se van a borrar ${delante} y las ${filas} filas que cargaste.`;
}

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
  // Una sola fila alcanza para saber cuántas hay: la confirmación de borrar nombra el conteo real.
  const registros = useRegistros(bloque.id, { tamanio: 1 });

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
  const errorEliminar = leerErrorServidor(eliminar.error);

  return (
    <Panel
      aria-labelledby={`bloque-${bloque.id}`}
      className={cn('workspace-detail flex min-w-0 flex-col gap-6', className)}
    >
      <Button variant="ghost" className="self-start lg:hidden" onClick={onVolver}>
        <ArrowLeftIcon aria-hidden size={20} />
        Ver todas las tablas
      </Button>

      <header className="flex flex-col gap-3">
        <div className="workspace-block-heading flex items-center gap-3">
          <span className="workspace-icon workspace-icon-0">
            <TableIcon aria-hidden size={25} />
          </span>
          <div className="min-w-0">
            <p className="m-0 text-caption font-bold tracking-widest text-petrol uppercase">
              Tabla
            </p>
            <h2
              ref={titulo}
              id={`bloque-${bloque.id}`}
              tabIndex={-1}
              className="m-0 text-h2 break-words text-petrol focus:outline-none focus-visible:outline-3"
            >
              {bloque.nombre}
            </h2>
          </div>
        </div>

        {modo === 'renombrando' ? (
          <NombreBloqueForm
            etiqueta="Nuevo nombre de la tabla"
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
          <div className="flex flex-wrap gap-3">
            <Button onClick={() => setModo('renombrando')}>
              <PencilSimpleIcon aria-hidden size={20} />
              Cambiar nombre
            </Button>
            <Button variant="danger" onClick={() => setModo('eliminando')}>
              <TrashIcon aria-hidden size={20} />
              Eliminar tabla
            </Button>
          </div>
        )}
      </header>

      <section aria-labelledby={`datos-${bloque.id}`} className="flex flex-col gap-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="min-w-0">
            <h3 id={`datos-${bloque.id}`} className="m-0 text-h3 text-petrol">
              Columnas de esta tabla
            </h3>
            <p className="m-0 text-hint text-soft">
              Cada columna es un dato que guardás, como Nombre, Precio o Teléfono.
            </p>
          </div>
          {campos.isSuccess && modo !== 'agregandoDato' && (
            <Button variant="primary" onClick={() => setModo('agregandoDato')}>
              <PlusIcon aria-hidden size={20} />
              Agregar columna
            </Button>
          )}
        </div>

        <ListaCampos proyectoId={proyectoId} bloqueId={bloque.id} />
      </section>

      <Sheet
        open={modo === 'agregandoDato'}
        onClose={volverAVer}
        title="Nueva columna"
        description={`Se suma a la tabla «${bloque.nombre}».`}
      >
        <CampoForm
          proyectoId={proyectoId}
          bloqueId={bloque.id}
          onListo={volverAVer}
          onCancelar={volverAVer}
        />
      </Sheet>

      <ConfirmDialog
        open={modo === 'eliminando'}
        titulo={`¿Eliminar la tabla «${bloque.nombre}»?`}
        confirmar="Sí, eliminar tabla"
        conservar="No, conservarla"
        ocupado={eliminar.isPending}
        error={errorEliminar?.mensaje}
        onConservar={volverAVer}
        onConfirmar={() => void eliminar.mutateAsync(bloque.id).then(onEliminado, () => undefined)}
      >
        <p className="m-0">
          {textoQueSePierde(campos.data?.length, registros.data?.totalElements)} No se puede
          deshacer.
        </p>
      </ConfirmDialog>
    </Panel>
  );
}
