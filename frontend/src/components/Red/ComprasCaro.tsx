import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, LockKeyIcon, WarningIcon } from '@phosphor-icons/react';
import { RequierePlan } from '@/components/Plan/PlanGate';
import { Badge, EmptyState, Panel, buttonClasses } from '@/components/ui';
import { useBarrio, useProductos, useUmbrales, useZona } from '@/hooks/red/useRed';
import { variacionPct } from '@/lib/red/calculos';
import { pesos, porcentaje } from '@/lib/red/formato';
import type { ProductoRed, ReferenciaZona } from '@/types/red';
import { SEMANAS_POR_MES } from './calculosRed';

// «¿Comprás caro?»: el costo del comercio contra el promedio anónimo de su zona. Si hay pocos
// comercios en la muestra no se muestra el número (privacidad: mínimo configurable, 5 por defecto).
export function ComprasCaro() {
  return (
    <RequierePlan
      funcion="comparacion-zona"
      beneficio="Te decimos si pagás más que otros comercios de tu zona, sin mostrar datos de nadie en particular."
    >
      <ComprasCaroContenido />
    </RequierePlan>
  );
}

type Fila = {
  producto: ProductoRed;
  referencia: ReferenciaZona;
  suficiente: boolean;
  diferencia: number;
};

function ComprasCaroContenido() {
  const productos = useProductos();
  const zona = useZona();
  const barrio = useBarrio();
  const { minimoComerciosZona, diferenciaCompra } = useUmbrales();

  const filas: Fila[] = zona
    .flatMap((referencia) => {
      const producto = productos.find((p) => p.id === referencia.productoId);
      if (!producto) return [];
      return [
        {
          producto,
          referencia,
          suficiente: referencia.comercios >= minimoComerciosZona,
          diferencia: variacionPct(referencia.costoPromedio, producto.costo),
        },
      ];
    })
    .sort((a, b) => Number(b.suficiente) - Number(a.suficiente) || b.diferencia - a.diferencia);

  return (
    <section aria-labelledby="red-caro" data-recorrido="red-caro" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <h2 id="red-caro" className="m-0 text-h3">
          ¿Comprás caro?
        </h2>
        <p className="m-0 text-body text-soft">
          Tu costo contra el promedio de los comercios de {barrio.zona}. Son datos agregados y
          anónimos: sólo se comparan productos con {minimoComerciosZona} comercios o más.
        </p>
      </div>

      {filas.length === 0 ? (
        <EmptyState
          icon={<WarningIcon aria-hidden size={32} />}
          title="Todavía no hay datos de tu zona"
        >
          Cuando haya suficientes comercios que compren lo mismo que vos, te mostramos cómo estás.
        </EmptyState>
      ) : (
        <ul className="m-0 flex list-none flex-col gap-3 p-0">
          {filas.map((fila) => (
            <li key={fila.producto.id}>
              <FilaZona fila={fila} minimo={minimoComerciosZona} umbral={diferenciaCompra} />
            </li>
          ))}
        </ul>
      )}

      <p className="m-0 text-hint">
        <Link to="/precios" className={buttonClasses({ variant: 'ghost' })}>
          Ver mis precios y costos
        </Link>
      </p>
    </section>
  );
}

function FilaZona({ fila, minimo, umbral }: { fila: Fila; minimo: number; umbral: number }) {
  const { producto, referencia, suficiente, diferencia } = fila;

  if (!suficiente) {
    return (
      <Panel flat tight className="flex flex-col gap-1.5">
        <h3 className="m-0 text-body font-bold">{producto.nombre}</h3>
        <p className="m-0 flex items-start gap-2 text-hint text-soft">
          <LockKeyIcon aria-hidden size={20} className="mt-0.5 shrink-0" />
          Todavía no hay suficientes comercios para comparar ({referencia.comercios} de {minimo}).
        </p>
      </Panel>
    );
  }

  const tono = diferencia >= umbral ? 'warn' : diferencia > 0 ? 'neutral' : 'ok';
  const etiqueta =
    diferencia > 0
      ? `Pagás ${porcentaje(diferencia)} más`
      : diferencia < 0
        ? `Pagás ${porcentaje(Math.abs(diferencia))} menos`
        : 'Pagás igual';
  const ahorroMes =
    Math.max(0, producto.costo - referencia.costoPromedio) *
    producto.ventasSemana *
    SEMANAS_POR_MES;
  const maximo = Math.max(producto.costo, referencia.costoPromedio);

  return (
    <Panel className="flex flex-col gap-3">
      <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1">
        <h3 className="m-0 min-w-0 text-body font-bold">{producto.nombre}</h3>
        <Badge
          tone={tono}
          icon={
            tono === 'warn' ? (
              <WarningIcon size={16} weight="fill" />
            ) : (
              <CheckCircleIcon size={16} weight="fill" />
            )
          }
        >
          {etiqueta}
        </Badge>
      </div>

      <div className="flex flex-col gap-2.5">
        <Barra
          etiqueta="Tu costo"
          valor={producto.costo}
          proporcion={producto.costo / maximo}
          clase="bg-accent-700"
        />
        <Barra
          etiqueta={`Promedio de ${referencia.comercios} comercios de la zona`}
          valor={referencia.costoPromedio}
          proporcion={referencia.costoPromedio / maximo}
          clase="bg-sand"
        />
      </div>

      {diferencia >= umbral && ahorroMes > 0 && (
        <p className="m-0 text-hint text-soft">
          Si lo consiguieras al promedio, ahorrarías ≈{' '}
          <strong className="text-fg">{pesos(ahorroMes)}</strong> por mes.
        </p>
      )}
    </Panel>
  );
}

function Barra({
  etiqueta,
  valor,
  proporcion,
  clase,
}: {
  etiqueta: string;
  valor: number;
  proporcion: number;
  clase: string;
}) {
  return (
    <div className="flex flex-col gap-1">
      <p className="m-0 flex flex-wrap items-baseline justify-between gap-x-3 text-hint text-soft">
        <span>{etiqueta}</span>
        <strong className="text-body text-fg">{pesos(valor)}</strong>
      </p>
      <div aria-hidden className="h-3 w-full rounded-full bg-sand-100">
        <div
          className={`h-3 rounded-full ${clase}`}
          style={{ width: `${Math.max(4, Math.round(proporcion * 100))}%` }}
        />
      </div>
    </div>
  );
}
