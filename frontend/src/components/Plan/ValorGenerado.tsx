import type { ReactNode } from 'react';
import { ClockIcon, ListChecksIcon, PiggyBankIcon, TagIcon } from '@phosphor-icons/react';
import { Panel } from '@/components/ui';
import { usePlanActual, useValor } from '@/hooks/red/useRed';
import { pesos, porcentaje } from '@/lib/red/formato';
import { PLANES, ORDEN_PLANES } from '@/lib/red/planes';
import type { PlanId } from '@/types/red';

/** «1 h 10 min» / «45 min». */
function duracion(minutos: number): string {
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  if (horas === 0) return `${resto} min`;
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`;
}

// El argumento central: lo que Ensambla ya le ahorró a este comercio, comparado con lo que cuesta
// el plan. Sólo dice «se paga solo» si la cuenta da; no inventa ahorro.
export function ValorGenerado() {
  const valor = useValor();
  const plan = usePlanActual();

  const paga = plan.suscripcion.estado === 'activa' || plan.estado === 'cancelada';
  // Contra qué plan se compara: el que ya paga, o el más completo que el valor alcanza a cubrir.
  const referencia: PlanId = paga
    ? plan.plan
    : ([...ORDEN_PLANES]
        .reverse()
        .find((id) => id !== 'gratis' && PLANES[id].precioMensual <= valor.plataProtegidaMes) ??
      'comercio');
  const precio = PLANES[referencia].precioMensual;
  const sePagaSolo = valor.plataProtegidaMes > 0 && precio > 0 && valor.plataProtegidaMes >= precio;

  return (
    <Panel
      aria-labelledby="plan-valor"
      className="flex flex-col gap-4 border-accent-300 bg-accent-100"
    >
      <h2 id="plan-valor" className="m-0 text-h3">
        Lo que Ensambla ya hizo por vos
      </h2>

      {valor.plataProtegidaMes > 0 ? (
        <div className="flex flex-col gap-2">
          <p className="m-0 text-lead font-bold">
            Este mes Ensambla te ayudó a no perder ≈ {pesos(valor.plataProtegidaMes)}
            {sePagaSolo ? ': tu plan se paga solo.' : '.'}
          </p>
          {precio > 0 &&
            (sePagaSolo ? (
              <p className="m-0 text-body text-soft">
                El plan {PLANES[referencia].nombre} cuesta {pesos(precio)} por mes y vos protegiste
                ≈{' '}
                {(valor.plataProtegidaMes / precio).toLocaleString('es-AR', {
                  maximumFractionDigits: 1,
                })}{' '}
                veces eso.
              </p>
            ) : (
              <p className="m-0 text-body text-soft">
                Eso cubre {porcentaje((valor.plataProtegidaMes / precio) * 100)} del plan{' '}
                {PLANES[referencia].nombre} ({pesos(precio)} por mes). Cuantas más listas cargues,
                más se nota; todavía no alcanza para decir que se paga solo.
              </p>
            ))}
        </div>
      ) : (
        <p className="m-0 text-body text-soft">
          Todavía no aplicaste cambios de precio con Ensambla. Cuando apliques tu primera lista vas
          a ver acá cuánta plata te ayudó a no perder.
        </p>
      )}

      <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-3">
        <Dato
          icono={<TagIcon aria-hidden size={24} />}
          numero={String(valor.preciosCorregidos)}
          texto={valor.preciosCorregidos === 1 ? 'precio corregido' : 'precios corregidos'}
        />
        <Dato
          icono={<ListChecksIcon aria-hidden size={24} />}
          numero={String(valor.listasAplicadas)}
          texto={valor.listasAplicadas === 1 ? 'lista aplicada' : 'listas aplicadas'}
        />
        <Dato
          icono={<ClockIcon aria-hidden size={24} />}
          numero={valor.minutosAhorrados > 0 ? `≈ ${duracion(valor.minutosAhorrados)}` : '0 min'}
          texto="de trabajo ahorrado"
        />
      </ul>

      <p className="m-0 flex items-start gap-2 text-hint text-muted">
        <PiggyBankIcon aria-hidden size={20} className="mt-0.5 shrink-0" />
        Es una estimación con tus propios productos y ventas de la semana: lo que vendés por la
        diferencia de cada precio corregido. No es una promesa.
      </p>
    </Panel>
  );
}

function Dato({ icono, numero, texto }: { icono: ReactNode; numero: string; texto: string }) {
  return (
    <li className="flex items-center gap-3 rounded-lg bg-surface px-4 py-3">
      <span className="text-accent-800">{icono}</span>
      <p className="m-0 flex flex-col leading-tight">
        <span className="text-h3 font-extrabold">{numero}</span>
        <span className="text-hint text-soft">{texto}</span>
      </p>
    </li>
  );
}
