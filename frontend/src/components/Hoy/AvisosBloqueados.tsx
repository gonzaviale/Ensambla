import { Link } from '@tanstack/react-router';
import { LockSimpleIcon } from '@phosphor-icons/react';
import { Panel, buttonClasses } from '@/components/ui';
import { usePlanActual } from '@/hooks/red/useRed';
import { pesos, plural } from '@/lib/red/formato';
import { FUNCION_DE_ALERTA, ORDEN_PLANES, PLANES, planMinimoPara } from '@/lib/red/planes';
import type { Alerta } from '@/types/red';
import { META_AVISO, ORDEN_TIPOS } from './avisoMeta';

const TITULOS_VISIBLES = 4;

// Venta honesta del plan: lo que se está perdiendo hoy, con los títulos a la vista y sin la acción.
// No es una pared: el resto de la pantalla sigue funcionando.
export function AvisosBloqueados({ bloqueadas }: { bloqueadas: Alerta[] }) {
  const plan = usePlanActual();
  if (bloqueadas.length === 0) return null;

  const impacto = bloqueadas.reduce((suma, alerta) => suma + alerta.impacto, 0);
  const tipos = ORDEN_TIPOS.filter((tipo) => bloqueadas.some((alerta) => alerta.tipo === tipo));
  const planNecesario = tipos
    .map((tipo) => planMinimoPara(FUNCION_DE_ALERTA[tipo]))
    .reduce((mayor, actual) =>
      ORDEN_PLANES.indexOf(actual) > ORDEN_PLANES.indexOf(mayor) ? actual : mayor,
    );
  const visibles = bloqueadas.slice(0, TITULOS_VISIBLES);
  const resto = bloqueadas.length - visibles.length;
  const puedeProbar = !plan.suscripcion.pruebaUsada;

  return (
    <Panel
      aria-labelledby="hoy-bloqueados"
      className="flex flex-col gap-4 border-amber/60 bg-amber-100/40"
    >
      <div className="flex items-start gap-3">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-amber-100 text-amber-700">
          <LockSimpleIcon aria-hidden size={26} weight="bold" />
        </span>
        <div className="flex min-w-0 flex-col gap-1">
          <h2 id="hoy-bloqueados" className="m-0 text-h3">
            {bloqueadas.length === 1
              ? 'Hay 1 aviso que no estás viendo'
              : `Hay ${bloqueadas.length} avisos que no estás viendo`}
          </h2>
          <p className="m-0 text-body text-soft">
            {impacto > 0 ? (
              <>
                Hay <strong className="text-fg">≈ {pesos(impacto)} por mes</strong> en juego.{' '}
              </>
            ) : null}
            Son avisos de: {tipos.map((tipo) => META_AVISO[tipo].etiqueta).join(', ')}.
          </p>
        </div>
      </div>

      <ul className="m-0 flex list-none flex-col gap-2 p-0">
        {visibles.map((alerta) => (
          <li
            key={alerta.id}
            className="rounded-md border border-amber/40 bg-surface px-4 py-3 text-body text-soft"
          >
            {alerta.titulo}
          </li>
        ))}
        {resto > 0 && (
          <li className="px-1 text-hint text-muted">
            y {plural(resto, 'aviso más', 'avisos más')}
          </li>
        )}
      </ul>

      <div className="flex flex-wrap items-center gap-3">
        <Link to="/plan" className={buttonClasses({ variant: 'primary' })}>
          {puedeProbar ? 'Probar 7 días gratis' : `Ver el plan ${PLANES[planNecesario].nombre}`}
        </Link>
        <p className="m-0 text-hint text-muted">
          Tus datos no se pierden: los avisos se activan cuando tengas el plan{' '}
          {PLANES[planNecesario].nombre}.
        </p>
      </div>
    </Panel>
  );
}
