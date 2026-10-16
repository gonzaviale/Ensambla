import { CheckCircleIcon, ReceiptIcon, XCircleIcon } from '@phosphor-icons/react';
import { Badge, EmptyState, Panel } from '@/components/ui';
import { useSuscripcion } from '@/hooks/negocio/usePlan';
import { fechaLarga, pesos } from '@/lib/formato';

// Lista de pagos: fecha, concepto, importe y estado (ícono + texto).
export function HistorialPagos() {
  const { pagos } = useSuscripcion();

  return (
    <section aria-labelledby="plan-pagos" className="flex flex-col gap-4">
      <h2 id="plan-pagos" className="m-0 text-h3">
        Historial de pagos
      </h2>
      {pagos.length === 0 ? (
        <EmptyState icon={<ReceiptIcon aria-hidden size={32} />} title="Todavía no hiciste pagos">
          <p className="m-0">
            Mientras estés en la prueba o en el plan Gratis no pagás nada. Cuando elijas un plan,
            cada cobro aparece acá.
          </p>
        </EmptyState>
      ) : (
        <Panel tight className="p-0">
          <ul className="m-0 flex list-none flex-col p-0">
            {pagos.map((pago) => (
              <li
                key={pago.id}
                className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line px-4 py-3.5 last:border-b-0"
              >
                <div className="flex min-w-0 flex-col">
                  <span className="text-body font-semibold">{pago.concepto}</span>
                  <span className="text-hint text-muted">{fechaLarga(pago.fecha)}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-body font-bold">{pesos(pago.importe)}</span>
                  {pago.estado === 'aprobado' ? (
                    <Badge tone="ok" icon={<CheckCircleIcon size={16} weight="fill" />}>
                      Aprobado
                    </Badge>
                  ) : (
                    <Badge tone="bad" icon={<XCircleIcon size={16} weight="fill" />}>
                      Rechazado
                    </Badge>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </Panel>
      )}
    </section>
  );
}
