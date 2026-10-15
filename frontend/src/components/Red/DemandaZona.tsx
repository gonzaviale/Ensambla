import { ChartBarHorizontalIcon } from '@phosphor-icons/react';
import { Badge, Panel } from '@/components/ui';
import { useUmbrales } from '@/hooks/red/useRed';
import { DEMANDA_ILUSTRATIVA } from './datosDemo';

// Lado del mayorista, paso 3: demanda agregada y anónima de la zona, para decidir qué ofertar.
export function DemandaZona() {
  const { minimoComerciosZona } = useUmbrales();
  const maximo = Math.max(...DEMANDA_ILUSTRATIVA.map((fila) => fila.pedidos));

  return (
    <Panel aria-labelledby="mayorista-demanda" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
          <h2 id="mayorista-demanda" className="m-0 text-h3">
            Demanda de la zona
          </h2>
          <Badge tone="warn" icon={<ChartBarHorizontalIcon size={16} weight="fill" />}>
            Datos ilustrativos
          </Badge>
        </div>
        <p className="m-0 text-body text-soft">
          Los productos más pedidos esta semana en la zona, sumando a todos los comercios. Sirve
          para decidir qué ofertar.
        </p>
      </div>

      <ul className="m-0 flex list-none flex-col gap-3 p-0">
        {DEMANDA_ILUSTRATIVA.map((fila) => (
          <li key={fila.producto} className="flex flex-col gap-1">
            <p className="m-0 flex flex-wrap items-baseline justify-between gap-x-3 text-hint text-soft">
              <span>{fila.producto}</span>
              <strong className="text-body text-fg">{fila.pedidos} pedidos</strong>
            </p>
            <div aria-hidden className="h-3 w-full rounded-full bg-sand-100">
              <div
                className="h-3 rounded-full bg-accent-700"
                style={{ width: `${Math.round((fila.pedidos / maximo) * 100)}%` }}
              />
            </div>
          </li>
        ))}
      </ul>

      <p className="m-0 text-hint text-soft">
        Nunca vemos qué compra cada comercio: sólo se muestran totales y de a {minimoComerciosZona}{' '}
        comercios o más.
      </p>
    </Panel>
  );
}
