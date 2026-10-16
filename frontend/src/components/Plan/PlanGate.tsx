import type { ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { LockSimpleIcon } from '@phosphor-icons/react';
import { Panel, buttonClasses } from '@/components/ui';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { NOMBRE_FUNCION, PLANES, planMinimoPara } from '@/lib/planes/planes';
import type { FuncionPlan } from '@/types/negocio';

type RequierePlanProps = {
  funcion: FuncionPlan;
  children: ReactNode;
  /** Qué pierde la persona si no tiene el plan, en una línea y en sus palabras. */
  beneficio?: string;
};

// Funciones por plan (T-067): si el plan actual no incluye la función, en lugar del contenido se
// muestra una invitación clara a mejorar el plan. Nunca una pantalla vacía ni un error.
export function RequierePlan({ funcion, children, beneficio }: RequierePlanProps) {
  const plan = usePlanActual();
  if (plan.incluye(funcion)) return <>{children}</>;
  return <CandadoPlan funcion={funcion} beneficio={beneficio} />;
}

export function CandadoPlan({ funcion, beneficio }: { funcion: FuncionPlan; beneficio?: string }) {
  const plan = usePlanActual();
  const minimo = PLANES[planMinimoPara(funcion)];
  const puedeProbar = !plan.suscripcion.pruebaUsada;

  return (
    <Panel flat className="flex flex-col items-start gap-3" aria-labelledby={`candado-${funcion}`}>
      <span className="flex size-12 items-center justify-center rounded-full bg-amber-100 text-amber-700">
        <LockSimpleIcon aria-hidden size={26} weight="bold" />
      </span>
      <h2 id={`candado-${funcion}`} className="m-0 text-h3">
        {NOMBRE_FUNCION[funcion]}
      </h2>
      <p className="m-0 text-body text-soft">
        {beneficio ?? 'Esto es parte de un plan pago.'} Está incluido en el plan{' '}
        <strong>{minimo.nombre}</strong>.
      </p>
      <Link to="/plan" className={buttonClasses({ variant: 'solid' })}>
        {puedeProbar ? 'Probar 7 días gratis' : 'Ver planes'}
      </Link>
    </Panel>
  );
}

/** Aviso corto de «esto es del plan X» para usar al lado de un botón o un título. */
export function EtiquetaPlan({ funcion }: { funcion: FuncionPlan }) {
  const minimo = PLANES[planMinimoPara(funcion)];
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-100 px-3 py-1 text-caption font-semibold text-amber-700">
      <LockSimpleIcon aria-hidden size={14} weight="bold" />
      Plan {minimo.nombre}
    </span>
  );
}
