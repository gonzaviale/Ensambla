import { CheckIcon, LockSimpleIcon, StarIcon } from '@phosphor-icons/react';
import { Badge, Button } from '@/components/ui';
import { pesos } from '@/lib/red/formato';
import {
  NOMBRE_FUNCION,
  PLANES,
  importeACobrar,
  planIncluye,
  precioPorMes,
  MESES_PAGOS_ANUAL,
  type DefinicionPlan,
} from '@/lib/red/planes';
import { cn } from '@/lib/cn';
import type { PeriodoPlan } from '@/types/red';

/** Todas las funciones que existen en algún plan, en el orden del plan más completo. */
const TODAS_LAS_FUNCIONES = PLANES.red.funciones;

export type AccionTarjeta = {
  etiqueta: string;
  /** Sin `onClick` el botón queda deshabilitado y el texto explica por qué. */
  onClick?: () => void;
  /** Aclaración corta debajo del botón. */
  nota?: string;
  principal?: boolean;
};

type TarjetaPlanProps = {
  plan: DefinicionPlan;
  periodo: PeriodoPlan;
  accion: AccionTarjeta;
  /** Es el plan que rige hoy (queda marcado con ícono + texto). */
  esActual?: boolean;
};

function limitesDe(plan: DefinicionPlan): string[] {
  return [
    plan.listasPorMes === null
      ? 'Listas de proveedores sin límite'
      : `${plan.listasPorMes} listas por mes`,
    plan.productos === null
      ? 'Productos sin límite'
      : `Hasta ${plan.productos.toLocaleString('es-AR')} productos`,
    plan.usuarios === 1 ? '1 persona con acceso' : `${plan.usuarios} personas con acceso`,
  ];
}

export function TarjetaPlan({ plan, periodo, accion, esActual }: TarjetaPlanProps) {
  const titulo = `plan-${plan.id}`;
  const gratis = plan.precioMensual === 0;
  const anual = periodo === 'anual' && !gratis;

  return (
    <article
      aria-labelledby={titulo}
      className={cn(
        'flex min-w-0 flex-col gap-4 rounded-lg border bg-surface p-5 shadow-sm',
        plan.destacado ? 'border-2 border-accent-700 shadow-md' : 'border-line/60',
      )}
    >
      <header className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <h3 id={titulo} className="m-0 text-h3">
            {plan.nombre}
          </h3>
          {plan.destacado && (
            <Badge tone="accent" icon={<StarIcon size={14} weight="fill" />}>
              El más completo
            </Badge>
          )}
          {esActual && (
            <Badge tone="ok" icon={<CheckIcon size={14} weight="bold" />}>
              Tu plan
            </Badge>
          )}
        </div>
        <p className="m-0 text-body text-soft">{plan.bajada}</p>
      </header>

      <div className="flex flex-col gap-1">
        <p className="m-0 flex flex-wrap items-baseline gap-x-2">
          <span className="text-h2 font-extrabold">{pesos(precioPorMes(plan.id, periodo))}</span>
          <span className="text-hint text-muted">{gratis ? 'siempre' : 'por mes'}</span>
        </p>
        {anual && (
          <p className="m-0 text-hint text-muted">
            Pagás {pesos(importeACobrar(plan.id, periodo))} por año: {MESES_PAGOS_ANUAL} meses, usás
            12.
          </p>
        )}
      </div>

      <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-body text-fg">
        {limitesDe(plan).map((limite) => (
          <li key={limite} className="flex items-start gap-2">
            <CheckIcon aria-hidden size={22} weight="bold" className="mt-0.5 shrink-0 text-ok" />
            {limite}
          </li>
        ))}
      </ul>

      {/* En escritorio la comparación completa está en la tabla de abajo. */}
      <ul
        aria-label={`Funciones del plan ${plan.nombre}`}
        className="m-0 flex list-none flex-col gap-1.5 border-t border-line p-0 pt-4 text-hint lg:hidden"
      >
        {TODAS_LAS_FUNCIONES.map((funcion) => {
          const incluida = planIncluye(plan.id, funcion);
          return (
            <li
              key={funcion}
              className={cn('flex items-start gap-2', incluida ? 'text-fg' : 'text-muted')}
            >
              {incluida ? (
                <CheckIcon
                  aria-hidden
                  size={20}
                  weight="bold"
                  className="mt-0.5 shrink-0 text-ok"
                />
              ) : (
                <LockSimpleIcon aria-hidden size={20} className="mt-0.5 shrink-0" />
              )}
              <span>
                {NOMBRE_FUNCION[funcion]}
                <span className="sr-only">{incluida ? ': incluido' : ': no incluido'}</span>
              </span>
            </li>
          );
        })}
      </ul>

      <div className="mt-auto flex flex-col gap-2 pt-1">
        <Button
          variant={accion.principal ? 'solid' : 'secondary'}
          fullWidth
          disabled={!accion.onClick}
          onClick={accion.onClick}
        >
          {accion.etiqueta}
        </Button>
        {accion.nota && <p className="m-0 text-hint text-muted">{accion.nota}</p>}
      </div>
    </article>
  );
}
