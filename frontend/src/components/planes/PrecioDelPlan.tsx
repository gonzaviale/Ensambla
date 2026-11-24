import { detalleDelPrecio, precioDelPlan } from '@/lib/planes/precioDelPlan';
import type { Plan } from '@/types/planes';

export function PrecioDelPlan({ plan }: { plan: Plan }) {
  return (
    <>
      <p className="plan-price">{precioDelPlan(plan)}</p>
      <p className="plan-price-detail">{detalleDelPrecio(plan)}</p>
    </>
  );
}
