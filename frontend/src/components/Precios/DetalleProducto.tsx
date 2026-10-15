import { useState, type FormEvent, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import {
  CheckCircleIcon,
  TrendDownIcon,
  TrendUpIcon,
  WarningCircleIcon,
  WarningIcon,
} from '@phosphor-icons/react';
import { RequierePlan } from '@/components/Plan/PlanGate';
import { Badge, Button, InputField, Sheet, buttonClasses } from '@/components/ui';
import {
  useAccionesRed,
  useListas,
  useProducto,
  useProveedores,
  useReglas,
  useUmbrales,
  useZona,
} from '@/hooks/red/useRed';
import { margenSobreCosto, precioSegunRegla, reglaDe, variacionPct } from '@/lib/red/calculos';
import { costo, fechaCorta, fechaLarga, pesos, porcentaje } from '@/lib/red/formato';
import type { ProductoRed } from '@/types/red';
import { leerNumero, textoGanancia } from './ayudas';
import { GananciaBadge } from './GananciaBadge';
import { GraficoCostos } from './GraficoCostos';

type DetalleProductoProps = {
  productoId: string | null;
  onCerrar: () => void;
  /** Se llama después de guardar el precio. */
  onPrecioGuardado: () => void;
};

const enlace =
  'font-semibold text-accent-800 underline decoration-accent-800/40 underline-offset-4 hover:text-accent-900';

// Detalle de un producto en una hoja: precio y costo, de dónde sale el costo, cambio de precio y
// comparaciones. Lo que depende del plan lleva su candado.
export function DetalleProducto({ productoId, onCerrar, onPrecioGuardado }: DetalleProductoProps) {
  const producto = useProducto(productoId ?? undefined);
  const proveedores = useProveedores();
  if (!producto) return null;
  const proveedor = proveedores.find((p) => p.id === producto.proveedorId)?.nombre;

  return (
    <Sheet
      open
      onClose={onCerrar}
      title={producto.nombre}
      description={`${producto.categoria}${proveedor ? ` · ${proveedor}` : ''}`}
    >
      <ContenidoDetalle key={producto.id} producto={producto} onPrecioGuardado={onPrecioGuardado} />
    </Sheet>
  );
}

function Seccion({ id, titulo, children }: { id: string; titulo: string; children: ReactNode }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-3 border-t border-line pt-5">
      <h3 id={id} className="m-0 text-h3">
        {titulo}
      </h3>
      {children}
    </section>
  );
}

function ContenidoDetalle({
  producto,
  onPrecioGuardado,
}: {
  producto: ProductoRed;
  onPrecioGuardado: () => void;
}) {
  const reglas = useReglas();
  const margen = margenSobreCosto(producto.costo, producto.precio);

  return (
    <div className="flex flex-col gap-5">
      <section aria-label="Precio y costo de hoy" className="flex flex-col gap-3">
        <dl className="m-0 grid grid-cols-2 gap-3">
          <div>
            <dt className="text-hint text-muted">Te cuesta</dt>
            <dd className="m-0 text-h3 font-bold">{costo(producto.costo)}</dd>
          </div>
          <div>
            <dt className="text-hint text-muted">Lo vendés a</dt>
            <dd className="m-0 text-h3 font-bold">{pesos(producto.precio)}</dd>
          </div>
        </dl>
        <div>
          <GananciaBadge margen={margen} minimo={reglas.margenMinimo} />
        </div>
        <p className="m-0 text-body text-soft">
          Este costo rige desde el {fechaLarga(producto.costoDesde)}
          {', '}
          <OrigenDelCosto listaId={producto.costoListaId} />.
        </p>
      </section>

      <CambiarPrecio producto={producto} onGuardado={onPrecioGuardado} />

      <Seccion id={`historial-${producto.id}`} titulo="Historial de costos">
        <RequierePlan
          funcion="historial-costos"
          beneficio="Mirá cómo fue cambiando el costo y de qué lista salió cada vez."
        >
          <HistorialDeCostos producto={producto} />
        </RequierePlan>
      </Seccion>

      <Seccion id={`proveedores-${producto.id}`} titulo="Otros proveedores">
        <RequierePlan
          funcion="comparar-proveedores"
          beneficio="Compará lo que te cuesta con lo que cobran otros proveedores de la red."
        >
          <OtrosProveedores producto={producto} />
        </RequierePlan>
      </Seccion>

      <Seccion id={`zona-${producto.id}`} titulo="Tu zona">
        <RequierePlan
          funcion="comparacion-zona"
          beneficio="Enterate si pagás más o menos que otros comercios de tu zona."
        >
          <ComparacionZona producto={producto} />
        </RequierePlan>
      </Seccion>
    </div>
  );
}

