import { Link, useParams } from '@tanstack/react-router';
import { CaretRightIcon, PuzzlePieceIcon } from '@phosphor-icons/react';
import { UltimasFilas } from '@/components/Bloques/UltimasFilas';
import { FormularioRegistro } from '@/components/FormRenderer/FormularioRegistro';
import {
  EmptyState,
  ErrorState,
  PageHeading,
  PageSpinner,
  Panel,
  buttonClasses,
} from '@/components/ui';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';

export function BloquePage() {
  const { bloqueId } = useParams({ from: '/_app/bloques/$bloqueId' });
  const negocio = useMiNegocio();

  if (negocio.cargando) return <PageSpinner />;
  if (negocio.error) {
    return <ErrorState message="No pudimos traer este bloque. Probá de nuevo en un momento." />;
  }

  const sistema = negocio.sistema;
  const bloque = sistema?.bloques.find((item) => item.id === bloqueId);
  if (!sistema || !bloque) {
    return (
      <div className="flex flex-col gap-6">
        <EmptyState
          icon={<PuzzlePieceIcon aria-hidden size={32} />}
          title="No encontramos este bloque"
        >
          <p className="m-0">Puede que ya no esté en tu sistema.</p>
          <Link to="/bloques" className={buttonClasses({ variant: 'solid', className: 'mt-3' })}>
            Volver a Mis bloques
          </Link>
        </EmptyState>
      </div>
    );
  }

  const pieza = [...negocio.presentes.values()].find((item) => item.bloque.id === bloque.id)?.pieza;

  return (
    <div className="flex flex-col gap-8">
      <nav aria-label="Dónde estás">
        <ol className="m-0 flex list-none flex-wrap items-center gap-1 p-0 text-body text-soft">
          <li>
            <Link to="/bloques" className="font-semibold text-accent-800">
              Mis bloques
            </Link>
          </li>
          <li aria-hidden className="flex">
            <CaretRightIcon size={16} />
          </li>
          <li aria-current="page" className="font-semibold text-fg">
            {bloque.nombre}
          </li>
        </ol>
      </nav>

      <PageHeading kicker="Bloque" title={bloque.nombre} lead={pieza?.descripcion} />

      <Panel className="max-w-2xl" aria-labelledby="agregar-a-bloque">
        <h2 id="agregar-a-bloque" className="mt-0 mb-5 text-h3">
          Agregar a {bloque.nombre}
        </h2>
        <FormularioRegistro bloque={bloque} tablas={sistema.bloques} />
      </Panel>

      <section aria-labelledby="ultimas-filas" className="flex flex-col gap-4">
        <h2 id="ultimas-filas" className="m-0 text-h3">
          Lo último que cargaste
        </h2>
        <UltimasFilas bloque={bloque} />
        <div className="flex flex-col items-start gap-2">
          <Link
            to="/proyectos/$proyectoId/usar"
            params={{ proyectoId: sistema.id }}
            search={{ bloque: bloque.id }}
            className={buttonClasses({ variant: 'primary' })}
          >
            Ver todo
          </Link>
          <p className="m-0 text-hint text-soft">
            Ahí está la tabla completa, con búsqueda y orden.
          </p>
        </div>
      </section>
    </div>
  );
}
