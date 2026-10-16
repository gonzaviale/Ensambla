import { Link } from '@tanstack/react-router';
import { SparkleIcon } from '@phosphor-icons/react';
import { ContarQuePaso } from '@/components/Subir/ContarQuePaso';
import { TraeTuHistoria } from '@/components/Subir/TraeTuHistoria';
import { EmptyState, ErrorState, PageHeading, PageSpinner, buttonClasses } from '@/components/ui';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';

// «Sumá datos»: lo principal es contar qué pasó; la foto del cuaderno es un atajo opcional.
export function SubirPage() {
  const negocio = useMiNegocio();

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Sumar datos"
        title="Contanos qué pasó"
        lead="Escribilo como te salga. Ensambla lo ordena y vos confirmás."
      />
      {negocio.cargando ? (
        <PageSpinner />
      ) : negocio.error ? (
        <ErrorState message="No pudimos leer tu negocio. Probá de nuevo en un rato." />
      ) : !negocio.hayNegocio ? (
        <EmptyState icon={<SparkleIcon aria-hidden size={32} />} title="Primero armá tu negocio">
          <p className="m-0">
            Contanos qué hacés y Ensambla arma las piezas para guardar tus ventas, gastos y fiados.
          </p>
          <Link to="/bienvenida" className={`${buttonClasses({ variant: 'solid' })} mt-4`}>
            Armar mi negocio
          </Link>
        </EmptyState>
      ) : (
        <>
          <ContarQuePaso />
          <TraeTuHistoria />
        </>
      )}
    </div>
  );
}
