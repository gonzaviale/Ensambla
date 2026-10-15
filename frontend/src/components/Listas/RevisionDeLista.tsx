import { useEffect, useRef, useState } from 'react';
import { Link } from '@tanstack/react-router';
import {
  Button,
  ConfirmDialog,
  PageHeading,
  Panel,
  Stepper,
  StickyActionBar,
  buttonClasses,
} from '@/components/ui';
import {
  useAccionesRed,
  useProductos,
  useProveedor,
  useReglas,
  useResumenLista,
  useUmbrales,
} from '@/hooks/red/useRed';
import { haceCuanto } from '@/lib/fechas';
import { pesos, plural } from '@/lib/red/formato';
import type { ItemLista, ListaProveedor } from '@/types/red';
import {
  NOMBRE_ORIGEN_CORTO,
  idsParaRevisar,
  precioElegido,
  proteccionMensual,
  useRarosDeLista,
} from './analisis';
import { ListaCerrada } from './ListaCerrada';
import { PasoConfirmar } from './PasoConfirmar';
import { PasoDudas } from './PasoDudas';
import { PasoPrecios } from './PasoPrecios';
import { PasoQueCambio } from './PasoQueCambio';

const PASOS = ['Qué cambió', 'Revisá las dudas', 'Tus precios', 'Confirmar'];

