import { Link, useRouterState } from '@tanstack/react-router';
import { SparkleIcon, WarningCircleIcon } from '@phosphor-icons/react';
import { buttonClasses } from '@/components/ui';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { PLANES } from '@/lib/planes/planes';

// Franja bajo la barra superior: cuántos días quedan de la prueba o que la prueba terminó. No
// aparece con un plan activo ni en la propia pantalla del plan.
export function BannerPlan() {
  const plan = usePlanActual();
  const enPlan = useRouterState({ select: (s) => s.location.pathname === '/plan' });
  if (enPlan) return null;

  if (plan.enPrueba) {
    return (
      <aside
        aria-label="Estado de tu prueba"
        className="mb-4 flex flex-wrap items-center gap-3 rounded-lg bg-amber-100 px-4 py-3 text-amber-700"
      >
        <SparkleIcon aria-hidden size={24} weight="fill" className="shrink-0" />
        <p className="m-0 min-w-0 flex-1 basis-60 text-body font-semibold">
          Estás probando {PLANES['negocio-plus'].nombre}: te{' '}
          {plan.diasDePrueba === 1 ? 'queda' : 'quedan'} {plan.diasDePrueba}{' '}
          {plan.diasDePrueba === 1 ? 'día' : 'días'}.
        </p>
        <Link to="/plan" className={buttonClasses({ variant: 'primary', size: 'compact' })}>
          Elegir mi plan
        </Link>
      </aside>
    );
  }
  if (plan.estado === 'vencida') {
    return (
      <aside
        aria-label="Estado de tu plan"
        className="mb-4 flex flex-wrap items-center gap-3 rounded-lg bg-coral-100 px-4 py-3 text-bad"
      >
        <WarningCircleIcon aria-hidden size={24} weight="fill" className="shrink-0" />
        <p className="m-0 min-w-0 flex-1 basis-60 text-body font-semibold">
          Terminó tu prueba. Seguís con el plan Gratis: algunas respuestas quedan bloqueadas.
        </p>
        <Link to="/plan" className={buttonClasses({ variant: 'solid', size: 'compact' })}>
          Ver planes
        </Link>
      </aside>
    );
  }
  return null;
}
