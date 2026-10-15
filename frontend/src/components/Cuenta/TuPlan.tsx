import { Link } from '@tanstack/react-router';
import { Badge, Panel, buttonClasses } from '@/components/ui';
import { usePlanActual } from '@/hooks/red/useRed';
import { fechaCorta, pesos, plural } from '@/lib/red/formato';
import { precioPorMes } from '@/lib/red/planes';

// Resumen del plan con el link a la pantalla de planes (la gestión vive en /plan).
export function TuPlan() {
  const plan = usePlanActual();
  const { definicion, suscripcion } = plan;

  let detalle: string;
  if (plan.enPrueba) {
    detalle = `Estás en la prueba gratis. Te quedan ${plural(plan.diasDePrueba, 'día', 'días')} con todas las funciones.`;
  } else if (plan.estado === 'activa') {
    detalle = `Pagás ${pesos(precioPorMes(definicion.id, suscripcion.periodo))} por mes.`;
  } else if (plan.estado === 'cancelada' && suscripcion.activoHasta) {
    detalle = `Cancelaste el plan: sigue activo hasta el ${fechaCorta(suscripcion.activoHasta)}.`;
  } else if (definicion.listasPorMes !== null) {
    detalle = `Usaste ${plan.listasUsadasMes} de ${plural(definicion.listasPorMes, 'lista', 'listas')} de este mes.`;
  } else {
    detalle = 'Tenés todas las listas que necesites.';
  }

  return (
    <Panel aria-labelledby="cuenta-plan" className="flex flex-col gap-3">
      <h2 id="cuenta-plan" className="m-0 text-h3">
        Tu plan
      </h2>
      <p className="m-0 flex flex-wrap items-center gap-2 text-body">
        <strong>{definicion.nombre}</strong>
        {plan.enPrueba && <Badge tone="accent">Prueba gratis</Badge>}
      </p>
      <p className="m-0 text-body text-soft">{detalle}</p>
      <Link to="/plan" className={buttonClasses({ variant: 'primary', className: 'self-start' })}>
        Ver o cambiar mi plan
      </Link>
    </Panel>
  );
}
