import { ArrowRightIcon, CheckCircleIcon, QuestionIcon, WarningIcon } from '@phosphor-icons/react';
import { InfoTip, Panel } from '@/components/ui';
import type { resumenDeLista } from '@/lib/red/calculos';
import { costo, plural, porcentaje } from '@/lib/red/formato';
import type { ProveedorRed } from '@/types/red';
import type { CambioRaro } from './analisis';

type PasoQueCambioProps = {
  proveedor: ProveedorRed | undefined;
  resumen: ReturnType<typeof resumenDeLista>;
  raros: CambioRaro[];
};

// Paso 1: la foto general de la lista, sin pedirle nada al comercio.
export function PasoQueCambio({ proveedor, resumen, raros }: PasoQueCambioProps) {
  const masSubieron = [...resumen.cambios].sort((a, b) => b.variacion - a.variacion).slice(0, 3);
  const nombre = proveedor?.nombre ?? 'tu proveedor';

  return (
    <div className="flex flex-col gap-5">
      <Panel className="flex flex-col gap-2">
        <p className="m-0 text-h1 font-extrabold">
          {resumen.suben > 0
            ? plural(resumen.suben, 'producto subió', 'productos subieron')
            : 'Ningún producto subió'}
        </p>
        <p className="m-0 text-lead text-soft">
          {resumen.suben > 0
            ? `${porcentaje(resumen.aumentoPromedio, { decimales: 1 })} en promedio en la lista de ${nombre}.`
            : `En la lista de ${nombre} los costos quedaron igual.`}
        </p>
      </Panel>

      {masSubieron.length > 0 && (
        <section aria-labelledby="mas-subieron" className="flex flex-col gap-3">
          <h3 id="mas-subieron" className="m-0 text-h3">
            Los que más subieron
          </h3>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {masSubieron.map((cambio) => (
              <li key={cambio.itemId}>
                <Panel
                  tight
                  className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1"
                >
                  <span className="min-w-0 text-body font-semibold break-words">
                    {cambio.nombre}
                  </span>
                  <span className="flex flex-wrap items-center gap-2 text-body">
                    {costo(cambio.costoAnterior)}
                    <ArrowRightIcon aria-hidden size={18} />
                    <span className="sr-only">pasa a</span>
                    <strong>{costo(cambio.costoNuevo)}</strong>
                    <span className="text-hint text-soft">
                      ({porcentaje(cambio.variacion, { signo: true, decimales: 1 })})
                    </span>
                  </span>
                </Panel>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section aria-labelledby="que-pasa-con-tus-precios" className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-x-1">
          <h3 id="que-pasa-con-tus-precios" className="m-0 text-h3">
            Qué pasa con tus precios
          </h3>
          <InfoTip tema="producto en riesgo">
            Un producto queda en riesgo cuando, con el costo nuevo, te deja menos ganancia que tu
            mínimo. Si tu mínimo es 20 % y pasarías a ganar 12 %, te conviene subir el precio.
          </InfoTip>
        </div>
        {resumen.enRiesgo > 0 ? (
          <Panel flat tight className="flex items-start gap-3">
            <WarningIcon aria-hidden size={26} weight="fill" className="mt-0.5 shrink-0 text-bad" />
            <p className="m-0 text-body">
              <strong>
                {plural(resumen.enRiesgo, 'producto quedaría', 'productos quedarían')} en riesgo
              </strong>{' '}
              si no cambiás el precio: con el costo nuevo ganarías menos que tu mínimo.
            </p>
          </Panel>
        ) : (
          <Panel flat tight className="flex items-start gap-3">
            <CheckCircleIcon
              aria-hidden
              size={26}
              weight="fill"
              className="mt-0.5 shrink-0 text-ok"
            />
            <p className="m-0 text-body">Ningún producto queda en riesgo con tus precios de hoy.</p>
          </Panel>
        )}
      </section>

      {raros.length > 0 && (
        <section aria-labelledby="cambios-raros" className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-x-1">
            <h3 id="cambios-raros" className="m-0 text-h3">
              Cambios que llaman la atención
            </h3>
            <InfoTip tema="cambio raro">
              Es un costo que sube o baja mucho de golpe, por ejemplo 40 % en una sola lista. Puede
              ser un error de la lista: lo revisamos con vos antes de tocar tu precio.
            </InfoTip>
          </div>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {raros.map((raro) => (
              <li key={raro.itemId}>
                <Panel
                  flat
                  tight
                  className="flex items-start gap-3 border-amber-700/40 bg-amber-100"
                >
                  <WarningIcon
                    aria-hidden
                    size={26}
                    weight="fill"
                    className="mt-0.5 shrink-0 text-amber-700"
                  />
                  <p className="m-0 text-body text-fg">
                    <strong>{raro.nombre}</strong> {raro.variacion >= 0 ? 'subió' : 'bajó'}{' '}
                    {porcentaje(Math.abs(raro.variacion))} de golpe: ¿error de la lista? Lo
                    revisamos juntos en el próximo paso.
                  </p>
                </Panel>
              </li>
            ))}
          </ul>
        </section>
      )}

      {resumen.pendientes > 0 && (
        <Panel flat tight className="flex items-start gap-3">
          <QuestionIcon aria-hidden size={26} weight="fill" className="mt-0.5 shrink-0 text-soft" />
          <p className="m-0 text-body">
            <strong>
              {plural(resumen.pendientes, 'línea sin confirmar', 'líneas sin confirmar')}
            </strong>
            : no sabemos con certeza a qué producto tuyo corresponde. Las resolvés en el próximo
            paso.
          </p>
        </Panel>
      )}
    </div>
  );
}
