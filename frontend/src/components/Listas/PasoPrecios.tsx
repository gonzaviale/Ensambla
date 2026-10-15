import { useState, type ReactNode } from 'react';
import { ArrowRightIcon, ShieldCheckIcon, WarningIcon } from '@phosphor-icons/react';
import { Badge, Button, EmptyState, InfoTip, Panel } from '@/components/ui';
import { fieldControlClasses } from '@/components/ui/fieldStyles';
import { margenSobreCosto, reglaDe } from '@/lib/red/calculos';
import { costo, pesos, porcentaje } from '@/lib/red/formato';
import type { CambioPropuesto, ProductoRed, ReglasPrecio } from '@/types/red';
import { precioElegido, proteccionMensual } from './analisis';

type PasoPreciosProps = {
  cambios: CambioPropuesto[];
  productos: ProductoRed[];
  reglas: ReglasPrecio;
  precios: Record<string, number>;
  onPrecio: (itemId: string, precio: number) => void;
};

// Paso 3: el precio de cada producto según las reglas del comercio, editable y con el porqué.
export function PasoPrecios({ cambios, productos, reglas, precios, onPrecio }: PasoPreciosProps) {
  if (cambios.length === 0) {
    return (
      <EmptyState
        icon={<WarningIcon aria-hidden size={32} />}
        title="Todavía no hay precios para revisar"
      >
        <p className="m-0">
          Las líneas de la lista siguen sin confirmar o las ignoraste. Volvé al paso anterior para
          resolver las dudas.
        </p>
      </EmptyState>
    );
  }

  const ordenados = [...cambios].sort(
    (a, b) => Number(b.enRiesgo) - Number(a.enRiesgo) || b.variacion - a.variacion,
  );
  const protegido = proteccionMensual(cambios, productos, precios);

  return (
    <div className="flex flex-col gap-4">
      <Panel flat className="flex items-start gap-3">
        <ShieldCheckIcon aria-hidden size={32} weight="fill" className="shrink-0 text-ok" />
        <p className="m-0 text-body" aria-live="polite">
          Con estos precios <strong>protegés ≈ {pesos(protegido)} por mes</strong>
          <span className="text-soft"> (lo que subís en cada producto por lo que vendés).</span>
        </p>
      </Panel>

      <p className="m-0 text-hint text-soft">
        Ya calculamos cada precio con tus reglas. Podés cambiarlo o dejar el que tenés.{' '}
        <InfoTip tema="ganancia sobre el costo">
          La ganancia se calcula sobre lo que te cuesta. Si el costo nuevo es $ 100 y con tu regla
          de 30 % el precio queda en $ 130, ganás $ 30 por unidad.
        </InfoTip>
        <InfoTip tema="producto en riesgo">
          Un producto está en riesgo cuando, con el costo nuevo, te deja menos ganancia que tu
          mínimo. Si tu mínimo es 20 % y ganarías 12 %, conviene subir el precio.
        </InfoTip>
      </p>

      <ul className="m-0 flex list-none flex-col gap-3 p-0">
        {ordenados.map((cambio) => (
          <li key={cambio.itemId}>
            <FilaPrecio
              cambio={cambio}
              reglas={reglas}
              precio={precioElegido(cambio, precios)}
              onPrecio={(valor) => onPrecio(cambio.itemId, valor)}
            />
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Lee «2.450» o «2450,50» como número; 0 si no es un número válido. */
function leerPrecio(texto: string): number {
  const crudo = texto.trim();
  let limpio = crudo;
  // «2.450» (miles) y «2450,50» (decimales), como se escribe en Argentina.
  if (crudo.includes(',')) limpio = crudo.replace(/\./g, '').replace(',', '.');
  else if (/^\d{1,3}(\.\d{3})+$/.test(crudo)) limpio = crudo.replace(/\./g, '');
  if (limpio === '') return 0;
  const valor = Number(limpio);
  return Number.isFinite(valor) && valor > 0 ? valor : 0;
}

type FilaPrecioProps = {
  cambio: CambioPropuesto;
  reglas: ReglasPrecio;
  precio: number;
  onPrecio: (valor: number) => void;
};

function FilaPrecio({ cambio, reglas, precio, onPrecio }: FilaPrecioProps) {
  const regla = reglaDe(reglas, cambio.categoria);
  const [texto, setTexto] = useState(String(precio));
  const [ultimo, setUltimo] = useState(precio);
  // Si el precio cambia desde afuera («Dejar mi precio actual»), el campo lo refleja.
  if (precio !== ultimo) {
    setUltimo(precio);
    if (leerPrecio(texto) !== precio) setTexto(String(precio));
  }

  const invalido = precio <= 0;
  const margen = margenSobreCosto(cambio.costoNuevo, precio);
  const gananciaBaja = !invalido && margen < reglas.margenMinimo;
  const igualAActual = precio === cambio.precioActual;
  const idCampo = `precio-${cambio.itemId}`;

  return (
    <Panel className="flex flex-col gap-4">
      <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
        <div className="flex min-w-0 flex-col">
          <h3 className="m-0 text-h3 break-words">{cambio.nombre}</h3>
          <p className="m-0 text-hint text-soft">{cambio.categoria}</p>
        </div>
        {cambio.enRiesgo && (
          <Badge tone="bad" icon={<WarningIcon size={16} weight="fill" />}>
            En riesgo
          </Badge>
        )}
      </div>

      <dl className="m-0 grid grid-cols-2 gap-x-4 gap-y-3 md:grid-cols-4">
        <Dato titulo="Tu costo">
          <span className="flex flex-wrap items-center gap-1.5">
            {costo(cambio.costoAnterior)}
            <ArrowRightIcon aria-hidden size={16} />
            <span className="sr-only">pasa a</span>
            <strong>{costo(cambio.costoNuevo)}</strong>
          </span>
          <span className="text-hint text-soft">
            {porcentaje(cambio.variacion, { signo: true, decimales: 1 })}
          </span>
        </Dato>
        <Dato titulo="Tu precio hoy">
          <strong>{pesos(cambio.precioActual)}</strong>
          <span className="text-hint text-soft">
            Ganancia con el costo nuevo: {porcentaje(cambio.margenActual)}
          </span>
        </Dato>
        <Dato titulo="Precio según tus reglas">
          <strong>{pesos(cambio.precioSugerido)}</strong>
          <span className="text-hint text-soft">
            Tus reglas: {porcentaje(regla.margen)} de ganancia en {cambio.categoria}
          </span>
        </Dato>
        <Dato titulo="Ganancia resultante">
          <strong>{invalido ? '—' : porcentaje(margen)}</strong>
          {gananciaBaja && (
            <span className="flex items-center gap-1 text-hint font-semibold text-bad">
              <WarningIcon aria-hidden size={16} weight="fill" />
              Es menos que tu mínimo ({porcentaje(reglas.margenMinimo)})
            </span>
          )}
        </Dato>
      </dl>

      <div className="flex flex-wrap items-end gap-3">
        <div className="flex w-full max-w-48 min-w-36 flex-col gap-1.5">
          <label htmlFor={idCampo} className="text-base font-semibold">
            Precio nuevo<span className="sr-only"> de {cambio.nombre}</span>
          </label>
          <div className="relative">
            <span
              aria-hidden
              className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-body text-soft"
            >
              $
            </span>
            <input
              id={idCampo}
              type="text"
              inputMode="decimal"
              autoComplete="off"
              value={texto}
              aria-invalid={invalido || undefined}
              aria-describedby={invalido ? `${idCampo}-error` : undefined}
              onChange={(event) => {
                setTexto(event.target.value);
                onPrecio(leerPrecio(event.target.value));
              }}
              className={`${fieldControlClasses} pl-8`}
            />
          </div>
        </div>
        <Button
          variant="primary"
          disabled={igualAActual}
          onClick={() => onPrecio(cambio.precioActual)}
        >
          Dejar mi precio actual
        </Button>
        {precio !== cambio.precioSugerido && (
          <Button variant="ghost" onClick={() => onPrecio(cambio.precioSugerido)}>
            Usar el de mis reglas
          </Button>
        )}
      </div>
      {invalido && (
        <p
          id={`${idCampo}-error`}
          role="alert"
          className="m-0 flex items-start gap-1.5 text-hint font-semibold text-bad"
        >
          <WarningIcon aria-hidden size={20} weight="fill" className="mt-0.5 shrink-0" />
          Poné un precio mayor a cero.
        </p>
      )}
    </Panel>
  );
}

function Dato({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <dt className="text-hint font-semibold text-muted">{titulo}</dt>
      <dd className="m-0 flex flex-col text-body">{children}</dd>
    </div>
  );
}
