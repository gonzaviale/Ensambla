import { useCatalogoPlanes } from '@/hooks/planes/queries';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { TarjetaPlan } from '@/components/planes/TarjetaPlan';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';

export function PlanesPublicos() {
  useTituloPagina('Planes');
  const consulta = useCatalogoPlanes();
  return (
    <>
      <main id="main" tabIndex={-1} className="plans-page reveal">
        <header className="plans-heading">
          <div>
            <span className="eyebrow">A TU RITMO</span>
            <h1>
              Tu negocio tiene lugar.
              <br />
              Elegí cómo hacerlo crecer.
            </h1>
            <p>Empezá con lo esencial. Sumá herramientas cuando las necesites.</p>
          </div>
          <span className="plans-heading-piece" aria-hidden="true">
            <Icon name="spark" />
          </span>
        </header>
        <div className="plans-section-heading">
          <h2>Qué incluye cada plan</h2>
          <p>Modalidad mensual · Los límites son para todo tu negocio.</p>
        </div>
        {consulta.isPending ? (
          <p role="status" className="plan-loading">
            Estamos consultando los planes…
          </p>
        ) : null}
        {consulta.isError ? (
          <div role="alert" className="plan-error">
            <p>No pudimos consultar los planes. Probá nuevamente.</p>
            <Btn
              cls="secondary"
              disabled={consulta.isFetching}
              onClick={() => void consulta.refetch()}
            >
              Reintentar
            </Btn>
          </div>
        ) : null}
        {consulta.data ? (
          <section className="plans-grid" aria-label="Comparación de planes">
            {consulta.data.map((plan) => (
              <TarjetaPlan key={plan.codigo} plan={plan} publica />
            ))}
          </section>
        ) : null}
        <section className="plans-reassurance" aria-labelledby="plans-data-title">
          <Icon name="check" />
          <div>
            <h2 id="plans-data-title">Empezar es gratis.</h2>
            <p>
              Creá tu cuenta y organizá tu negocio. Si elegís probar Premium, conservás tus datos al
              terminar.
            </p>
          </div>
        </section>
      </main>
      <PageFooter />
    </>
  );
}
