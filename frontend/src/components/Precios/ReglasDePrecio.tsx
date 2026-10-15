import { useMemo, useState } from 'react';
import { ArrowRightIcon, CheckCircleIcon, WarningIcon } from '@phosphor-icons/react';
import {
  Badge,
  Button,
  ConfirmDialog,
  InfoTip,
  InputField,
  Panel,
  SelectField,
  StickyActionBar,
} from '@/components/ui';
import { useAccionesRed, useProductos, useReglas } from '@/hooks/red/useRed';
import {
  margenSobreCosto,
  precioSegunRegla,
  redondearHaciaArriba,
  reglaDe,
} from '@/lib/red/calculos';
import { pesos, plural, porcentaje } from '@/lib/red/formato';
import type { ProductoRed, ReglaCategoria, ReglasPrecio } from '@/types/red';
import { leerNumero, type AvisoPrecios } from './ayudas';

/* ---------- borrador (lo que se edita antes de guardar) ---------- */

type CamposBorrador = { margen: string; redondeo: number };

type Borrador = {
  general: CamposBorrador;
  porCategoria: Record<string, CamposBorrador>;
  minimo: string;
};

const MARGEN_MAXIMO = 300;
const REDONDEOS = [10, 50, 100];
const EJEMPLO = 2859;
const MENSAJE_NUMERO = `Escribí un número entre 0 y ${MARGEN_MAXIMO}.`;

const comoTexto = (numero: number) => String(numero).replace('.', ',');

const aBorrador = (reglas: ReglasPrecio): Borrador => ({
  general: { margen: comoTexto(reglas.general.margen), redondeo: reglas.general.redondeo },
  porCategoria: Object.fromEntries(
    Object.entries(reglas.porCategoria).map(([categoria, regla]) => [
      categoria,
      { margen: comoTexto(regla.margen), redondeo: regla.redondeo },
    ]),
  ),
  minimo: comoTexto(reglas.margenMinimo),
});

function leerMargen(texto: string): number | null {
  const valor = leerNumero(texto);
  return valor !== null && valor <= MARGEN_MAXIMO ? valor : null;
}

type Lectura = { reglas: ReglasPrecio | null; errores: Record<string, string> };

function leerBorrador(borrador: Borrador): Lectura {
  const errores: Record<string, string> = {};
  const general = leerMargen(borrador.general.margen);
  if (general === null) errores.general = MENSAJE_NUMERO;
  const minimo = leerMargen(borrador.minimo);
  if (minimo === null) errores.minimo = MENSAJE_NUMERO;
  const porCategoria: Record<string, ReglaCategoria> = {};
  for (const [categoria, campos] of Object.entries(borrador.porCategoria)) {
    const margen = leerMargen(campos.margen);
    if (margen === null) errores[`categoria:${categoria}`] = MENSAJE_NUMERO;
    else porCategoria[categoria] = { margen, redondeo: campos.redondeo };
  }
  if (Object.keys(errores).length > 0 || general === null || minimo === null) {
    return { reglas: null, errores };
  }
  return {
    reglas: {
      general: { margen: general, redondeo: borrador.general.redondeo },
      porCategoria,
      margenMinimo: minimo,
    },
    errores,
  };
}

/** Texto estable para saber si el borrador difiere de lo guardado. */
const firma = (reglas: ReglasPrecio) =>
  JSON.stringify([
    reglas.general.margen,
    reglas.general.redondeo,
    reglas.margenMinimo,
    Object.keys(reglas.porCategoria)
      .sort()
      .map((categoria) => [
        categoria,
        reglas.porCategoria[categoria]?.margen,
        reglas.porCategoria[categoria]?.redondeo,
      ]),
  ]);

/* ---------- impacto ---------- */

type Cambio = {
  producto: ProductoRed;
  nuevo: number;
  enRiesgo: boolean;
};

