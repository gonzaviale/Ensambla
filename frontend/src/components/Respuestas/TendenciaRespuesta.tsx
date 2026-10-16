import { ArrowDownIcon, ArrowUpIcon } from '@phosphor-icons/react';
import { Badge, InfoTip } from '@/components/ui';
import { EtiquetaPlan } from '@/components/Plan/PlanGate';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { porcentaje } from '@/lib/formato';

type Tendencia = { porcentaje: number; mejor: boolean };

// Variación contra el mes anterior: ícono + texto + tono (nunca sólo color). Es del plan Negocio:
// en el plan Gratis queda la etiqueta que invita a mejorar.
export function TendenciaRespuesta({ tendencia }: { tendencia?: Tendencia }) {
  const plan = usePlanActual();
  if (!tendencia) return null;
  if (!plan.incluye('comparativas')) {
    return (
      <span>
        <EtiquetaPlan funcion="comparativas" />
      </span>
    );
  }
  const sube = tendencia.porcentaje > 0;
  const Flecha = sube ? ArrowUpIcon : ArrowDownIcon;
  return (
    <span className="inline-flex flex-wrap items-center gap-x-1">
      <Badge
        tone={tendencia.mejor ? 'ok' : 'warn'}
        icon={<Flecha size={18} weight="bold" />}
        className="text-hint"
      >
        {sube ? 'Subió' : 'Bajó'} {porcentaje(Math.abs(tendencia.porcentaje))} vs. el mes anterior ·{' '}
        {tendencia.mejor ? 'va bien' : 'para mirar'}
      </Badge>
      <InfoTip tema="comparado con el mes anterior">
        Comparamos lo que va de este mes con el mes anterior completo. Por ejemplo: si el mes pasado
        vendiste $ 100.000 y este mes vas por $ 112.000, sube 12 %.
      </InfoTip>
    </span>
  );
}