/** «de la lista X» (con enlace, si la lista existe) o «cargado a mano». */
function OrigenDelCosto({ listaId }: { listaId: string | null }) {
  const listas = useListas();
  const lista = listaId ? listas.find((l) => l.id === listaId) : undefined;
  if (!listaId) return <>cargado a mano</>;
  if (!lista) return <>de una lista que ya no está</>;
  return (
    <>
      de la lista{' '}
      <Link to="/listas/$listaId" params={{ listaId }} className={enlace}>
        {lista.nombre}
      </Link>
    </>
  );
}

/* ---------- cambiar el precio ---------- */

function CambiarPrecio({
  producto,
  onGuardado,
}: {
  producto: ProductoRed;
  onGuardado: () => void;
}) {
  const reglas = useReglas();
  const { aplicarPrecio } = useAccionesRed();
  const [texto, setTexto] = useState(String(producto.precio));
  const [error, setError] = useState<string>();

  const regla = reglaDe(reglas, producto.categoria);
  const sugerido = precioSegunRegla(producto.costo, regla);
  const margenSugerido = margenSobreCosto(producto.costo, sugerido);
  const valor = leerNumero(texto);
  const esValido = valor !== null && valor > 0;
  const margenNuevo = esValido ? margenSobreCosto(producto.costo, valor) : null;

  function guardar(event: FormEvent) {
    event.preventDefault();
    if (!esValido) {
      setError('Escribí un precio mayor a cero.');
      return;
    }
    if (valor === producto.precio) {
      setError('Ese ya es el precio de hoy. Escribí uno distinto.');
      return;
    }
    aplicarPrecio(producto.id, valor);
    onGuardado();
  }

  return (
    <Seccion id={`precio-${producto.id}`} titulo="Cambiar el precio">
      <p className="m-0 text-body text-soft">
        Con tus reglas: <strong className="text-fg">{pesos(sugerido)}</strong>, ganancia{' '}
        {porcentaje(margenSugerido)}.
      </p>
      {sugerido === producto.precio && (
        <p className="m-0 text-hint text-muted">Es el precio que ya tenés.</p>
      )}
      {sugerido !== producto.precio && sugerido !== valor && (
        <div>
          <Button
            variant="primary"
            onClick={() => {
              setTexto(String(sugerido));
              setError(undefined);
            }}
          >
            Usar este precio
          </Button>
        </div>
      )}
      <form onSubmit={guardar} noValidate className="flex flex-col gap-3">
        <InputField
          label="Precio nuevo"
          inputMode="decimal"
          autoComplete="off"
          value={texto}
          error={error}
          onChange={(event) => {
            setTexto(event.target.value);
            setError(undefined);
          }}
          hint="En pesos, lo que cobrás al público."
        />
        {margenNuevo !== null && (
          <p className="m-0 flex items-start gap-2 text-hint text-soft" aria-live="polite">
            {margenNuevo < 0 ? (
              <>
                <WarningIcon
                  aria-hidden
                  size={20}
                  weight="fill"
                  className="mt-0.5 shrink-0 text-bad"
                />
                Con este precio vendés por debajo de lo que te cuesta.
              </>
            ) : margenNuevo < reglas.margenMinimo ? (
              <>
                <WarningCircleIcon
                  aria-hidden
                  size={20}
                  weight="fill"
                  className="mt-0.5 shrink-0 text-amber-700"
                />
                Con este precio ganás {textoGanancia(margenNuevo, reglas.margenMinimo)}: queda por
                debajo de tu mínimo ({porcentaje(reglas.margenMinimo)}).
              </>
            ) : (
              <>
                <CheckCircleIcon
                  aria-hidden
                  size={20}
                  weight="fill"
                  className="mt-0.5 shrink-0 text-ok"
                />
                Con este precio ganás {textoGanancia(margenNuevo, reglas.margenMinimo)} sobre el
                costo.
              </>
            )}
          </p>
        )}
        <div>
          <Button type="submit" variant="solid">
            Guardar precio
          </Button>
        </div>
      </form>
    </Seccion>
  );
}

/* ---------- historial ---------- */

