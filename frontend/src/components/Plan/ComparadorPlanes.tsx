import { useState } from 'react';
import { InfoIcon } from '@phosphor-icons/react';
import { InfoTip } from '@/components/ui';
import { useAccionesRed, usePlanActual } from '@/hooks/red/useRed';
import { fechaLarga } from '@/lib/red/formato';
import { DIAS_DE_PRUEBA, ORDEN_PLANES, PLANES } from '@/lib/red/planes';
import type { PeriodoPlan, PlanId } from '@/types/red';
import { SelectorPeriodo } from './SelectorPeriodo';
import { TablaComparativa } from './TablaComparativa';
import { TarjetaPlan, type AccionTarjeta } from './TarjetaPlan';

type ComparadorProps = {
  /** Abre el flujo de pago del plan elegido, con el período que se estaba mirando. */
  onElegir: (plan: PlanId, periodo: PeriodoPlan) => void;
  /** Pasar al plan Gratis = cancelar (pide confirmación en la pantalla). */
  onPasarAGratis: () => void;
  onAviso: (mensaje: string) => void;
};

// Comparar planes: tarjetas con una sola acción cada una, selector mensual/anual y, en escritorio,
// la tabla función por función.
export function ComparadorPlanes({ onElegir, onPasarAGratis, onAviso }: ComparadorProps) {
  const plan = usePlanActual();
  const { iniciarPrueba } = useAccionesRed();
  const [periodo, setPeriodo] = useState<PeriodoPlan>(plan.suscripcion.periodo);

  const puedeProbar = !plan.suscripcion.pruebaUsada && plan.plan === 'gratis' && !plan.enPrueba;
  const cancelada = plan.estado === 'cancelada';

  const accionDe = (id: PlanId): AccionTarjeta => {
    const definicion = PLANES[id];
    const esActual = !plan.enPrueba && plan.plan === id;
    const principal = definicion.destacado && !cancelada;

    if (esActual) return { etiqueta: 'Tu plan actual' };

    if (id === 'gratis') {
      if (cancelada && plan.suscripcion.activoHasta) {
        return { etiqueta: `Pasás a Gratis el ${fechaLarga(plan.suscripcion.activoHasta)}` };
      }
      return {
        etiqueta: 'Pasar a Gratis',
        onClick: onPasarAGratis,
        nota: 'No borramos tus datos: sólo se bloquean las funciones del plan pago.',
      };
    }

    if (puedeProbar) {
      return {
        etiqueta: 'Probar 7 días gratis',
        principal,
        nota: `La prueba de ${DIAS_DE_PRUEBA} días incluye todo ${PLANES.red.nombre}. No hace falta cargar un medio de pago.`,
        onClick: () => {
          iniciarPrueba();
          onAviso(`Empezó tu prueba de ${DIAS_DE_PRUEBA} días con todas las funciones.`);
        },
      };
    }

    const subiendo =
      plan.suscripcion.estado === 'activa' &&
      ORDEN_PLANES.indexOf(id) > ORDEN_PLANES.indexOf(plan.plan);
    const bajando =
      plan.suscripcion.estado === 'activa' &&
      ORDEN_PLANES.indexOf(id) < ORDEN_PLANES.indexOf(plan.plan);
    const etiqueta = subiendo
      ? `Subir a ${definicion.nombre}`
      : bajando
        ? `Bajar a ${definicion.nombre}`
        : `Elegir ${definicion.nombre}`;
    return {
      etiqueta,
      principal,
      onClick: () => onElegir(id, periodo),
      nota: plan.enPrueba && id === 'red' ? 'Estás probando este plan.' : undefined,
    };
  };

  return (
    <section
      aria-labelledby="plan-comparar"
      data-recorrido="plan-comparar"
      className="flex flex-col gap-5"
    >
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <div className="flex items-center gap-1">
            <h2 id="plan-comparar" className="m-0 text-h3">
              Elegí el plan que te sirve
            </h2>
            <InfoTip tema="qué pasa al volver al plan Gratis">
              No perdés nada de lo que cargaste. Sólo se bloquean las funciones pagas, que quedan
              con un candado, y tenés 2 listas por mes. Cuando quieras, las volvés a activar.
            </InfoTip>
          </div>
          <p className="m-0 flex items-start gap-1.5 text-hint text-muted">
            <InfoIcon aria-hidden size={18} className="mt-0.5 shrink-0" />
            Los precios son ilustrativos: los estamos validando con comercios.
          </p>
        </div>
        <SelectorPeriodo value={periodo} onChange={setPeriodo} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        {ORDEN_PLANES.map((id) => (
          <TarjetaPlan
            key={id}
            plan={PLANES[id]}
            periodo={periodo}
            accion={accionDe(id)}
            esActual={!plan.enPrueba && plan.plan === id}
          />
        ))}
      </div>

      <TablaComparativa periodo={periodo} />
    </section>
  );
}
