import { etiquetaDelPlan } from '@/lib/planes/etiquetaDelPlan';
import { presentacionDelPlan } from '@/lib/planes/presentacionDelPlan';
import type { Plan } from '@/types/planes';
import { EncabezadoDelPlan } from './EncabezadoDelPlan';
import { FilasDelPlan } from './FilasDelPlan';
import { PieDelPlanPremium } from './PieDelPlanPremium';
import { PrecioDelPlan } from './PrecioDelPlan';

type TarjetaPlanProps = {
  plan: Plan;
  actual: boolean;
  pruebaActiva: boolean;
  puedeIniciar: boolean;
  activando: boolean;
  pruebaUtilizada: boolean;
  onIniciar: () => void;
};

export function TarjetaPlan({ plan, onIniciar, ...situacion }: TarjetaPlanProps) {
  const { color, destacado } = presentacionDelPlan(plan);
  return (
    <article
      className={`plan-card ${destacado ? 'plan-card-premium' : ''}`}
      data-color={color}
      data-actual={situacion.actual ? 'true' : undefined}
      aria-label={`Plan ${plan.nombre}`}
    >
      <EncabezadoDelPlan plan={plan} etiqueta={etiquetaDelPlan({ plan, ...situacion })} />
      <PrecioDelPlan plan={plan} />
      <FilasDelPlan plan={plan} />
      {destacado ? <PieDelPlanPremium {...situacion} onIniciar={onIniciar} /> : null}
    </article>
  );
}
