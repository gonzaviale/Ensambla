import type { Plan } from '@/types/planes';

export function precioDelPlan(plan: Plan) {
  if (plan.precioMensual === 0) return '$0';
  if (!plan.precioConfigurado || plan.precioMensual === null || !plan.moneda) {
    return 'Precio por definir';
  }
  return conMoneda(plan.precioMensual, plan.moneda);
}

export function detalleDelPrecio(plan: Plan) {
  if (plan.precioMensual === 0) return 'Por mes · sin vencimiento';
  if (!plan.precioConfigurado) return 'La contratación estará disponible próximamente.';
  return 'Cada 30 días · renovación automática';
}

function conMoneda(importe: number, moneda: string) {
  const texto = importe.toLocaleString('es-AR', { maximumFractionDigits: 0 });
  return moneda === 'ARS' ? `$${texto}` : `${texto} ${moneda}`;
}
