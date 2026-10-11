import { useRef, useState } from 'react';
import { useNavigate, useParams, useSearch } from '@tanstack/react-router';
import { SquaresFourIcon } from '@phosphor-icons/react';
import { AgregarBloque } from '@/components/Constructor/AgregarBloque';
import { DetalleBloque } from '@/components/Constructor/DetalleBloque';
import { ListaBloques } from '@/components/Constructor/ListaBloques';
import { EmptyState, ErrorState, PageHeading, Spinner } from '@/components/ui';
import { useBloques } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { useProyecto } from '@/hooks/proyectos/queries';
import { cn } from '@/lib/cn';

const RUTA = '/_app/proyectos/$proyectoId/construir';

// T-019 a T-021: «Armar mi sistema» (bloques = tablas, campos = columnas).
// Escritorio: lista de tablas a la izquierda y la tabla elegida a la derecha.
// Celular: primero la lista; al tocar una tabla se ve sólo su detalle, con "Ver todas las tablas"
// (y el botón atrás del teléfono) para volver. La tabla elegida va en la URL (?bloque=).
export function ConstructorPage() {
  const { proyectoId } = useParams({ from: RUTA });
  const { bloque: elegidoId } = useSearch({ from: RUTA });
  const navigate = useNavigate();
  const proyecto = useProyecto(proyectoId);
  const bloques = useBloques(proyectoId);
  const [enfocarDetalle, setEnfocarDetalle] = useState(false);
  const tituloLista = useRef<HTMLHeadingElement>(null);

  const ordenados = [...(bloques.data ?? [])].sort((a, b) => a.posicion - b.posicion);
  const seleccionado = ordenados.find((bloque) => bloque.id === elegidoId) ?? ordenados[0];
  const enDetalle = Boolean(elegidoId) && seleccionado?.id === elegidoId;

  function irA(bloqueId: string | undefined, { replace = false } = {}) {
    return navigate({
      to: '/proyectos/$proyectoId/construir',
      params: { proyectoId },
      search: bloqueId ? { bloque: bloqueId } : {},
      replace,
    });
  }

  function elegir(bloqueId: string) {
    setEnfocarDetalle(true);
    void irA(bloqueId);
  }

  function volverALista() {
    const anterior = seleccionado?.id;
    setEnfocarDetalle(false);
    void irA(undefined).then(() =>
      document.querySelector<HTMLElement>(`[data-bloque-id="${anterior}"]`)?.focus(),
    );
  }

  function alEliminar() {
    setEnfocarDetalle(false);
    void irA(undefined, { replace: true }).then(() => tituloLista.current?.focus());
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Volver a Mis sistemas y pasar a Cargar y ver lo resuelven los breadcrumbs y el selector de modo del layout. */}
      <PageHeading
        kicker="Armar"
        title={proyecto.nombre}
        lead="Elegí cómo se ordena tu información: tablas, como Productos o Clientes, y las columnas que guarda cada una."
      />

      {bloques.isPending ? (
        <Spinner label="Cargando las tablas…" />
      ) : bloques.isError ? (
        <ErrorState
          title="No pudimos cargar las tablas"
          message={leerErrorServidor(bloques.error)?.mensaje ?? ''}
          onRetry={() => void bloques.refetch()}
        />
      ) : (
        <div
          className={cn(
            'grid items-start gap-6',
            seleccionado && 'lg:grid-cols-[minmax(16rem,20rem)_minmax(0,1fr)]',
          )}
        >
          <section
            aria-labelledby="titulo-bloques"
            className={cn(
              'flex flex-col gap-4',
              enDetalle && 'hidden lg:flex',
              !seleccionado && 'max-w-xl',
            )}
          >
            <div className="flex flex-col gap-1">
              <h2
                ref={tituloLista}
                id="titulo-bloques"
                tabIndex={-1}
                className="m-0 text-h3 focus:outline-none focus-visible:outline-3"
              >
                Tus tablas
              </h2>
              {ordenados.length > 0 && (
                <p className="m-0 text-hint text-soft">
                  Una tabla junta información del mismo tipo.
                </p>
              )}
            </div>
            {ordenados.length === 0 ? (
              <EmptyState
                icon={<SquaresFourIcon aria-hidden size={32} />}
                title="Todavía no hay tablas en este sistema"
              >
                <p className="m-0">
                  Una tabla junta información del mismo tipo, como tus Productos, tus Clientes o tus
                  Pedidos. Empezá agregando la primera.
                </p>
              </EmptyState>
            ) : (
              <ListaBloques
                bloques={ordenados}
                seleccionadoId={seleccionado?.id}
                onSeleccionar={(bloque) => elegir(bloque.id)}
              />
            )}
            <AgregarBloque proyectoId={proyectoId} onCreado={(bloque) => elegir(bloque.id)} />
          </section>

          {seleccionado && (
            <DetalleBloque
              key={seleccionado.id}
              proyectoId={proyectoId}
              bloque={seleccionado}
              enfocar={enfocarDetalle}
              onVolver={volverALista}
              onEliminado={alEliminar}
              className={cn(!enDetalle && 'hidden lg:flex')}
            />
          )}
        </div>
      )}
    </div>
  );
}
