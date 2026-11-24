import { Icon } from '@/components/sistema/Icon';
import { filasDelPlan, type FilaDelPlan } from '@/lib/planes/filasDelPlan';
import type { Plan } from '@/types/planes';

export function FilasDelPlan({ plan }: { plan: Plan }) {
  return (
    <dl className="plan-limits">
      {filasDelPlan(plan).map((fila) => (
        <FilaDelPlanVisible key={fila.titulo} fila={fila} />
      ))}
    </dl>
  );
}

function FilaDelPlanVisible({ fila }: { fila: FilaDelPlan }) {
  return (
    <div data-excluida={fila.excluida ? 'true' : undefined}>
      <dt>
        <span className="plan-limit-icono">
          <Icon name={fila.icono} />
        </span>
        {fila.titulo}
      </dt>
      <dd>{fila.valor}</dd>
    </div>
  );
}
