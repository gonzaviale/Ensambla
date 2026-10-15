import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, MegaphoneIcon, TagIcon } from '@phosphor-icons/react';
import {
  Badge,
  Button,
  EmptyState,
  InfoTip,
  InputField,
  Panel,
  SelectField,
  buttonClasses,
} from '@/components/ui';
import {
  useAccionesRed,
  useBarrio,
  useOfertas,
  usePlanActual,
  useProductos,
  useZona,
} from '@/hooks/red/useRed';
import { fechaCorta, nombreDelDia, pesos, plural, porcentaje } from '@/lib/red/formato';
import { descuentoDeOferta, ofertaVigente } from './calculosRed';
import { PROVEEDOR_DEMO_ID } from './datosDemo';

const FRACCION_SI_NO_HAY_DATO = 0.3;

const numero = (texto: string) => Number(texto.trim().replace(',', '.'));

// Lado del mayorista, paso 2: publica una oferta que llega sólo a quien vende ese producto.
export function PublicarOferta() {
  const productos = useProductos().filter((p) => p.proveedorId === PROVEEDOR_DEMO_ID);
  const ofertas = useOfertas();
  const zona = useZona();
  const barrio = useBarrio();
  const plan = usePlanActual();
  const { publicarOferta } = useAccionesRed();
  const [ahora] = useState(() => new Date());

  const [productoId, setProductoId] = useState('');
  const [costoTexto, setCostoTexto] = useState('');
  const [diasTexto, setDiasTexto] = useState('7');
  const [bultosTexto, setBultosTexto] = useState('1');
  const [intentado, setIntentado] = useState(false);
  const [publicada, setPublicada] = useState<string | null>(null);

  const producto = productos.find((p) => p.id === productoId) ?? productos[0];
  const publicadas = ofertas
    .filter((oferta) => oferta.proveedorId === PROVEEDOR_DEMO_ID)
    .sort((a, b) => new Date(b.vence).getTime() - new Date(a.vence).getTime());

  if (!producto) {
    return (
      <Panel aria-labelledby="mayorista-oferta" data-recorrido="mayorista-oferta">
        <EmptyState
          icon={<TagIcon aria-hidden size={32} />}
          title="Este proveedor todavía no tiene productos"
        >
          Cuando suba una lista, va a poder publicar ofertas de esos productos.
        </EmptyState>
      </Panel>
    );
  }

  const costoOferta = numero(costoTexto);
  const dias = numero(diasTexto);
  const bultos = numero(bultosTexto);

  const errorCosto =
    costoTexto.trim() === ''
      ? intentado
        ? 'Escribí el costo de la oferta.'
        : undefined
      : !Number.isFinite(costoOferta) || costoOferta <= 0
        ? 'Escribí un costo mayor a cero.'
        : costoOferta >= producto.costo
          ? `La oferta tiene que ser menor al costo habitual (${pesos(producto.costo)}).`
          : undefined;
  const errorDias =
    !Number.isInteger(dias) || dias < 1 || dias > 60
      ? 'Poné una cantidad de días entre 1 y 60.'
      : undefined;
  const errorBultos =
    !Number.isInteger(bultos) || bultos < 1 ? 'El mínimo es de 1 bulto.' : undefined;

  const ofertaValida = costoTexto.trim() !== '' && !errorCosto;
  const descuento = ofertaValida ? -((costoOferta - producto.costo) / producto.costo) * 100 : null;

  const referencia = zona.find((r) => r.productoId === producto.id);
  const alcanzan = referencia?.comercios ?? Math.round(barrio.comercios * FRACCION_SI_NO_HAY_DATO);

  const publicar = (evento: FormEvent) => {
    evento.preventDefault();
    setIntentado(true);
    setPublicada(null);
    if (!ofertaValida || errorDias || errorBultos) return;
    publicarOferta({
      proveedorId: PROVEEDOR_DEMO_ID,
      productoId: producto.id,
      costoOferta,
      dias,
      minimoBultos: bultos,
    });
    setPublicada(producto.nombre);
    setCostoTexto('');
    setIntentado(false);
  };

  return (
    <section
      aria-labelledby="mayorista-oferta"
      data-recorrido="mayorista-oferta"
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1.5">
        <h2 id="mayorista-oferta" className="m-0 text-h3">
          Publicar una oferta
        </h2>
        <p className="m-0 text-body text-soft">
          La oferta le llega sólo a los comercios que venden ese producto, como un aviso de «oferta
          que sirve».{' '}
          <InfoTip tema="alcance de la oferta">
            El alcance es a cuántos comercios les llega la oferta. No se manda a todos: sólo a los
            que ya venden ese producto, así nadie recibe avisos que no le sirven.
          </InfoTip>
        </p>
      </div>

      <Panel>
        <form noValidate onSubmit={publicar} className="flex flex-col gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField
              label="Producto"
              value={producto.id}
              onChange={(evento) => {
                setProductoId(evento.target.value);
                setPublicada(null);
              }}
            >
              {productos.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nombre}
                </option>
              ))}
            </SelectField>
            <InputField
              label="Costo de la oferta (por unidad)"
              required
              type="text"
              inputMode="decimal"
              autoComplete="off"
              value={costoTexto}
              onChange={(evento) => {
                setCostoTexto(evento.target.value);
                setPublicada(null);
              }}
              hint={`Costo habitual: ${pesos(producto.costo)}`}
              error={errorCosto}
            />
            <InputField
              label="Vigencia (días)"
              required
              type="text"
              inputMode="numeric"
              autoComplete="off"
              value={diasTexto}
              onChange={(evento) => setDiasTexto(evento.target.value)}
              error={errorDias}
            />
            <InputField
              label="Mínimo de bultos"
              required
              type="text"
              inputMode="numeric"
              autoComplete="off"
              value={bultosTexto}
              onChange={(evento) => setBultosTexto(evento.target.value)}
              hint={
                producto.unidadesPorBulto
                  ? `Cada bulto trae ${plural(producto.unidadesPorBulto, 'unidad', 'unidades')}.`
                  : undefined
              }
              error={errorBultos}
            />
          </div>

          <Panel flat tight aria-live="polite" className="flex flex-col gap-1.5">
            <p className="m-0 text-hint font-bold tracking-widest text-accent-800 uppercase">
              Vista previa
            </p>
            <p className="m-0 text-body">
              Llegará sólo a los comercios que venden este producto (≈ {alcanzan} de{' '}
              {barrio.comercios}).
            </p>
            <p className="m-0 text-body text-soft">
              {descuento !== null
                ? `Los comercios van a ver ${porcentaje(descuento, { decimales: 1 })} menos: de ${pesos(producto.costo)} a ${pesos(costoOferta)}.`
                : 'Escribí el costo de la oferta para ver el descuento.'}
            </p>
          </Panel>

          <div>
            <Button type="submit" variant="solid">
              <MegaphoneIcon aria-hidden size={22} />
              Publicar oferta
            </Button>
          </div>
        </form>
      </Panel>

      {publicada && (
        <Panel
          role="status"
          className="flex flex-col items-start gap-3 border-ok/40"
          aria-label="Oferta publicada"
        >
          <p className="m-0 flex items-start gap-2 text-body font-semibold">
            <CheckCircleIcon aria-hidden size={26} weight="fill" className="shrink-0 text-ok" />
            <span>
              Publicada. Mirá cómo le llega al comercio.
              <span className="block text-hint font-normal text-soft">
                {publicada}: el aviso aparece en Hoy como «oferta que sirve»
                {plan.incluye('ofertas')
                  ? '.'
                  : ', pero tu plan actual no incluye ofertas y se muestra bloqueado.'}
              </span>
            </span>
          </p>
          <Link to="/hoy" className={buttonClasses({ variant: 'primary' })}>
            Ver el aviso en Hoy
          </Link>
        </Panel>
      )}

      <div className="flex flex-col gap-3">
        <h3 className="m-0 text-body font-bold">Ofertas publicadas por Distribuidora Sur</h3>
        {publicadas.length === 0 ? (
          <p className="m-0 text-body text-soft">Todavía no publicó ninguna oferta.</p>
        ) : (
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {publicadas.map((oferta) => {
              const vigente = ofertaVigente(oferta, ahora);
              return (
                <li key={oferta.id}>
                  <Panel tight className="flex flex-col gap-1.5">
                    <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1">
                      <p className="m-0 min-w-0 text-body font-bold">{oferta.nombreProducto}</p>
                      <Badge tone={vigente ? 'ok' : 'neutral'}>
                        {vigente ? 'Vigente' : 'Vencida'}
                      </Badge>
                    </div>
                    <p className="m-0 text-hint text-soft">
                      {pesos(oferta.costoHabitual)} → <strong>{pesos(oferta.costoOferta)}</strong> (
                      {porcentaje(descuentoDeOferta(oferta))} menos) · hasta el{' '}
                      {nombreDelDia(oferta.vence)} ({fechaCorta(oferta.vence)})
                      {oferta.minimoBultos
                        ? ` · mínimo ${plural(oferta.minimoBultos, 'bulto', 'bultos')}`
                        : ''}
                    </p>
                  </Panel>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}
