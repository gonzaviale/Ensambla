import { Icon } from '@/components/sistema/Icon';
import type { EtiquetaDelPlan } from '@/lib/planes/etiquetaDelPlan';
import { presentacionDelPlan } from '@/lib/planes/presentacionDelPlan';
import type { Plan } from '@/types/planes';

type EncabezadoDelPlanProps = {
  plan: Plan;
  etiqueta: EtiquetaDelPlan | null;
};

export function EncabezadoDelPlan({ plan, etiqueta }: EncabezadoDelPlanProps) {
  const { icono, descripcion } = presentacionDelPlan(plan);
  return (
    <>
      <div className="plan-card-top">
        <span className="plan-card-icon">
          <Icon name={icono} />
        </span>
        <Etiqueta etiqueta={etiqueta} />
      </div>
      <h2>{plan.nombre}</h2>
      <p className="plan-card-description">{descripcion}</p>
    </>
  );
}

function Etiqueta({ etiqueta }: { etiqueta: EtiquetaDelPlan | null }) {
  if (!etiqueta) return null;
  return <span className={etiqueta.clase}>{etiqueta.texto}</span>;
}
