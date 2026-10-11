import { useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearch } from '@tanstack/react-router';
import { SquaresFourIcon, TableIcon } from '@phosphor-icons/react';
import { AgregarBloque } from '@/components/Constructor/AgregarBloque';
import { DetalleBloque } from '@/components/Constructor/DetalleBloque';
import { ListaBloques } from '@/components/Constructor/ListaBloques';
import { EmptyState, ErrorState, PageHeading, Spinner, buttonClasses } from '@/components/ui';
import { useBloques } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { useProyecto } from '@/hooks/proyectos/queries';
import { cn } from '@/lib/cn';

const RUTA = '/_app/proyectos/$proyectoId/construir';

// T-019 a T-021: constructor de bloques y datos (campos).
// Escritorio: lista de bloques a la izquierda y el bloque elegido a la derecha.
// Celular: primero la lista; al tocar un bloque se ve sólo su detalle, con "Ver todos los bloques"
// (y el botón atrás del teléfono) para volver. El bloque elegido va en la URL (?bloque=).
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
      {/* Volver a Mis proyectos lo resuelven los breadcrumbs del layout (ENS-61). */}
      <Link
        to="/proyectos/$proyectoId/usar"
        params={{ proyectoId }}
        className={buttonClasses({ variant: 'secondary', className: 'self-end' })}
      >
        <TableIcon aria-hidden size={18} />
        Ver mis registros
      </Link>

      <PageHeading
        kicker="Modo construir"
        title={proyecto.nombre}
        lead="Elegí cómo se ordena tu información: bloques, como Productos o Clientes, y los datos que guarda cada uno."
      />

      {bloques.isPending ? (
        <Spinner label="Cargando los bloques…" />
      ) : bloques.isError ? (
        <ErrorState
          title="No pudimos cargar los bloques"
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
            <h2
              ref={tituloLista}
              id="titulo-bloques"
              tabIndex={-1}
              className="m-0 text-xl focus:outline-none focus-visible:outline-2"
            >
              Tus bloques
            </h2>
            {ordenados.length === 0 ? (
              <EmptyState
                icon={<SquaresFourIcon aria-hidden size={32} />}
                title="Todavía no hay bloques en este proyecto"
              >
                <p className="m-0 text-hint">
                  Un bloque junta información del mismo tipo, como tus Productos, tus Clientes o tus
                  Pedidos. Empezá agregando el primero.
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