function calcularCambios(productos: ProductoRed[], reglas: ReglasPrecio): Cambio[] {
  return productos
    .map((producto) => ({
      producto,
      nuevo: precioSegunRegla(producto.costo, reglaDe(reglas, producto.categoria)),
      enRiesgo: margenSobreCosto(producto.costo, producto.precio) < reglas.margenMinimo,
    }))
    .filter((cambio) => cambio.nuevo !== cambio.producto.precio)
    .sort(
      (a, b) =>
        Number(b.enRiesgo) - Number(a.enRiesgo) ||
        Math.abs(b.nuevo - b.producto.precio) - Math.abs(a.nuevo - a.producto.precio),
    );
}

const LIMITE_VISTA_PREVIA = 5;

type ReglasDePrecioProps = { onAviso: (aviso: AvisoPrecios) => void };

// Las reglas con las que Ensambla calcula los precios que propone. Se editan en un borrador, se ve
// qué cambiaría y recién al guardar o al confirmar se toca algo.
export function ReglasDePrecio({ onAviso }: ReglasDePrecioProps) {
  const productos = useProductos();
  const guardadas = useReglas();
  const { guardarReglas, aplicarPrecio } = useAccionesRed();

  const [borrador, setBorrador] = useState<Borrador>(() => aBorrador(guardadas));
  const [verTodos, setVerTodos] = useState(false);
  const [pendiente, setPendiente] = useState<Cambio[] | null>(null);

  const { reglas: leidas, errores } = useMemo(() => leerBorrador(borrador), [borrador]);
  const valido = leidas !== null;
  const hayCambios = leidas !== null ? firma(leidas) !== firma(guardadas) : true;
  // Con datos mal escritos la vista previa sigue mostrando las reglas que ya están guardadas.
  const vigentes = leidas ?? guardadas;

  const categorias = useMemo(
    () =>
      [
        ...new Set([...productos.map((p) => p.categoria), ...Object.keys(borrador.porCategoria)]),
      ].sort((a, b) => a.localeCompare(b, 'es')),
    [productos, borrador.porCategoria],
  );

  const cambios = useMemo(() => calcularCambios(productos, vigentes), [productos, vigentes]);
  const enRiesgo = cambios.filter((cambio) => cambio.enRiesgo);
  const suben = cambios.filter((cambio) => cambio.nuevo > cambio.producto.precio).length;
  const bajan = cambios.length - suben;
  const visibles = verTodos ? cambios : cambios.slice(0, LIMITE_VISTA_PREVIA);

  const margenMinimo = vigentes.margenMinimo;

  function cambiarGeneral(campos: CamposBorrador) {
    setBorrador((actual) => ({ ...actual, general: campos }));
  }

  function cambiarCategoria(categoria: string, campos: CamposBorrador | null) {
    setBorrador((actual) => {
      const resto = Object.entries(actual.porCategoria).filter(([nombre]) => nombre !== categoria);
      const porCategoria = Object.fromEntries(campos ? [...resto, [categoria, campos]] : resto);
      return { ...actual, porCategoria };
    });
  }

  function guardar() {
    if (!leidas) return;
    guardarReglas(leidas);
    onAviso({ mensaje: 'Listo. Guardamos tus reglas de precio.' });
  }

  function descartar() {
    setBorrador(aBorrador(guardadas));
  }

  function confirmarAplicar() {
    if (!pendiente) return;
    for (const cambio of pendiente) aplicarPrecio(cambio.producto.id, cambio.nuevo);
    const cantidad = pendiente.length;
    setPendiente(null);
    onAviso({
      mensaje:
        cantidad === 1
          ? 'Listo. Cambiamos 1 precio. Falta cambiar la etiqueta en el mostrador'
          : `Listo. Cambiamos ${cantidad} precios. Faltan cambiar las etiquetas en el mostrador`,
      mostrador: true,
    });
  }

  return (
    <div className="flex flex-col gap-5">
      <Panel flat className="flex flex-col gap-2">
        <h2 className="m-0 text-h3">Cómo se arma tu precio</h2>
        <p className="m-0 text-body text-soft">
          Ensambla toma lo que te cuesta cada producto, le suma tu ganancia y redondea hacia arriba.
          Con eso te propone el precio cuando llega una lista nueva.{' '}
          <InfoTip tema="redondeo">
            El redondeo deja el precio en un número cómodo para cobrar. Con redondeo de a $ 50, un
            precio calculado de $ 1.234 queda en $ 1.250.
          </InfoTip>
        </p>
        <p className="m-0 text-body text-soft">
          <strong className="text-fg">La regla de una categoría gana sobre la general.</strong> Si
          una categoría no tiene regla propia, usa la general. Nada cambia hasta que confirmás.{' '}
          <InfoTip tema="regla de la categoría">
            Si una categoría tiene su propia regla, esa gana. Por ejemplo: la general es 30 % pero
            en Bebidas ponés 20 %; las bebidas se calculan con 20 % y el resto con 30 %.
          </InfoTip>
        </p>
      </Panel>

      <Panel aria-labelledby="regla-minimo" className="flex flex-col gap-3">
        <h2 id="regla-minimo" className="m-0 text-h3">
          Ganancia mínima
        </h2>
        <p className="m-0 text-body text-soft">
          Si un producto queda con menos ganancia que esta, te avisamos que el precio está en
          riesgo.{' '}
          <InfoTip tema="ganancia mínima">
            Es lo menos que aceptás ganar sobre el costo. Si tu mínimo es 20 % y un producto
            quedaría en 12 %, aparece como «en riesgo» para que lo corrijas.
          </InfoTip>
        </p>
        <div className="max-w-xs">
          <InputField
            label="Ganancia mínima (%)"
            inputMode="decimal"
            autoComplete="off"
            value={borrador.minimo}
            error={errores.minimo}
            onChange={(event) => setBorrador({ ...borrador, minimo: event.target.value })}
          />
        </div>
      </Panel>

      <Panel aria-labelledby="regla-general" className="flex flex-col gap-3">
        <h2 id="regla-general" className="m-0 text-h3">
          Regla general
        </h2>
        <p className="m-0 text-body text-soft">
          Se usa en las categorías que no tienen una regla propia.
        </p>
        <CamposRegla
          etiquetaGanancia="Ganancia general (%)"
          etiquetaRedondeo="Redondeo general"
          campos={borrador.general}
          error={errores.general}
          minimo={margenMinimo}
          onChange={cambiarGeneral}
        />
      </Panel>

      <Panel aria-labelledby="regla-categorias" className="flex flex-col gap-4">
        <h2 id="regla-categorias" className="m-0 text-h3">
          Reglas por categoría
        </h2>
        <ul className="m-0 flex list-none flex-col gap-4 p-0">
          {categorias.map((categoria) => {
            const propia = borrador.porCategoria[categoria];
            return (
              <li
                key={categoria}
                className="flex flex-col gap-3 rounded-lg border border-line bg-canvas p-4"
              >
                <h3 className="m-0 text-body font-bold">{categoria}</h3>
                {propia ? (
                  <>
                    <CamposRegla
                      etiquetaGanancia={`Ganancia de ${categoria} (%)`}
                      etiquetaRedondeo={`Redondeo de ${categoria}`}
                      campos={propia}
                      error={errores[`categoria:${categoria}`]}
                      minimo={margenMinimo}
                      onChange={(campos) => cambiarCategoria(categoria, campos)}
                    />
                    <div>
                      <Button
                        variant="ghost"
                        aria-label={`Usar la regla general en ${categoria}`}
                        onClick={() => cambiarCategoria(categoria, null)}
                      >
                        Usar la regla general
                      </Button>
                    </div>
                  </>
                ) : (
                  <>
                    <p className="m-0 text-body text-soft">
                      Usa la regla general: {porcentaje(leerMargen(borrador.general.margen) ?? 0)}{' '}
                      de ganancia y redondeo de a {pesos(borrador.general.redondeo)}.
                    </p>
                    <div>
                      <Button
                        variant="primary"
                        aria-label={`Poner una regla propia en ${categoria}`}
                        onClick={() => cambiarCategoria(categoria, { ...borrador.general })}
                      >
                        Poner una regla propia
                      </Button>
                    </div>
                  </>
                )}
              </li>
            );
          })}
        </ul>
      </Panel>

      <Panel aria-labelledby="regla-impacto" className="flex flex-col gap-4">
        <h2 id="regla-impacto" className="m-0 text-h3">
          Qué cambiaría
        </h2>
        {!valido && (
          <p className="m-0 flex items-start gap-2 text-body font-semibold text-bad">
            <WarningIcon aria-hidden size={22} weight="fill" className="mt-0.5 shrink-0" />
            Hay datos para corregir. Mientras tanto mostramos lo que cambiaría con tus reglas
            guardadas.
          </p>
        )}
        {cambios.length === 0 ? (
          <p className="m-0 flex items-start gap-2 text-body text-soft">
            <CheckCircleIcon aria-hidden size={24} weight="fill" className="shrink-0 text-ok" />
            Con estas reglas, ningún producto cambiaría de precio.
          </p>
        ) : (
          <>
            <p className="m-0 text-body">
              Con estas reglas,{' '}
              <strong>
                {plural(cambios.length, 'producto cambiaría', 'productos cambiarían')}
              </strong>{' '}
              de precio
              {suben > 0 && bajan > 0
                ? `: ${suben} ${suben === 1 ? 'sube' : 'suben'} y ${bajan} ${bajan === 1 ? 'baja' : 'bajan'}.`
                : suben > 0
                  ? ` (${suben === 1 ? 'sube' : 'suben'}).`
                  : ` (${bajan === 1 ? 'baja' : 'bajan'}).`}
            </p>
            <ul
              aria-label="Precios que cambiarían"
              className="m-0 flex list-none flex-col gap-3 p-0"
            >
              {visibles.map((cambio) => (
                <li
                  key={cambio.producto.id}
                  className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line/60 pb-3 last:border-b-0 last:pb-0"
                >
                  <div className="flex min-w-0 flex-col gap-1">
                    <span className="text-body font-semibold break-words">
                      {cambio.producto.nombre}
                    </span>
                    <span className="flex flex-wrap items-center gap-2 text-body text-soft">
                      {pesos(cambio.producto.precio)}
                      <span className="sr-only">pasaría a</span>
                      <ArrowRightIcon aria-hidden size={18} />
                      <strong className="text-fg">{pesos(cambio.nuevo)}</strong>
                      {cambio.enRiesgo && (
                        <Badge tone="bad" icon={<WarningIcon size={16} weight="fill" />}>
                          Hoy en riesgo
                        </Badge>
                      )}
                    </span>
                  </div>
                  <Button
                    size="compact"
                    aria-label={`Aplicar ${pesos(cambio.nuevo)} a ${cambio.producto.nombre}`}
                    onClick={() => setPendiente([cambio])}
                  >
                    Aplicar
                  </Button>
                </li>
              ))}
            </ul>
            {cambios.length > LIMITE_VISTA_PREVIA && (
              <div>
                <Button variant="ghost" onClick={() => setVerTodos((actual) => !actual)}>
                  {verTodos ? 'Ver menos' : `Ver los ${cambios.length} productos`}
                </Button>
              </div>
            )}
            {enRiesgo.length > 0 && (
              <div className="flex flex-col items-start gap-2 border-t border-line pt-4">
                <p className="m-0 text-body text-soft">
                  {plural(enRiesgo.length, 'producto está', 'productos están')} por debajo de tu
                  ganancia mínima.
                </p>
                <Button variant="primary" onClick={() => setPendiente(enRiesgo)}>
                  {enRiesgo.length === 1
                    ? 'Aplicar al que queda en riesgo'
                    : `Aplicar a los ${enRiesgo.length} que quedan en riesgo`}
                </Button>
              </div>
            )}
          </>
        )}
      </Panel>

      {hayCambios && (
        <StickyActionBar label="Guardar reglas">
          <p className="m-0 text-base font-semibold text-fg" role="status">
            {valido
              ? 'Tenés cambios sin guardar.'
              : 'Corregí los datos marcados para poder guardar.'}
          </p>
          <div className="flex flex-wrap gap-3">
            <Button onClick={descartar}>Descartar cambios</Button>
            <Button variant="solid" disabled={!valido} onClick={guardar}>
              Guardar reglas
            </Button>
          </div>
        </StickyActionBar>
      )}

      <ConfirmDialog
        open={pendiente !== null}
        tono="normal"
        titulo={
          pendiente?.length === 1
            ? `¿Cambiar el precio de ${pendiente[0]?.producto.nombre ?? 'este producto'}?`
            : `¿Cambiar el precio de ${pendiente?.length ?? 0} productos?`
        }
        confirmar="Sí, cambiar"
        conservar="No, dejarlos como están"
        onConfirmar={confirmarAplicar}
        onConservar={() => setPendiente(null)}
      >
        <ul className="m-0 flex list-none flex-col gap-2 p-0">
          {(pendiente ?? []).map((cambio) => (
            <li key={cambio.producto.id}>
              <strong className="text-fg">{cambio.producto.nombre}</strong>:{' '}
              {pesos(cambio.producto.precio)} → {pesos(cambio.nuevo)}
            </li>
          ))}
        </ul>
        <p className="m-0">
          Después tenés que cambiar la etiqueta en el mostrador. Podés volver a cambiar el precio
          cuando quieras.
        </p>
      </ConfirmDialog>
    </div>
  );
}

