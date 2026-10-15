import { useEffect, useRef, useState } from 'react';
import {
  ArrowCounterClockwiseIcon,
  CheckCircleIcon,
  ProhibitIcon,
  QuestionIcon,
  WarningIcon,
} from '@phosphor-icons/react';
import { Badge, Button, InfoTip, Panel, SelectField } from '@/components/ui';
import { useAccionesRed } from '@/hooks/red/useRed';
import { costoUnitario, variacionPct } from '@/lib/red/calculos';
import { costo, plural, porcentaje } from '@/lib/red/formato';
import type { ItemLista, PresentacionLista, ProductoRed } from '@/types/red';
import { costoEsperado, nivelDeConfianza } from './analisis';
import { OpcionRadio } from './OpcionRadio';

type PasoDudasProps = {
  listaId: string;
  /** Ids de las líneas a revisar, fijados al abrir la lista para que no desaparezcan al resolverse. */
  ids: string[];
  /** Cómo estaba cada línea al abrir la lista (sirve para deshacer). */
  originales: Record<string, ItemLista>;
  items: ItemLista[];
  productos: ProductoRed[];
};

// Paso 2: sólo lo que Ensambla no puede decidir solo. Cada línea se resuelve con un toque y se puede deshacer.
export function PasoDudas({ listaId, ids, originales, items, productos }: PasoDudasProps) {
  const lineas = ids
    .map((id) => items.find((item) => item.id === id))
    .filter((item): item is ItemLista => item !== undefined);
  const resueltas = lineas.filter((item) => item.confirmado || item.ignorado).length;
  const faltan = lineas.length - resueltas;

  if (lineas.length === 0) {
    return (
      <Panel flat className="flex items-start gap-3">
        <CheckCircleIcon aria-hidden size={32} weight="fill" className="shrink-0 text-ok" />
        <div className="flex flex-col gap-1">
          <p className="m-0 text-h3">No hay dudas: todo coincidió</p>
          <p className="m-0 text-body text-soft">
            Todas las líneas de la lista se corresponden con tus productos. Seguí para ver tus
            precios.
          </p>
        </div>
      </Panel>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-x-1">
        <p className="m-0 text-body text-soft" aria-live="polite">
          {faltan > 0
            ? `Te ${faltan === 1 ? 'falta' : 'faltan'} ${plural(faltan, 'duda', 'dudas')} por resolver de ${lineas.length}.`
            : 'Resolviste todas las dudas. Ya podés seguir.'}
        </p>
        <InfoTip tema="seguridad de la lectura">
          Es qué tan seguros estamos de haber entendido bien la línea de la lista. Si la seguridad
          es baja, conviene que confirmes vos que es el producto correcto.
        </InfoTip>
      </div>
      <ol className="m-0 flex list-none flex-col gap-4 p-0">
        {lineas.map((item, indice) => {
          const original = originales[item.id] ?? item;
          const siguiente = lineas.slice(indice + 1).find((l) => !(l.confirmado || l.ignorado));
          return (
            <li key={item.id} id={`duda-${item.id}`}>
              <LineaDuda
                listaId={listaId}
                item={item}
                original={original}
                productos={productos}
                idSiguiente={siguiente?.id}
              />
            </li>
          );
        })}
      </ol>
    </div>
  );
}

type LineaDudaProps = {
  listaId: string;
  item: ItemLista;
  original: ItemLista;
  productos: ProductoRed[];
  idSiguiente: string | undefined;
};

function irA(id: string | undefined) {
  if (!id) return;
  const el = document.getElementById(`duda-${id}`);
  el?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
  el?.querySelector<HTMLElement>('button, select, input')?.focus({ preventScroll: true });
}

function LineaDuda({ listaId, item, original, productos, idSiguiente }: LineaDudaProps) {
  const { confirmarItem, ignorarItem } = useAccionesRed();
  const resuelta = item.confirmado || item.ignorado;
  const sugeridoId = original.productoId ?? original.sugeridos[0] ?? null;
  const sugerido = productos.find((p) => p.id === sugeridoId);
  const confirmado = productos.find((p) => p.id === item.productoId);
  const confianza = nivelDeConfianza(original.confianza);

  const unidadesPorBulto = original.unidadesPorBulto ?? sugerido?.unidadesPorBulto ?? null;
  const [presentacion, setPresentacion] = useState<PresentacionLista>(() => {
    if (original.motivo !== 'unidad-bulto' || !unidadesPorBulto || !sugerido) {
      return original.presentacion;
    }
    // Parece ser la opción cuyo costo por unidad queda más cerca del costo actual.
    const porUnidad = Math.abs(variacionPct(sugerido.costo, original.precioLista));
    const porBulto = Math.abs(
      variacionPct(sugerido.costo, costoUnitario(original.precioLista, 'BULTO', unidadesPorBulto)),
    );
    return porBulto <= porUnidad ? 'BULTO' : 'UNIDAD';
  });
  const [otroId, setOtroId] = useState('');

  const esUnidadBulto = original.motivo === 'unidad-bulto' && unidadesPorBulto !== null;
  const decisionExtra = esUnidadBulto
    ? { presentacion, unidadesPorBulto: unidadesPorBulto }
    : undefined;

  // Al resolver una línea el foco pasa a la siguiente duda (o queda en «Deshacer»); al deshacer,
  // vuelve a las decisiones de la propia línea. Así el teclado no se pierde cuando el botón desaparece.
  const contenedor = useRef<HTMLDivElement>(null);
  const resueltaAntes = useRef(resuelta);
  useEffect(() => {
    if (resueltaAntes.current === resuelta) return;
    resueltaAntes.current = resuelta;
    if (resuelta && idSiguiente) irA(idSiguiente);
    else contenedor.current?.querySelector<HTMLElement>('button')?.focus();
  }, [resuelta, idSiguiente]);

  function confirmar(productoId: string) {
    confirmarItem(listaId, item.id, { productoId, ...decisionExtra });
  }

  function ignorar() {
    ignorarItem(listaId, item.id, true);
  }

  function deshacer() {
    if (item.ignorado) {
      ignorarItem(listaId, item.id, false);
      return;
    }
    // No hay una acción de «sacar la confirmación»: se vuelve a la lectura original de la línea.
    confirmarItem(listaId, item.id, {
      productoId: null,
      presentacion: original.presentacion,
      unidadesPorBulto: original.unidadesPorBulto,
    });
  }

  return (
    <Panel className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <p className="m-0 text-hint text-soft">Tu proveedor escribió:</p>
        <p className="m-0 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
          <code className="rounded-md bg-sand-100 px-2 py-1 font-mono text-base break-words text-fg">
            {original.textoOriginal}
          </code>
          <span className="text-body font-semibold">{costo(original.precioLista)}</span>
        </p>
      </div>

      {resuelta ? (
        <div ref={contenedor} className="flex flex-wrap items-center justify-between gap-3">
          <p className="m-0 flex min-w-0 items-start gap-2 text-body">
            <CheckCircleIcon
              aria-hidden
              size={26}
              weight="fill"
              className="mt-0.5 shrink-0 text-ok"
            />
            <span>
              <strong>Listo</strong>
              {item.ignorado ? (
                <>: la ignoramos, no se va a usar para cambiar precios.</>
              ) : (
                <>
                  : es <strong>{confirmado?.nombre ?? 'tu producto'}</strong>
                  {confirmado && (
                    <>
                      . Costo por unidad: <strong>{costo(costoEsperado(item))}</strong> (antes{' '}
                      {costo(confirmado.costo)}).
                    </>
                  )}
                </>
              )}
            </span>
          </p>
          <Button variant="ghost" onClick={deshacer}>
            <ArrowCounterClockwiseIcon aria-hidden size={20} />
            Deshacer
          </Button>
        </div>
      ) : (
        <div ref={contenedor} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            {sugerido ? (
              <p className="m-0 text-body">
                Creemos que es <strong>{sugerido.nombre}</strong>.
              </p>
            ) : (
              <p className="m-0 flex items-start gap-2 text-body">
                <QuestionIcon aria-hidden size={24} weight="fill" className="mt-0.5 shrink-0" />
                No encontramos ningún producto tuyo que se parezca a esta línea.
              </p>
            )}
            <Seguridad valor={original.confianza} texto={confianza.texto} tono={confianza.tono} />
          </div>

          {original.motivo === 'precio-raro' && sugerido && (
            <p className="m-0 flex items-start gap-2 rounded-md bg-amber-100 p-3 text-body text-fg">
              <WarningIcon
                aria-hidden
                size={24}
                weight="fill"
                className="mt-0.5 shrink-0 text-amber-700"
              />
              <span>
                El costo pasa de {costo(sugerido.costo)} a {costo(costoEsperado(original))}:{' '}
                <strong>
                  {porcentaje(variacionPct(sugerido.costo, costoEsperado(original)), {
                    signo: true,
                  })}{' '}
                  de golpe
                </strong>
                . ¿Es correcto o es un error de la lista?
              </span>
            </p>
          )}

          {esUnidadBulto && sugerido && (
            <ControlUnidadBulto
              itemId={item.id}
              precioLista={original.precioLista}
              unidadesPorBulto={unidadesPorBulto}
              costoActual={sugerido.costo}
              valor={presentacion}
              onChange={setPresentacion}
            />
          )}

          {original.estado === 'sin-coincidencia' || !sugerido ? (
            <p className="m-0 text-hint text-soft">
              Si es un producto que no vendés, podés ignorar la línea: no se usa para cambiar ningún
              precio.
            </p>
          ) : null}

          <div className="flex flex-wrap items-end gap-3">
            {sugerido && (
              <Button variant="solid" onClick={() => confirmar(sugerido.id)}>
                <CheckCircleIcon aria-hidden size={22} weight="bold" />
                Es este producto
              </Button>
            )}
            <Button onClick={ignorar}>
              <ProhibitIcon aria-hidden size={20} />
              {sugerido ? 'No es ninguno de mis productos' : 'Ignorar esta línea'}
            </Button>
          </div>

          <div className="flex flex-wrap items-end gap-3">
            <SelectField
              className="min-w-0"
              label="O elegí otro de tus productos"
              value={otroId}
              onChange={(event) => setOtroId(event.target.value)}
            >
              <option value="">Elegí un producto…</option>
              {productos.map((producto) => (
                <option key={producto.id} value={producto.id}>
                  {producto.nombre}
                </option>
              ))}
            </SelectField>
            <Button variant="primary" disabled={!otroId} onClick={() => confirmar(otroId)}>
              Usar este otro
            </Button>
          </div>
        </div>
      )}
    </Panel>
  );
}

function Seguridad({ valor, texto, tono }: { valor: number; texto: string; tono: string }) {
  const porcentajeEntero = Math.round(valor * 100);
  const color = tono === 'ok' ? 'bg-ok' : tono === 'warn' ? 'bg-amber' : 'bg-bad';
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
      <span className="text-hint font-semibold text-soft">Seguridad:</span>
      <span aria-hidden className="h-2.5 w-32 overflow-hidden rounded-full bg-fg/12">
        <span
          className={`block h-full rounded-full ${color}`}
          style={{ width: `${porcentajeEntero}%` }}
        />
      </span>
      <Badge tone={tono === 'ok' ? 'ok' : tono === 'warn' ? 'warn' : 'bad'}>
        {texto}
        {valor > 0 && ` · ${porcentajeEntero} %`}
      </Badge>
    </div>
  );
}

