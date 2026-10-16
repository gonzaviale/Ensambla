import { Link } from '@tanstack/react-router';
import { PuzzlePieceIcon } from '@phosphor-icons/react';
import { rubroDelSistema } from '@/components/Bloques/rubro';
import { SumarBloque } from '@/components/Bloques/SumarBloque';
import { TarjetaBloque } from '@/components/Bloques/TarjetaBloque';
import { EmptyState, ErrorState, PageHeading, PageSpinner, buttonClasses } from '@/components/ui';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';

export function BloquesPage() {
  const negocio = useMiNegocio();

  if (negocio.cargando) return <PageSpinner />;
  if (negocio.error) {
    return <ErrorState message="No pudimos traer tus bloques. Probá de nuevo en un momento." />;
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Mis bloques"
        title="Tu sistema, bloque a bloque"
        lead="Cada bloque desbloquea respuestas."
      />

      {!negocio.sistema ? (
        <EmptyState
          icon={<PuzzlePieceIcon aria-hidden size={32} />}
          title="Todavía no armaste tu sistema"
        >
          <p className="m-0">Contanos qué hacés y Ensambla te arma los bloques.</p>
          <Link to="/bienvenida" className={buttonClasses({ variant: 'solid', className: 'mt-3' })}>
            Armar mi sistema
          </Link>
        </EmptyState>
      ) : (
        <>
          <section
            aria-labelledby="tus-bloques"
            data-recorrido="bloques-tarjetas"
            className="flex flex-col gap-4"
          >
            <h2 id="tus-bloques" className="m-0 text-h3">
              Tus bloques
            </h2>
            {negocio.presentes.size === 0 ? (
              <EmptyState
                icon={<PuzzlePieceIcon aria-hidden size={32} />}
                title="Todavía no tenés bloques"
              >
                <p className="m-0">Sumá el primero desde la lista de abajo.</p>
              </EmptyState>
            ) : (
              <ul className="m-0 grid list-none grid-cols-1 gap-4 p-0 md:grid-cols-2 xl:grid-cols-3">
                {[...negocio.presentes.values()].map(({ pieza, bloque }) => (
                  <TarjetaBloque key={bloque.id} bloque={bloque} pieza={pieza} />
                ))}
              </ul>
            )}
          </section>

          <SumarBloque
            sistemaId={negocio.sistema.id}
            cantidadDeBloques={negocio.sistema.bloques.length}
            piezasPresentes={negocio.piezasPresentes}
            rubro={rubroDelSistema(negocio.sistema.bloques)}
          />
        </>
      )}
    </div>
  );
}