/* ---------- un par ganancia + redondeo ---------- */

type CamposReglaProps = {
  etiquetaGanancia: string;
  etiquetaRedondeo: string;
  campos: CamposBorrador;
  error?: string;
  /** Ganancia mínima vigente: avisa si esta regla queda por debajo. */
  minimo: number;
  onChange: (campos: CamposBorrador) => void;
};

function CamposRegla({
  etiquetaGanancia,
  etiquetaRedondeo,
  campos,
  error,
  minimo,
  onChange,
}: CamposReglaProps) {
  const margen = leerMargen(campos.margen);
  const opciones = [...new Set([...REDONDEOS, campos.redondeo])].sort((a, b) => a - b);
  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <InputField
          label={etiquetaGanancia}
          inputMode="decimal"
          autoComplete="off"
          value={campos.margen}
          error={error}
          hint="Lo que le sumás al costo."
          onChange={(event) => onChange({ ...campos, margen: event.target.value })}
        />
        <SelectField
          label={etiquetaRedondeo}
          value={String(campos.redondeo)}
          hint="Para que el precio quede redondo."
          onChange={(event) => onChange({ ...campos, redondeo: Number(event.target.value) })}
        >
          {opciones.map((redondeo) => (
            <option key={redondeo} value={redondeo}>
              {`De a ${pesos(redondeo)} (${pesos(EJEMPLO)} → ${pesos(redondearHaciaArriba(EJEMPLO, redondeo))})`}
            </option>
          ))}
        </SelectField>
      </div>
      {margen !== null && margen < minimo && (
        <p className="m-0 flex items-start gap-2 text-hint font-semibold text-soft">
          <WarningIcon
            aria-hidden
            size={20}
            weight="fill"
            className="mt-0.5 shrink-0 text-amber-700"
          />
          Esta ganancia queda por debajo de tu mínimo ({porcentaje(minimo)}): los productos
          seguirían en riesgo.
        </p>
      )}
    </div>
  );
}