function HistorialDeCostos({ producto }: { producto: ProductoRed }) {
  const listas = useListas();
  const proveedores = useProveedores();
  const filas = [...producto.historial].sort((a, b) => b.fecha.localeCompare(a.fecha));

  return (
    <div className="flex flex-col gap-3">
      <GraficoCostos historial={producto.historial} producto={producto.nombre} />
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-hint">
          <caption className="sr-only">Costos anteriores de {producto.nombre}</caption>
          <thead>
            <tr className="border-b border-line text-soft">
              <th scope="col" className="py-2 pr-3 font-semibold">
                Fecha
              </th>
              <th scope="col" className="py-2 pr-3 text-right font-semibold">
                Costo
              </th>
              <th scope="col" className="py-2 font-semibold">
                De dónde salió
              </th>
            </tr>
          </thead>
          <tbody>
            {filas.map((fila, indice) => {
              const lista = fila.listaId ? listas.find((l) => l.id === fila.listaId) : undefined;
              const proveedor = proveedores.find((p) => p.id === fila.proveedorId)?.nombre;
              return (
                <tr key={`${fila.fecha}-${indice}`} className="border-b border-line/60 align-top">
                  <th scope="row" className="py-2 pr-3 font-normal whitespace-nowrap">
                    {fechaCorta(fila.fecha)}
                  </th>
                  <td className="py-2 pr-3 text-right font-semibold whitespace-nowrap">
                    {costo(fila.costo)}
                  </td>
                  <td className="py-2">
                    {fila.listaId && lista ? (
                      <Link
                        to="/listas/$listaId"
                        params={{ listaId: fila.listaId }}
                        className={enlace}
                      >
                        {lista.nombre}
                      </Link>
                    ) : (
                      <span>Cargado a mano</span>
                    )}
                    {proveedor && <span className="block text-muted">{proveedor}</span>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* ---------- otros proveedores ---------- */

function OtrosProveedores({ producto }: { producto: ProductoRed }) {
  const proveedores = useProveedores();

  if (producto.alternativas.length === 0) {
    return (
      <p className="m-0 text-body text-soft">
        Por ahora ningún otro proveedor de la red tiene este producto.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <ul className="m-0 flex list-none flex-col gap-3 p-0">
        {producto.alternativas.map((alternativa) => {
          const nombre =
            proveedores.find((p) => p.id === alternativa.proveedorId)?.nombre ?? 'Otro proveedor';
          const diferencia = alternativa.costo - producto.costo;
          const variacion = variacionPct(producto.costo, alternativa.costo);
          const ahorroMes = Math.round((-diferencia * producto.ventasSemana * 52) / 12);
          return (
            <li key={alternativa.proveedorId} className="flex flex-col gap-1.5">
              <p className="m-0 text-body">
                <strong>{nombre}</strong>: {costo(alternativa.costo)}
              </p>
              <p className="m-0">
                {diferencia < 0 ? (
                  <Badge tone="ok" icon={<TrendDownIcon size={18} weight="bold" />}>
                    {pesos(-diferencia)} menos ({porcentaje(Math.abs(variacion))})
                  </Badge>
                ) : diferencia > 0 ? (
                  <Badge tone="warn" icon={<TrendUpIcon size={18} weight="bold" />}>
                    {pesos(diferencia)} más ({porcentaje(variacion)})
                  </Badge>
                ) : (
                  <Badge>Cuesta lo mismo</Badge>
                )}
              </p>
              {diferencia < 0 && ahorroMes > 0 && (
                <p className="m-0 text-hint text-muted">
                  Al ritmo de tus ventas, serían unos {pesos(ahorroMes)} menos por mes.
                </p>
              )}
            </li>
          );
        })}
      </ul>
      <div>
        <Link to="/red" className={buttonClasses({ variant: 'primary' })}>
          Ver en mi red
        </Link>
      </div>
    </div>
  );
}

/* ---------- zona ---------- */

function ComparacionZona({ producto }: { producto: ProductoRed }) {
  const zona = useZona();
  const umbrales = useUmbrales();
  const referencia = zona.find((z) => z.productoId === producto.id);

  if (!referencia) {
    return (
      <p className="m-0 text-body text-soft">
        Todavía no tenemos datos de tu zona para este producto.
      </p>
    );
  }

  // Privacidad: con pocos comercios no se muestra ningún dato de la zona.
  if (referencia.comercios < umbrales.minimoComerciosZona) {
    return (
      <p className="m-0 text-body text-soft">
        Todavía no hay suficientes comercios en tu zona para comparar ({referencia.comercios} de{' '}
        {umbrales.minimoComerciosZona}). Los datos se muestran juntos y sin nombres.
      </p>
    );
  }

  const variacion = variacionPct(referencia.costoPromedio, producto.costo);
  const cerca = Math.abs(variacion) < 1;

  return (
    <div className="flex flex-col gap-2">
      <dl className="m-0 grid grid-cols-2 gap-3">
        <div>
          <dt className="text-hint text-muted">Vos pagás</dt>
          <dd className="m-0 text-h3 font-bold">{costo(producto.costo)}</dd>
        </div>
        <div>
          <dt className="text-hint text-muted">Promedio de la zona</dt>
          <dd className="m-0 text-h3 font-bold">{costo(referencia.costoPromedio)}</dd>
        </div>
      </dl>
      <p className="m-0">
        {cerca ? (
          <Badge tone="ok" icon={<CheckCircleIcon size={18} weight="fill" />}>
            Pagás lo mismo que la zona
          </Badge>
        ) : variacion > 0 ? (
          <Badge tone="bad" icon={<TrendUpIcon size={18} weight="bold" />}>
            Pagás {porcentaje(variacion)} más que la zona
          </Badge>
        ) : (
          <Badge tone="ok" icon={<TrendDownIcon size={18} weight="bold" />}>
            Pagás {porcentaje(Math.abs(variacion))} menos que la zona
          </Badge>
        )}
      </p>
      <p className="m-0 text-hint text-muted">
        Promedio anónimo de {referencia.comercios} comercios de tu zona.
      </p>
    </div>
  );
}