type ControlUnidadBultoProps = {
  itemId: string;
  precioLista: number;
  unidadesPorBulto: number;
  costoActual: number;
  valor: PresentacionLista;
  onChange: (valor: PresentacionLista) => void;
};

// El control clave: el mismo número puede ser el precio de una unidad o de un bulto cerrado.
function ControlUnidadBulto({
  itemId,
  precioLista,
  unidadesPorBulto,
  costoActual,
  valor,
  onChange,
}: ControlUnidadBultoProps) {
  const porUnidad = costoUnitario(precioLista, 'UNIDAD', unidadesPorBulto);
  const porBulto = costoUnitario(precioLista, 'BULTO', unidadesPorBulto);
  const actual = valor === 'BULTO' ? porBulto : porUnidad;

  return (
    <fieldset className="m-0 flex min-w-0 flex-col gap-3 rounded-lg border border-line bg-canvas p-4">
      <legend className="px-1 text-base font-semibold">
        ¿El precio de {costo(precioLista)} es por unidad o por bulto de {unidadesPorBulto}?
      </legend>
      <div className="grid gap-3 sm:grid-cols-2">
        <OpcionRadio
          name={`presentacion-${itemId}`}
          value="UNIDAD"
          checked={valor === 'UNIDAD'}
          onChange={() => onChange('UNIDAD')}
          titulo="Por unidad"
          detalle={`Cada una sale ${costo(porUnidad)}`}
        />
        <OpcionRadio
          name={`presentacion-${itemId}`}
          value="BULTO"
          checked={valor === 'BULTO'}
          onChange={() => onChange('BULTO')}
          titulo={`Por bulto de ${unidadesPorBulto}`}
          detalle={`Cada una sale ${costo(porBulto)}`}
        />
      </div>
      <div className="flex flex-wrap items-center gap-x-1">
        <p className="m-0 text-body" aria-live="polite">
          Costo por unidad: <strong>{costo(actual)}</strong>
          <span className="text-soft"> (hoy pagás {costo(costoActual)}).</span>
        </p>
        <InfoTip tema="unidad o bulto">
          Algunas listas ponen el precio de una caja cerrada (bulto) y otras el de una sola unidad.
          Si un bulto de 12 cuesta $ 1.200, cada unidad te sale $ 100: por eso necesitamos saber
          cuál es.
        </InfoTip>
      </div>
    </fieldset>
  );
}