// Revisar una lista en cuatro pasos. Nada se aplica hasta el último, y sólo si el comercio lo pide.
export function RevisionDeLista({ lista }: { lista: ListaProveedor }) {
  const proveedor = useProveedor(lista.proveedorId);
  const resumen = useResumenLista(lista.id);
  const productos = useProductos();
  const reglas = useReglas();
  const umbrales = useUmbrales();
  const raros = useRarosDeLista(lista);
  const { aplicarLista, descartarLista } = useAccionesRed();

  const [paso, setPaso] = useState(0);
  const [precios, setPrecios] = useState<Record<string, number>>({});
  // Las líneas a revisar se fijan al abrir: al resolverlas pasan a «Listo» pero no desaparecen.
  const [ids] = useState(() => idsParaRevisar(lista, productos, umbrales));
  const [originales] = useState<Record<string, ItemLista>>(() =>
    Object.fromEntries(lista.items.map((item) => [item.id, item])),
  );
  const [exito, setExito] = useState<{ precios: number; protegido: number } | null>(null);
  const [descartando, setDescartando] = useState(false);

  const tituloPaso = useRef<HTMLHeadingElement>(null);
  const pasoAnterior = useRef(paso);
  useEffect(() => {
    if (pasoAnterior.current === paso) return;
    pasoAnterior.current = paso;
    tituloPaso.current?.focus();
  }, [paso]);

  const cambios = resumen?.cambios ?? [];
  const nombre = proveedor?.nombre ?? 'tu proveedor';
  const preciosFinales = Object.fromEntries(
    cambios.map((cambio) => [cambio.itemId, precioElegido(cambio, precios)]),
  );
  const hayPrecioInvalido = cambios.some((cambio) => !(precioElegido(cambio, precios) > 0));
  const preciosQueCambian = cambios.filter(
    (cambio) => precioElegido(cambio, precios) !== cambio.precioActual,
  ).length;
  const protegido = proteccionMensual(cambios, productos, precios);
  const dudasSinResolver = ids.filter((id) => {
    const item = lista.items.find((i) => i.id === id);
    return item ? !item.confirmado && !item.ignorado : false;
  }).length;

  // Descartada desde el último paso: queda la vista de sólo lectura.
  if (!exito && lista.estado !== 'por-revisar') return <ListaCerrada lista={lista} />;

  if (exito) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeading
          kicker="Lista aplicada"
          title={
            exito.precios > 0
              ? `Listo, cambiamos ${plural(exito.precios, 'precio', 'precios')}`
              : 'Listo, actualizamos tus costos'
          }
          lead={`Los costos y los precios de la lista de ${nombre} ya están al día en tu sistema.`}
        />
        <Panel className="flex flex-col gap-4">
          {exito.protegido > 0 && (
            <p className="m-0 text-body">
              Protegés <strong>≈ {pesos(exito.protegido)} por mes</strong> de ganancia.
            </p>
          )}
          <p className="m-0 text-body text-soft">
            Falta un paso: cambiar las etiquetas en el local. Te armamos la lista con todo lo que
            hay que cambiar.
          </p>
          <div className="flex flex-wrap gap-3">
            <Link to="/mostrador" className={buttonClasses({ variant: 'solid' })}>
              Ver los cambios para el mostrador
            </Link>
            <Link to="/listas" className={buttonClasses({ variant: 'secondary' })}>
              Volver a Listas
            </Link>
          </div>
        </Panel>
      </div>
    );
  }

  function aplicar() {
    aplicarLista(lista.id, preciosFinales);
    setExito({ precios: preciosQueCambian, protegido });
  }

  const ultimoPaso = paso === PASOS.length - 1;
  const textoDeAyuda = [
    'Paso 1 de 4: mirá qué cambió. No hace falta decidir nada todavía.',
    dudasSinResolver > 0
      ? `Quedan ${plural(dudasSinResolver, 'duda', 'dudas')} sin resolver: esas líneas no se van a aplicar.`
      : 'Paso 2 de 4: dudas resueltas.',
    hayPrecioInvalido
      ? 'Hay un precio sin completar.'
      : 'Paso 3 de 4: revisá cada precio y cambialo si querés.',
    cambios.length === 0
      ? 'No hay precios para aplicar. Podés volver atrás o descartar la lista.'
      : 'Paso 4 de 4: confirmá para actualizar tu sistema.',
  ][paso];

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Revisar la lista"
        title={`Lista de ${nombre}`}
        lead={`Llegó ${haceCuanto(lista.recibidaEl)} por ${NOMBRE_ORIGEN_CORTO[lista.origen]}. Nada cambia hasta que vos lo confirmes.`}
      />

      <div data-recorrido="lista-pasos">
        <Stepper pasos={PASOS} actual={paso} label="Pasos para revisar la lista" />
      </div>

      <section
        aria-labelledby="titulo-del-paso"
        data-recorrido="lista-contenido"
        className="flex flex-col gap-4"
      >
        <h2
          id="titulo-del-paso"
          ref={tituloPaso}
          tabIndex={-1}
          className="m-0 text-h2 focus:outline-none"
        >
          {PASOS[paso]}
        </h2>
        {paso === 0 && resumen && (
          <PasoQueCambio proveedor={proveedor} resumen={resumen} raros={raros} />
        )}
        {paso === 1 && (
          <PasoDudas
            listaId={lista.id}
            ids={ids}
            originales={originales}
            items={lista.items}
            productos={productos}
          />
        )}
        {paso === 2 && (
          <PasoPrecios
            cambios={cambios}
            productos={productos}
            reglas={reglas}
            precios={precios}
            onPrecio={(itemId, precio) =>
              setPrecios((actuales) => ({ ...actuales, [itemId]: precio }))
            }
          />
        )}
        {paso === 3 && (
          <PasoConfirmar
            costos={cambios.length}
            preciosQueCambian={preciosQueCambian}
            preciosQueSeQuedan={cambios.length - preciosQueCambian}
            sinAplicar={lista.items.length - cambios.length}
            protegido={protegido}
            onDescartar={() => setDescartando(true)}
          />
        )}
      </section>

      <StickyActionBar label="Pasos de la revisión">
        <p className="m-0 text-hint text-soft" aria-live="polite">
          {textoDeAyuda}
        </p>
        <div className="flex flex-wrap gap-3">
          {paso > 0 && <Button onClick={() => setPaso(paso - 1)}>Atrás</Button>}
          {ultimoPaso ? (
            <Button variant="solid" disabled={cambios.length === 0} onClick={aplicar}>
              {preciosQueCambian > 0
                ? `Aplicar ${plural(preciosQueCambian, 'precio', 'precios')}`
                : 'Aplicar los costos nuevos'}
            </Button>
          ) : (
            <Button
              variant="solid"
              disabled={paso === 2 && hayPrecioInvalido}
              onClick={() => setPaso(paso + 1)}
            >
              Seguir
            </Button>
          )}
        </div>
      </StickyActionBar>

      <ConfirmDialog
        open={descartando}
        titulo={`¿Descartar la lista de ${nombre}?`}
        confirmar="Descartar la lista"
        conservar="Seguir revisando"
        onConservar={() => setDescartando(false)}
        onConfirmar={() => {
          descartarLista(lista.id);
          setDescartando(false);
        }}
      >
        <p className="m-0">
          No vamos a cambiar ningún costo ni ningún precio con esta lista. Queda en tu historial
          como descartada.
        </p>
      </ConfirmDialog>
    </div>
  );
}
