import type { ReactNode } from 'react';
import { HandCoinsIcon, ShieldCheckIcon, TagIcon, TimerIcon } from '@phosphor-icons/react';
import { InfoTip, Panel } from '@/components/ui';
import { useValor } from '@/hooks/red/useRed';
import { pesos, plural } from '@/lib/red/formato';
import { MINUTOS_CON_ENSAMBLA_POR_LISTA, MINUTOS_MANUALES_POR_LISTA } from '@/lib/red/valor';
import { formatearTiempo } from './formatoHoy';

type CifraProps = { icono: ReactNode; cifra: string; descripcion: string; ayuda?: ReactNode };

function Cifra({ icono, cifra, descripcion, ayuda }: CifraProps) {
  return (
    <li className="flex min-w-0 flex-col gap-1.5 rounded-lg bg-canvas p-4">
      <span aria-hidden className="text-accent-800">
        {icono}
      </span>
      <p className="m-0 text-h2 leading-none font-bold break-words text-fg">{cifra}</p>
      <p className="m-0 text-hint text-soft">
        {descripcion}
        {ayuda && <> {ayuda}</>}
      </p>
    </li>
  );
}

// «El valor que aporta» (doc §2): cuatro cifras que se entienden sin explicación, calculadas con los
// datos del propio comercio, y un desplegable que dice cómo se estiman.
export function ValorAportado() {
  const valor = useValor();
  const ahorroPorLista = MINUTOS_MANUALES_POR_LISTA - MINUTOS_CON_ENSAMBLA_POR_LISTA;
  const sinValorTodavia = valor.preciosCorregidos === 0 && valor.listasAplicadas === 0;

  return (
    <Panel aria-labelledby="hoy-valor" data-recorrido="hoy-valor" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 id="hoy-valor" className="m-0 text-h3">
          El valor que aporta Ensambla
        </h2>
        <p className="m-0 text-hint text-muted">Lo que lograste en los últimos 30 días.</p>
      </div>

      <ul className="m-0 grid list-none grid-cols-1 gap-3 p-0 sm:grid-cols-2 lg:grid-cols-4">
        <Cifra
          icono={<TagIcon size={28} weight="bold" />}
          cifra={String(valor.preciosCorregidos)}
          descripcion={
            valor.preciosCorregidos === 1 ? 'precio corregido' : 'precios corregidos a tiempo'
          }
        />
        <Cifra
          icono={<HandCoinsIcon size={28} weight="bold" />}
          cifra={pesos(valor.plataProtegidaMes)}
          descripcion="por mes que dejaste de perder"
          ayuda={
            <InfoTip tema="plata que dejaste de perder">
              Es lo que ganás de más cada mes gracias a los precios que corregiste a tiempo. Si
              subiste un producto $ 20 y vendés 10 por semana, son $ 800 por mes.
            </InfoTip>
          }
        />
        <Cifra
          icono={<TimerIcon size={28} weight="bold" />}
          cifra={formatearTiempo(valor.minutosAhorrados)}
          descripcion="de trabajo que te ahorraste con las listas"
        />
        <Cifra
          icono={<ShieldCheckIcon size={28} weight="bold" />}
          cifra={String(valor.bajoCosto)}
          descripcion={
            valor.bajoCosto === 1
              ? 'producto que vendías bajo el costo y detectamos'
              : 'productos que vendías bajo el costo y detectamos'
          }
        />
      </ul>

      {sinValorTodavia && (
        <p className="m-0 text-body text-soft">
          Todavía no hay nada para mostrar: cuando apliques tu primera lista vas a ver acá cuánto
          ganás.
        </p>
      )}

      <div className="flex items-start gap-1">
        <details className="group min-w-0 flex-1 rounded-lg border border-line bg-canvas">
          <summary className="flex min-h-12 cursor-pointer items-center px-4 py-2.5 text-base font-semibold text-accent-800 hover:text-accent-900">
            ¿Cómo lo calculamos?
          </summary>
          <div className="flex flex-col gap-3 px-4 pb-4 text-body text-soft">
            <p className="m-0">
              Son estimaciones con tus propios datos, para que tengas una idea del orden de
              magnitud.
            </p>
            <ul className="m-0 flex list-disc flex-col gap-2 pl-5">
              <li>
                <strong className="text-fg">Precios corregidos:</strong> los cambios de precio que
                aplicaste en los últimos 30 días.
              </li>
              <li>
                <strong className="text-fg">Plata que dejaste de perder:</strong> por cada precio
                que subiste, la diferencia con el precio viejo por las unidades que vendés por
                semana y por 4 semanas. Supone que vendés lo mismo que esta semana.
              </li>
              <li>
                <strong className="text-fg">Tiempo ahorrado:</strong> revisar una lista a mano lleva
                unos {MINUTOS_MANUALES_POR_LISTA} minutos y con Ensambla unos{' '}
                {MINUTOS_CON_ENSAMBLA_POR_LISTA}. Ahorrás {ahorroPorLista} minutos por cada lista
                aplicada: en estos 30 días aplicaste{' '}
                {plural(valor.listasAplicadas, 'lista', 'listas')}.
              </li>
              <li>
                <strong className="text-fg">Bajo el costo:</strong> productos cuyo precio de venta
                quedó por debajo del costo nuevo y que corregiste.
              </li>
            </ul>
          </div>
        </details>
        <InfoTip tema="cómo lo calculamos" className="mt-2.5">
          Son estimaciones con tus propios datos: te dan el orden de magnitud, no una promesa. Si
          esta semana vendés distinto, las cifras cambian.
        </InfoTip>
      </div>
    </Panel>
  );
}
